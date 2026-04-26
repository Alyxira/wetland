package com.example.demo.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.demo.entity.HistoricalData;
import com.example.demo.entity.Region;
import com.example.demo.repository.HistoricalDataRepository;
import com.example.demo.repository.RegionRepository;
import com.example.demo.repository.TiffImageRepository;
import com.example.demo.repository.WaterQualityResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class WaterQualityController {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TiffImageRepository tiffImageRepository;

    @Autowired
    private WaterQualityResultRepository waterQualityResultRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/regions")
    public ResponseEntity<?> createRegion(@RequestBody Map<String, Object> request) {
        Object nameObj = request.get("name");
        if (nameObj == null || String.valueOf(nameObj).isBlank()) {
            return badRequest("INVALID_ARGUMENT", "name is required");
        }
        Object geometryObj = request.get("geometry");
        if (!(geometryObj instanceof Map<?, ?> geometry)) {
            return badRequest("INVALID_GEOMETRY", "geometry is required");
        }

        String name = String.valueOf(nameObj).trim();
        Object geometryTypeObj = geometry.get("type");
        String geometryType = geometryTypeObj == null ? "Polygon" : String.valueOf(geometryTypeObj);
        Object coordinates = geometry.get("coordinates");
        if (coordinates == null) {
            return badRequest("INVALID_GEOMETRY", "geometry.coordinates is required");
        }

        try {
            Region region = regionRepository.findByRegionName(name).orElseGet(Region::new);
            region.setRegionName(name);
            if (region.getRegionCode() == null || region.getRegionCode().isBlank()) {
                region.setRegionCode("REGION_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }
            region.setGeometryType(geometryType);
            region.setGeometryJson(objectMapper.writeValueAsString(coordinates));

            updateBounds(region, coordinates);
            Region saved = regionRepository.save(region);
            return ResponseEntity.status(201).body(Map.of(
                "regionId", saved.getId(),
                "name", saved.getRegionName()
            ));
        } catch (Exception e) {
            return badRequest("INVALID_GEOMETRY", e.getMessage());
        }
    }

    @GetMapping("/regions")
    public ResponseEntity<?> listRegions() {
        List<Map<String, Object>> items = regionRepository.findAllByOrderByRegionNameAsc().stream()
            .map(this::toRegionItem)
            .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/regions/{regionId}")
    public ResponseEntity<?> getRegion(@PathVariable Long regionId) {
        Optional<Region> regionOpt = regionRepository.findById(regionId);
        if (regionOpt.isEmpty()) {
            return notFound("NOT_FOUND", "region not found");
        }
        return ResponseEntity.ok(toRegionItem(regionOpt.get()));
    }

    @DeleteMapping("/regions/{regionId}")
    @Transactional
    public ResponseEntity<?> deleteRegion(@PathVariable Long regionId) {
        Optional<Region> regionOpt = regionRepository.findById(regionId);
        if (regionOpt.isEmpty()) {
            return notFound("NOT_FOUND", "region not found");
        }

        int unlinkedFromTiff = tiffImageRepository.clearRegionByRegionId(regionId);
        int unlinkedFromResults = waterQualityResultRepository.clearRegionByRegionId(regionId);
        int unlinkedFromHistory = historicalDataRepository.clearRegionByRegionId(regionId);
        regionRepository.deleteById(regionId);

        return ResponseEntity.ok(Map.of(
            "regionId", regionId,
            "deleted", true,
            "unlinked", Map.of(
                "tiffImages", unlinkedFromTiff,
                "waterQualityResults", unlinkedFromResults,
                "historicalData", unlinkedFromHistory
            )
        ));
    }

    @GetMapping("/regions/{regionId}/timeseries")
    public ResponseEntity<?> getTimeSeries(
        @PathVariable Long regionId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
        @RequestParam String parameter,
        @RequestParam(defaultValue = "month") String interval
    ) {
        if (start.isAfter(end)) {
            return badRequest("INVALID_ARGUMENT", "start must be before end");
        }
        if (regionRepository.findById(regionId).isEmpty()) {
            return notFound("NOT_FOUND", "region not found");
        }
        String p = normalizeParameter(parameter);
        if (p == null) {
            return badRequest("INVALID_ARGUMENT", "parameter must be CHLA/TURBIDITY/TSS");
        }

        List<HistoricalData> data = queryHistoricalData(regionId, start, end);
        List<Map<String, Object>> points = aggregateSeries(data, p, interval);
        return ResponseEntity.ok(Map.of(
            "regionId", regionId,
            "parameter", p,
            "unit", unitOf(p),
            "points", points
        ));
    }

    @GetMapping("/regions/{regionId}/statistics")
    public ResponseEntity<?> getStatistics(
        @PathVariable Long regionId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
        @RequestParam String parameters
    ) {
        if (start.isAfter(end)) {
            return badRequest("INVALID_ARGUMENT", "start must be before end");
        }
        if (regionRepository.findById(regionId).isEmpty()) {
            return notFound("NOT_FOUND", "region not found");
        }

        List<HistoricalData> data = queryHistoricalData(regionId, start, end);
        List<Map<String, Object>> items = new ArrayList<>();
        for (String token : parameters.split(",")) {
            String p = normalizeParameter(token.trim());
            if (p == null) {
                continue;
            }
            List<BigDecimal> values = data.stream().map(d -> valueOf(d, p)).collect(Collectors.toList());
            if (values.isEmpty()) {
                continue;
            }
            Map<String, Object> one = new HashMap<>();
            one.put("parameter", p);
            one.put("unit", unitOf(p));
            one.put("mean", mean(values));
            one.put("median", median(values));
            one.put("stddev", stddev(values));
            one.put("trendSlope", trendSlope(data, p));
            items.add(one);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("regionId", regionId);
        body.put("start", start);
        body.put("end", end);
        body.put("items", items);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/history/wetlands/{wetlandName}")
    public ResponseEntity<?> getWetlandHistory(
        @PathVariable String wetlandName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        String name = wetlandName == null ? "" : wetlandName.trim();
        if (name.isBlank()) {
            return badRequest("INVALID_ARGUMENT", "wetlandName is required");
        }

        LocalDateTime effectiveStart = start == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : start;
        LocalDateTime effectiveEnd = end == null ? LocalDateTime.of(2999, 12, 31, 23, 59, 59) : end;
        if (effectiveStart.isAfter(effectiveEnd)) {
            return badRequest("INVALID_ARGUMENT", "start must be before end");
        }

        List<HistoricalData> data = historicalDataRepository
            .findByRegionNameAndDataDateBetweenOrderByDataDateAsc(name, effectiveStart, effectiveEnd);

        List<Map<String, Object>> rows = data.stream()
            .map(this::toWetlandHistoryRow)
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "wetlandName", name,
            "start", effectiveStart,
            "end", effectiveEnd,
            "rows", rows
        ));
    }

    private Map<String, Object> toWetlandHistoryRow(HistoricalData data) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", data.getId());
        row.put("date", data.getDataDate());
        row.put("chla", data.getChlaValue());
        row.put("tss", data.getSpmValue());
        row.put("dataSource", data.getDataSource());
        return row;
    }

    private List<HistoricalData> queryHistoricalData(Long regionId, LocalDateTime start, LocalDateTime end) {
        return historicalDataRepository.findByRegionIdAndDataDateBetweenOrderByDataDateAsc(regionId, start, end);
    }

    private Map<String, Object> toRegionItem(Region region) {
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", region.getGeometryType() == null ? "Polygon" : region.getGeometryType());
        Object coordinates = List.of();
        if (region.getGeometryJson() != null) {
            try {
                coordinates = objectMapper.readValue(region.getGeometryJson(), new TypeReference<Object>() {});
            } catch (Exception ignored) {
            }
        }
        if (isEmptyCoordinates(coordinates)) {
            coordinates = buildPolygonCoordinatesFromBounds(region);
        }
        geometry.put("coordinates", coordinates);

        Map<String, Object> item = new HashMap<>();
        item.put("regionId", region.getId());
        item.put("name", region.getRegionName());
        item.put("geometry", geometry);
        item.put("createdAt", region.getCreateTime());
        return item;
    }

    private boolean isEmptyCoordinates(Object coordinates) {
        if (coordinates == null) {
            return true;
        }
        if (coordinates instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private Object buildPolygonCoordinatesFromBounds(Region region) {
        if (region.getMinLon() == null || region.getMaxLon() == null || region.getMinLat() == null || region.getMaxLat() == null) {
            return List.of();
        }
        return List.of(
            List.of(
                List.of(region.getMinLon(), region.getMinLat()),
                List.of(region.getMinLon(), region.getMaxLat()),
                List.of(region.getMaxLon(), region.getMaxLat()),
                List.of(region.getMaxLon(), region.getMinLat()),
                List.of(region.getMinLon(), region.getMinLat())
            )
        );
    }

    private void updateBounds(Region region, Object coordinatesObj) {
        if (!(coordinatesObj instanceof List<?> root)) {
            return;
        }
        List<BigDecimal> lons = new ArrayList<>();
        List<BigDecimal> lats = new ArrayList<>();
        flattenCoordinates(root, lons, lats);
        if (!lons.isEmpty() && !lats.isEmpty()) {
            region.setMinLon(lons.stream().min(Comparator.naturalOrder()).orElse(null));
            region.setMaxLon(lons.stream().max(Comparator.naturalOrder()).orElse(null));
            region.setMinLat(lats.stream().min(Comparator.naturalOrder()).orElse(null));
            region.setMaxLat(lats.stream().max(Comparator.naturalOrder()).orElse(null));
        }
    }

    private void flattenCoordinates(List<?> node, List<BigDecimal> lons, List<BigDecimal> lats) {
        if (node.isEmpty()) {
            return;
        }
        Object first = node.get(0);
        if (first instanceof Number && node.size() >= 2) {
            lons.add(new BigDecimal(String.valueOf(node.get(0))));
            lats.add(new BigDecimal(String.valueOf(node.get(1))));
            return;
        }
        for (Object child : node) {
            if (child instanceof List<?> list) {
                flattenCoordinates(list, lons, lats);
            }
        }
    }

    private String normalizeParameter(String p) {
        if (p == null) {
            return null;
        }
        return switch (p.toUpperCase()) {
            case "CHLA" -> "CHLA";
            case "TURBIDITY" -> "TURBIDITY";
            case "TSS", "SPM" -> "TSS";
            default -> null;
        };
    }

    private String unitOf(String parameter) {
        return switch (parameter) {
            case "CHLA" -> "mg/m3";
            case "TURBIDITY" -> "NTU";
            case "TSS" -> "mg/L";
            default -> "";
        };
    }

    private List<Map<String, Object>> aggregateSeries(List<HistoricalData> data, String parameter, String interval) {
        Map<String, List<BigDecimal>> groupedValues = new HashMap<>();
        Map<String, LocalDateTime> bucketTimes = new HashMap<>();
        for (HistoricalData d : data) {
            LocalDateTime time = d.getDataDate();
            String bucket = switch (interval.toLowerCase()) {
                case "day" -> time.toLocalDate().toString();
                case "week" -> time.toLocalDate().with(DayOfWeek.MONDAY).toString();
                case "quarter" -> time.getYear() + "-Q" + (((time.getMonthValue() - 1) / 3) + 1);
                default -> YearMonth.of(time.getYear(), time.getMonthValue()).toString();
            };
            groupedValues.computeIfAbsent(bucket, x -> new ArrayList<>()).add(valueOf(d, parameter));
            bucketTimes.putIfAbsent(bucket, time.truncatedTo(ChronoUnit.DAYS));
        }

        return groupedValues.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> {
                Map<String, Object> point = new HashMap<>();
                point.put("time", normalizeBucketTime(e.getKey(), interval, bucketTimes.get(e.getKey())));
                point.put("value", mean(e.getValue()));
                return point;
            })
            .collect(Collectors.toList());
    }

    private LocalDateTime normalizeBucketTime(String bucket, String interval, LocalDateTime fallback) {
        try {
            return switch (interval.toLowerCase()) {
                case "day", "week" -> LocalDate.parse(bucket).atStartOfDay();
                case "quarter" -> {
                    String[] arr = bucket.split("-Q");
                    int year = Integer.parseInt(arr[0]);
                    int quarter = Integer.parseInt(arr[1]);
                    int month = (quarter - 1) * 3 + 1;
                    yield LocalDate.of(year, month, 1).atStartOfDay();
                }
                default -> YearMonth.parse(bucket).atDay(1).atStartOfDay();
            };
        } catch (Exception e) {
            return fallback == null ? LocalDateTime.now() : fallback;
        }
    }

    private BigDecimal valueOf(HistoricalData data, String parameter) {
        return switch (parameter) {
            case "CHLA" -> data.getChlaValue();
            case "TURBIDITY" -> data.getTurbidityValue();
            case "TSS" -> data.getSpmValue();
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal mean(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
            .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal stddev(List<BigDecimal> values) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }
        double avg = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2)).sum() / values.size();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal trendSlope(List<HistoricalData> data, String parameter) {
        if (data.size() < 2) {
            return BigDecimal.ZERO;
        }
        double xMean = (data.size() - 1) / 2.0;
        double yMean = data.stream().mapToDouble(d -> valueOf(d, parameter).doubleValue()).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < data.size(); i++) {
            double x = i;
            double y = valueOf(data.get(i), parameter).doubleValue();
            numerator += (x - xMean) * (y - yMean);
            denominator += Math.pow(x - xMean, 2);
        }
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator / denominator).setScale(6, RoundingMode.HALF_UP);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(errorBody(code, message));
    }

    private ResponseEntity<Map<String, Object>> notFound(String code, String message) {
        return ResponseEntity.status(404).body(errorBody(code, message));
    }

    private Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("requestId", null);
        return body;
    }
}

