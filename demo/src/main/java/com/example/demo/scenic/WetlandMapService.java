package com.example.demo.scenic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
class WetlandMapService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern COORDINATE_HINT_PATTERN = Pattern.compile(
        "(?i)(东经|西经|北纬|南纬|经度|纬度|longitude|latitude|lng|lat|涓滅粡|瑗跨粡|鍖楃含|鍗楃含)"
    );
    private static final List<String> IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

    private final JdbcTemplate jdbcTemplate;
    private final String dataSourceUrl;
    private final Path uploadsRootDir;

    WetlandMapService(
        JdbcTemplate jdbcTemplate,
        @Value("${spring.datasource.url:jdbc:sqlserver://localhost:1433;databaseName=wetlend_db}") String dataSourceUrl,
        @Value("${scenic.uploads-dir:../uploads}") String uploadsDir
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSourceUrl = dataSourceUrl;
        Path rawPath = Paths.get(uploadsDir);
        this.uploadsRootDir = rawPath.isAbsolute()
            ? rawPath.normalize()
            : Paths.get(System.getProperty("user.dir")).resolve(rawPath).normalize();
    }

    WetlandMapPayload getWetlandMapPayload() {
        List<WetlandDbRow> rows;
        try {
            rows = jdbcTemplate.query("""
                SELECT WetlandID, WetlandName, ImagePath, CoordinateRange, [Description], FloraFaunaInfo, Tags, IsActive
                FROM dbo.wetlandInfo
                ORDER BY WetlandID
                """, wetlandRowMapper());
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "failed to query dbo.wetlandInfo from local SQL Server",
                ex
            );
        }

        int totalRows = rows.size();
        int skippedRows = 0;
        List<WetlandMapSpot> spots = new ArrayList<>();

        for (WetlandDbRow row : rows) {
            Optional<WetlandMapSpot> maybeSpot = toMapSpot(row);
            if (maybeSpot.isEmpty()) {
                skippedRows += 1;
                continue;
            }
            spots.add(maybeSpot.get());
        }

        spots.sort(Comparator.comparingInt(WetlandMapSpot::wetlandId));
        return new WetlandMapPayload(
            dataSourceUrl,
            totalRows,
            spots.size(),
            skippedRows,
            List.copyOf(spots)
        );
    }

    private RowMapper<WetlandDbRow> wetlandRowMapper() {
        return (rs, rowNum) -> new WetlandDbRow(
            rs.getInt("WetlandID"),
            safe(rs, "WetlandName"),
            safe(rs, "ImagePath"),
            safe(rs, "CoordinateRange"),
            safe(rs, "Description"),
            safe(rs, "FloraFaunaInfo"),
            safe(rs, "Tags"),
            isRowActive(rs)
        );
    }

    private String safe(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? "" : value.trim();
    }

    private boolean isRowActive(ResultSet rs) throws SQLException {
        Object value = rs.getObject("IsActive");
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return true;
    }

    private Optional<WetlandMapSpot> toMapSpot(WetlandDbRow row) {
        if (!row.isActive() || row.wetlandId() <= 0) {
            return Optional.empty();
        }

        Optional<LngLatValue> coordinate = parseCoordinateRange(row.coordinateRange());
        if (coordinate.isEmpty()) {
            return Optional.empty();
        }

        String normalizedName = row.wetlandName().isBlank() ? ("Wetland " + row.wetlandId()) : row.wetlandName();
        String tags = row.tags();
        String type = resolveType(tags);
        String description = resolveDescription(row);
        String imageHint = tags.isBlank() ? "wetland nature" : tags.replace('/', ' ');
        String imagePath = resolveCompatibleImagePath(row.imagePath());
        LngLatValue point = coordinate.get();

        return Optional.of(new WetlandMapSpot(
            row.wetlandId(),
            "wetland-" + row.wetlandId(),
            normalizedName,
            "wetland-" + row.wetlandId(),
            point.lat(),
            point.lng(),
            type,
            description,
            imageHint,
            imagePath,
            row.coordinateRange(),
            tags
        ));
    }

    private String normalizeImagePath(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String resolveCompatibleImagePath(String rawPath) {
        String normalized = normalizeImagePath(rawPath);
        if (normalized.isBlank()) {
            return "";
        }

        if (resourceExists(normalized)) {
            return normalized;
        }

        int dotIndex = normalized.lastIndexOf('.');
        int slashIndex = normalized.lastIndexOf('/');
        if (dotIndex > slashIndex && dotIndex >= 0) {
            String base = normalized.substring(0, dotIndex);
            String currentExt = normalized.substring(dotIndex).toLowerCase(Locale.ROOT);
            for (String ext : IMAGE_EXTENSIONS) {
                if (ext.equals(currentExt)) continue;
                String candidate = base + ext;
                if (resourceExists(candidate)) {
                    return candidate;
                }
            }
            return normalized;
        }

        for (String ext : IMAGE_EXTENSIONS) {
            String candidate = normalized + ext;
            if (resourceExists(candidate)) {
                return candidate;
            }
        }
        return normalized;
    }

    private boolean resourceExists(String webPath) {
        String normalized = normalizeImagePath(webPath);
        if (normalized.isBlank()) return false;

        String relative = normalized.startsWith("/uploads/")
            ? normalized.substring("/uploads/".length())
            : normalized.replaceFirst("^/+", "");
        if (relative.isBlank()) return false;

        Path resolved = uploadsRootDir.resolve(relative).normalize();
        if (!resolved.startsWith(uploadsRootDir)) return false;
        return Files.isRegularFile(resolved);
    }

    private Optional<LngLatValue> parseCoordinateRange(String coordinateRange) {
        if (coordinateRange == null || coordinateRange.isBlank()) {
            return Optional.empty();
        }

        String normalized = coordinateRange
            .replace("～", "~")
            .replace("—", "-")
            .replace("－", "-")
            .replace("至", "~")
            .replace("，", ",")
            .replace("；", ";")
            .replace("：", ":")
            .replaceAll("\\s+", "");

        List<Double> values = extractTwoCoordinateValues(normalized);
        if (values.size() < 2) {
            return Optional.empty();
        }

        double lng = values.get(0);
        double lat = values.get(1);
        if (lng < 73 || lng > 136 || lat < 3 || lat > 54) {
            return Optional.empty();
        }
        return Optional.of(new LngLatValue(lng, lat));
    }

    private List<Double> extractTwoCoordinateValues(String text) {
        String axisText = text
            .replace("东经", "")
            .replace("西经", "")
            .replace("北纬", "")
            .replace("南纬", "");

        String[] axisParts = axisText.split("[,;]");
        List<Double> values = new ArrayList<>();
        for (String axisPart : axisParts) {
            parseAxisValue(axisPart).ifPresent(values::add);
            if (values.size() >= 2) {
                break;
            }
        }
        return values;
    }

    private Optional<Double> parseAxisValue(String axisPart) {
        if (axisPart == null || axisPart.isBlank()) {
            return Optional.empty();
        }

        String[] candidates = axisPart.split("[~-]+");
        List<Double> parsed = new ArrayList<>();
        for (String candidate : candidates) {
            parseCoordinateValue(candidate).ifPresent(parsed::add);
        }
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        if (parsed.size() == 1) {
            return Optional.of(parsed.get(0));
        }
        return Optional.of((parsed.get(0) + parsed.get(parsed.size() - 1)) / 2.0);
    }

    private Optional<Double> parseCoordinateValue(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher numberMatcher = NUMBER_PATTERN.matcher(text);
        List<Double> parts = new ArrayList<>();
        while (numberMatcher.find()) {
            parts.add(Double.parseDouble(numberMatcher.group(1)));
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }

        double degree = parts.get(0);
        double minute = parts.size() > 1 ? parts.get(1) : 0.0;
        double second = parts.size() > 2 ? parts.get(2) : 0.0;
        return Optional.of(degree + (minute / 60.0) + (second / 3600.0));
    }

    private String resolveType(String tags) {
        if (tags == null || tags.isBlank()) {
            return "湿地";
        }
        String[] segments = tags.split("[/|,，]");
        for (String segment : segments) {
            if (segment != null && !segment.isBlank()) {
                String value = segment.trim();
                return value.endsWith("湿地") ? value : value + "湿地";
            }
        }
        return "湿地";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String resolveDescription(WetlandDbRow row) {
        String description = sanitizeDescription(row.description());
        if (!description.isBlank()) {
            return description;
        }
        return sanitizeDescription(row.floraFaunaInfo());
    }

    private String sanitizeDescription(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (isLikelyCoordinateText(normalized)) {
            return "";
        }
        return normalized;
    }

    private boolean isLikelyCoordinateText(String text) {
        String normalized = text.replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return false;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        int numberCount = 0;
        while (matcher.find()) {
            numberCount += 1;
        }
        if (numberCount < 2) {
            return false;
        }

        boolean hasHint = COORDINATE_HINT_PATTERN.matcher(normalized).find();
        boolean hasSymbols = normalized.contains("°")
            || normalized.contains("'")
            || normalized.contains("′")
            || normalized.contains("\"")
            || normalized.contains("″")
            || normalized.contains("度")
            || normalized.contains("分")
            || normalized.contains("秒");
        boolean hasRangeSeparator = normalized.contains("~")
            || normalized.contains("-")
            || normalized.contains(",")
            || normalized.contains("，")
            || normalized.contains(";")
            || normalized.contains("；")
            || normalized.contains("、");

        if (hasHint) {
            return true;
        }
        if (hasSymbols && hasRangeSeparator) {
            return true;
        }
        return hasRangeSeparator && numberCount >= 4 && normalized.length() <= 56;
    }
}

record WetlandDbRow(
    int wetlandId,
    String wetlandName,
    String imagePath,
    String coordinateRange,
    String description,
    String floraFaunaInfo,
    String tags,
    boolean isActive
) {}

record LngLatValue(double lng, double lat) {}
