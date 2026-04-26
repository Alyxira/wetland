package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WaterbodyMaskService {

    private static final Pattern YEAR_4_PATTERN = Pattern.compile("(\\d{4})");
    private static final Pattern YEAR_2_PATTERN = Pattern.compile("(?<!\\d)(\\d{2})(?!\\d)");
    private static final Map<String, Integer> SEASON_ORDER = Map.of(
        "spring", 1,
        "春", 1,
        "summer", 2,
        "夏", 2,
        "autumn", 3,
        "fall", 3,
        "秋", 3,
        "winter", 4,
        "冬", 4
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PythonService pythonService;
    private final Map<String, CachedGeoJson> tifGeoJsonCache = new HashMap<>();

    @Value("${app.waterbody.geojson.dir:${user.dir}/waterbody-geojson}")
    private String waterbodyGeojsonDir;

    public WaterbodyMaskService(PythonService pythonService) {
        this.pythonService = pythonService;
    }

    public List<String> listFiles() throws IOException {
        Path dir = resolveDataDir();
        if (!Files.exists(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(this::isSupportedMaskFile)
                .sorted(this::compareByYearAndSeason)
                .collect(Collectors.toList());
        }
    }

    public Map<String, String> getAllGeoJson() throws IOException {
        List<String> files = listFiles();
        Map<String, String> result = new LinkedHashMap<>();
        for (String filename : files) {
            result.put(filename, getGeoJson(filename));
        }
        return result;
    }

    public String getGeoJson(String filename) throws IOException {
        Path filePath = resolveFile(filename);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("Mask file not found: " + filename);
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".geojson")) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }

        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return convertTifToGeoJsonWithCache(filePath);
        }

        throw new IOException("Unsupported mask format: " + filename);
    }

    public double[] getBounds(String filename) throws IOException {
        String geoJson = getGeoJson(filename);
        JsonNode root = objectMapper.readTree(geoJson);

        JsonNode coordinatesNode = root.path("geometry").path("coordinates");
        if (coordinatesNode.isMissingNode() || !coordinatesNode.isArray()) {
            throw new IOException("Invalid GeoJSON geometry: " + filename);
        }

        BoundsAccumulator acc = new BoundsAccumulator();
        traverseCoordinates(coordinatesNode, acc);

        if (!acc.hasValue()) {
            throw new IOException("No valid coordinates found in GeoJSON: " + filename);
        }

        return new double[] {acc.minLon, acc.minLat, acc.maxLon, acc.maxLat};
    }

    private void traverseCoordinates(JsonNode node, BoundsAccumulator acc) {
        if (!node.isArray()) {
            return;
        }

        if (isCoordinatePair(node)) {
            double lon = node.get(0).asDouble();
            double lat = node.get(1).asDouble();
            acc.accept(lon, lat);
            return;
        }

        for (JsonNode child : node) {
            traverseCoordinates(child, acc);
        }
    }

    private boolean isCoordinatePair(JsonNode node) {
        return node.size() >= 2 && node.get(0).isNumber() && node.get(1).isNumber();
    }

    private Path resolveDataDir() {
        Path dir = Paths.get(waterbodyGeojsonDir);
        if (!dir.isAbsolute()) {
            dir = Paths.get(System.getProperty("user.dir")).resolve(dir);
        }
        Path configured = dir.normalize().toAbsolutePath();
        if (Files.exists(configured)) {
            return configured;
        }

        Path userDir = Paths.get(System.getProperty("user.dir")).normalize().toAbsolutePath();
        List<Path> candidates = List.of(
            userDir.resolve("waterbody-geojson"),
            userDir.resolve("demo").resolve("waterbody-geojson"),
            userDir.resolve("backend").resolve("waterbody-geojson"),
            userDir.getParent() != null ? userDir.getParent().resolve("waterbody-geojson") : userDir,
            userDir.getParent() != null ? userDir.getParent().resolve("demo").resolve("waterbody-geojson") : userDir,
            userDir.getParent() != null ? userDir.getParent().resolve("backend").resolve("waterbody-geojson") : userDir
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize().toAbsolutePath();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }

        return configured;
    }

    private Path resolveFile(String filename) {
        Path safeName = Paths.get(filename).getFileName();
        return resolveDataDir().resolve(safeName).normalize().toAbsolutePath();
    }

    private boolean isSupportedMaskFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".geojson") || lower.endsWith(".tif") || lower.endsWith(".tiff");
    }

    private String convertTifToGeoJsonWithCache(Path tifPath) throws IOException {
        String cacheKey = tifPath.toString();
        long size = Files.size(tifPath);
        FileTime lastModified = Files.getLastModifiedTime(tifPath);

        CachedGeoJson cached = tifGeoJsonCache.get(cacheKey);
        if (cached != null && cached.size == size && cached.lastModified.equals(lastModified)) {
            return cached.geoJson;
        }

        String geoJson = pythonService.convertMaskTiffToGeoJson(tifPath.toString());
        tifGeoJsonCache.put(cacheKey, new CachedGeoJson(lastModified, size, geoJson));
        return geoJson;
    }

    private int compareByYearAndSeason(String a, String b) {
        FileSortKey ka = parseSortKey(a);
        FileSortKey kb = parseSortKey(b);

        int yearDiff = Integer.compare(ka.year, kb.year);
        if (yearDiff != 0) {
            return yearDiff;
        }

        int seasonDiff = Integer.compare(ka.seasonOrder, kb.seasonOrder);
        if (seasonDiff != 0) {
            return seasonDiff;
        }

        return a.compareToIgnoreCase(b);
    }

    private FileSortKey parseSortKey(String filename) {
        String normalized = filename
            .replace(".geojson", "")
            .replace(".tif", "")
            .replace(".tiff", "");
        String lower = normalized.toLowerCase(Locale.ROOT);

        int year = 0;
        Matcher matcher = YEAR_4_PATTERN.matcher(lower);
        if (matcher.find()) {
            year = Integer.parseInt(matcher.group(1));
        } else {
            Matcher shortYearMatcher = YEAR_2_PATTERN.matcher(lower);
            if (shortYearMatcher.find()) {
                int twoDigit = Integer.parseInt(shortYearMatcher.group(1));
                year = 2000 + twoDigit;
            }
        }

        int seasonOrder = 99;
        for (Map.Entry<String, Integer> entry : SEASON_ORDER.entrySet()) {
            if (normalized.contains(entry.getKey()) || lower.contains(entry.getKey())) {
                seasonOrder = entry.getValue();
                break;
            }
        }

        return new FileSortKey(year, seasonOrder);
    }

    private static final class FileSortKey {
        private final int year;
        private final int seasonOrder;

        private FileSortKey(int year, int seasonOrder) {
            this.year = year;
            this.seasonOrder = seasonOrder;
        }
    }

    private static final class BoundsAccumulator {
        private double minLon = Double.POSITIVE_INFINITY;
        private double minLat = Double.POSITIVE_INFINITY;
        private double maxLon = Double.NEGATIVE_INFINITY;
        private double maxLat = Double.NEGATIVE_INFINITY;

        private void accept(double lon, double lat) {
            minLon = Math.min(minLon, lon);
            minLat = Math.min(minLat, lat);
            maxLon = Math.max(maxLon, lon);
            maxLat = Math.max(maxLat, lat);
        }

        private boolean hasValue() {
            return minLon != Double.POSITIVE_INFINITY && minLat != Double.POSITIVE_INFINITY
                && maxLon != Double.NEGATIVE_INFINITY && maxLat != Double.NEGATIVE_INFINITY;
        }
    }

    private static final class CachedGeoJson {
        private final FileTime lastModified;
        private final long size;
        private final String geoJson;

        private CachedGeoJson(FileTime lastModified, long size, String geoJson) {
            this.lastModified = lastModified;
            this.size = size;
            this.geoJson = geoJson;
        }
    }
}
