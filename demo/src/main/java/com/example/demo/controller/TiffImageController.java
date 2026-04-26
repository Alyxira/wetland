package com.example.demo.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.example.demo.entity.TiffImage;
import com.example.demo.repository.TiffImageRepository;
import com.example.demo.service.PythonService;
import com.example.demo.service.TiffImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class TiffImageController {

    @Autowired
    private TiffImageService tiffImageService;

    @Autowired
    private TiffImageRepository tiffImageRepository;

    @Autowired
    private PythonService pythonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.tiles.cache.dir:uploads/tiles-cache}")
    private String tilesCacheDir;

    @Value("${app.tiles.min-zoom:6}")
    private int minZoom;

    @Value("${app.tiles.enabled:true}")
    private boolean tilesEnabled;

    @Value("${app.tiles.max-image-bytes-for-low-zoom:600000000}")
    private long maxImageBytesForLowZoom;

    @Value("${app.local.images.dir:${user.dir}/uploads/local-images}")
    private String localImagesDir;

    @Value("${app.distribution.maps.dir:${user.dir}/frontend/public/distribution-maps}")
    private String distributionMapsDir;

    @PostMapping("/images")
    public ResponseEntity<?> uploadImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam("acquiredAt") String acquiredAt,
        @RequestParam("sensor") String sensor,
        @RequestParam(value = "cloudCover", required = false) BigDecimal cloudCover
    ) {
        try {
            if (file.isEmpty()) {
                return badRequest("INVALID_FILE", "file is empty");
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".tif") && !fileName.toLowerCase().endsWith(".tiff"))) {
                return badRequest("INVALID_FILE_TYPE", "only TIFF files are supported");
            }

            LocalDateTime acquiredDateTime = parseDateTime(acquiredAt);
            String description = "sensor=" + sensor + (cloudCover == null ? "" : ", cloudCover=" + cloudCover);
            TiffImage tiffImage = tiffImageService.uploadTiffImage(file, null, acquiredDateTime, description);

            Map<String, Object> body = new HashMap<>();
            body.put("imageId", tiffImage.getId());
            body.put("fileName", tiffImage.getFileName());
            body.put("acquiredAt", tiffImage.getAcquisitionDate());
            body.put("status", "READY");
            return ResponseEntity.status(201).body(body);
        } catch (IllegalArgumentException e) {
            return badRequest("INVALID_ARGUMENT", e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(errorBody("UPLOAD_FAILED", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(errorBody("UPLOAD_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/images")
    public ResponseEntity<?> listImages() {
        List<Map<String, Object>> result = tiffImageRepository.findAllByOrderByUploadTimeDesc().stream()
            .map(this::toImageListItem)
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/images/local")
    public ResponseEntity<?> listLocalImages() {
        try {
            Path dir = resolveLocalImagesDir();
            Files.createDirectories(dir);

            List<Map<String, Object>> items;
            try (Stream<Path> stream = Files.walk(dir)) {
                items = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".tif") || name.endsWith(".tiff");
                    })
                    .sorted((a, b) -> {
                        try {
                            FileTime ta = Files.getLastModifiedTime(a);
                            FileTime tb = Files.getLastModifiedTime(b);
                            return tb.compareTo(ta);
                        } catch (IOException e) {
                            return b.getFileName().toString().compareToIgnoreCase(a.getFileName().toString());
                        }
                    })
                    .map(path -> {
                        Map<String, Object> row = new HashMap<>();
                        Path abs = path.normalize().toAbsolutePath();

                        Long imageId = null;
                        try {
                            TiffImage synced = tiffImageService.registerLocalTiffImage(abs, "LOCAL_IMAGE_SYNC");
                            imageId = synced.getId();
                        } catch (Exception ignored) {
                        }

                        row.put("fileName", abs.getFileName().toString());
                        row.put("filePath", abs.toString());
                        row.put("bounds", readBounds(abs));
                        row.put("boundsCrs", "EPSG:4326");
                        row.put("imageId", imageId);
                        try {
                            row.put("fileSize", Files.size(abs));
                            row.put("modifiedAt", Files.getLastModifiedTime(abs).toInstant().toString());
                        } catch (IOException e) {
                            row.put("fileSize", null);
                            row.put("modifiedAt", null);
                        }
                        if (imageId == null) {
                            tiffImageRepository.findByFilePath(abs.toString())
                                .ifPresent(image -> row.put("imageId", image.getId()));
                        }
                        return row;
                    })
                    .toList();
            }
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorBody("LOCAL_IMAGES_READ_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/distribution-maps/pngs")
    public ResponseEntity<?> listDistributionMapPngs() {
        try {
            Path dir = resolveConfiguredPath(distributionMapsDir);
            Files.createDirectories(dir);

            List<Map<String, Object>> items;
            try (Stream<Path> stream = Files.list(dir)) {
                items = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                    .map(path -> {
                        Map<String, Object> row = new HashMap<>();
                        String fileName = path.getFileName().toString();
                        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                        row.put("fileName", fileName);
                        row.put("url", "/distribution-maps/" + encodedName);
                        try {
                            row.put("fileSize", Files.size(path));
                            row.put("modifiedAt", Files.getLastModifiedTime(path).toInstant().toString());
                        } catch (IOException e) {
                            row.put("fileSize", null);
                            row.put("modifiedAt", null);
                        }
                        return row;
                    })
                    .toList();
            }
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(errorBody("DISTRIBUTION_MAPS_READ_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/images/{imageId}/file")
    public ResponseEntity<?> downloadImageFile(@PathVariable Long imageId) {
        Optional<TiffImage> imageOpt = tiffImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.status(404).body(errorBody("NOT_FOUND", "image not found"));
        }
        TiffImage image = imageOpt.get();
        Resource resource = new FileSystemResource(image.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.status(404).body(errorBody("FILE_NOT_FOUND", "image file not found"));
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/tiff"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFileName() + "\"")
            .body(resource);
    }

    @GetMapping("/images/{imageId}/bounds")
    public ResponseEntity<?> getImageBounds(@PathVariable Long imageId) {
        Optional<TiffImage> imageOpt = tiffImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.status(404).body(errorBody("NOT_FOUND", "image not found"));
        }

        TiffImage image = imageOpt.get();
        Map<String, Object> body = new HashMap<>();
        body.put("minLon", image.getMinLon());
        body.put("maxLon", image.getMaxLon());
        body.put("minLat", image.getMinLat());
        body.put("maxLat", image.getMaxLat());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/images/{imageId}/tiles/{z}/{x}/{y}")
    public ResponseEntity<?> getTile(
        @PathVariable Long imageId,
        @PathVariable int z,
        @PathVariable int x,
        @PathVariable int y,
        @RequestParam(defaultValue = "1") int band,
        @RequestParam(defaultValue = "viridis") String colorRamp
    ) {
        if (!tilesEnabled) {
            return ResponseEntity.status(503).body(errorBody("TILE_API_DISABLED", "tile interface is temporarily disabled"));
        }
        Optional<TiffImage> imageOpt = tiffImageRepository.findById(imageId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.status(404).body(errorBody("NOT_FOUND", "image not found"));
        }
        if (z < 0 || x < 0 || y < 0) {
            return badRequest("INVALID_ARGUMENT", "z/x/y must be non-negative");
        }
        if (z < minZoom) {
            return badRequest("ZOOM_TOO_LOW", "z must be >= " + minZoom);
        }
        if (band < 1) {
            return badRequest("INVALID_ARGUMENT", "band must be >= 1");
        }

        try {
            TiffImage image = imageOpt.get();
            if (image.getFileSize() != null && image.getFileSize() > maxImageBytesForLowZoom && z < (minZoom + 2)) {
                return ResponseEntity.status(422).body(errorBody(
                    "ZOOM_TOO_LOW_FOR_LARGE_IMAGE",
                    "large image requires higher zoom level, try z >= " + (minZoom + 2)
                ));
            }

            Path cacheFile = getTileCachePath(imageId, z, x, y, band, colorRamp);
            if (Files.exists(cacheFile)) {
                byte[] png = Files.readAllBytes(cacheFile);
                return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(png);
            }

            byte[] png = pythonService.renderTilePng(image.getFilePath(), z, x, y, band, colorRamp);
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, png, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(png);
        } catch (Exception e) {
            return ResponseEntity.status(422).body(errorBody("TILE_RENDER_FAILED", e.getMessage()));
        }
    }

    private Map<String, Object> toImageListItem(TiffImage image) {
        Map<String, Object> bounds = new HashMap<>();
        bounds.put("minLon", image.getMinLon());
        bounds.put("maxLon", image.getMaxLon());
        bounds.put("minLat", image.getMinLat());
        bounds.put("maxLat", image.getMaxLat());

        Map<String, Object> row = new HashMap<>();
        row.put("imageId", image.getId());
        row.put("fileName", image.getFileName());
        row.put("sensor", extractSensor(image.getDescription()));
        row.put("acquiredAt", image.getAcquisitionDate());
        row.put("bounds", bounds);
        return row;
    }

    private String extractSensor(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        for (String token : description.split(",")) {
            String t = token.trim();
            if (t.startsWith("sensor=")) {
                return t.substring("sensor=".length());
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("acquiredAt is required");
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("acquiredAt format is invalid, use ISO8601");
    }

    private ResponseEntity<Map<String, Object>> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(errorBody(code, message));
    }

    private Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("requestId", null);
        return body;
    }

    private Path getTileCachePath(Long imageId, int z, int x, int y, int band, String colorRamp) {
        String safeRamp = colorRamp == null || colorRamp.isBlank()
            ? "viridis"
            : colorRamp.replaceAll("[^a-zA-Z0-9_-]", "_");
        Path base = Paths.get(tilesCacheDir);
        return base
            .resolve(String.valueOf(imageId))
            .resolve("b" + band)
            .resolve(safeRamp)
            .resolve(String.valueOf(z))
            .resolve(String.valueOf(x))
            .resolve(y + ".png")
            .normalize()
            .toAbsolutePath();
    }

    private Path resolveConfiguredPath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize().toAbsolutePath();
    }

    private Path resolveLocalImagesDir() {
        Path configured = resolveConfiguredPath(localImagesDir);
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize().toAbsolutePath();
        List<Path> candidates = List.of(
            configured,
            userDir.resolve("uploads").resolve("local-images").normalize().toAbsolutePath(),
            userDir.getParent() != null
                ? userDir.getParent().resolve("uploads").resolve("local-images").normalize().toAbsolutePath()
                : configured
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && containsLocalTiff(candidate)) {
                return candidate;
            }
        }
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return configured;
    }

    private boolean containsLocalTiff(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString().toLowerCase())
                .anyMatch(name -> name.endsWith(".tif") || name.endsWith(".tiff"));
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, Object> readBounds(Path filePath) {
        Optional<TiffImage> imageOpt = tiffImageRepository.findByFilePath(filePath.toString());
        if (imageOpt.isPresent()) {
            TiffImage image = imageOpt.get();
            boolean hasDbBounds = image.getMinLon() != null && image.getMaxLon() != null
                && image.getMinLat() != null && image.getMaxLat() != null;
            boolean dbBoundsIn4326 = "EPSG:4326".equalsIgnoreCase(
                image.getBoundsCrs() == null ? "" : image.getBoundsCrs().trim()
            );

            // DB values are trusted only when they are explicitly tagged as EPSG:4326.
            if (hasDbBounds && dbBoundsIn4326) {
                Map<String, Object> bounds = new HashMap<>();
                bounds.put("minLon", image.getMinLon());
                bounds.put("maxLon", image.getMaxLon());
                bounds.put("minLat", image.getMinLat());
                bounds.put("maxLat", image.getMaxLat());
                return bounds;
            }
        }

        try {
            String metadataJson = pythonService.getTiffMetadata(filePath.toString());
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            Object boundsObj = metadata.get("bounds");
            if (boundsObj instanceof Map<?, ?> rawBounds) {
                Map<String, Object> bounds = new HashMap<>();
                bounds.put("minLon", rawBounds.get("left"));
                bounds.put("maxLon", rawBounds.get("right"));
                bounds.put("minLat", rawBounds.get("bottom"));
                bounds.put("maxLat", rawBounds.get("top"));

                // Keep DB and API consistent with waterbody mask CRS (EPSG:4326).
                if (imageOpt.isPresent()) {
                    TiffImage image = imageOpt.get();
                    image.setMinLon(toBigDecimal(rawBounds.get("left")));
                    image.setMaxLon(toBigDecimal(rawBounds.get("right")));
                    image.setMinLat(toBigDecimal(rawBounds.get("bottom")));
                    image.setMaxLat(toBigDecimal(rawBounds.get("top")));
                    image.setBoundsCrs("EPSG:4326");
                    tiffImageRepository.save(image);
                }
                return bounds;
            }
        } catch (Exception ignored) {
        }

        if (imageOpt.isPresent()) {
            TiffImage image = imageOpt.get();
            if (image.getMinLon() != null && image.getMaxLon() != null && image.getMinLat() != null && image.getMaxLat() != null) {
                Map<String, Object> bounds = new HashMap<>();
                bounds.put("minLon", image.getMinLon());
                bounds.put("maxLon", image.getMaxLon());
                bounds.put("minLat", image.getMinLat());
                bounds.put("maxLat", image.getMaxLat());
                return bounds;
            }
        }

        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
