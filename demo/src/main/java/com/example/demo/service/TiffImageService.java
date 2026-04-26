package com.example.demo.service;

import com.example.demo.dto.TiffImageResponse;
import com.example.demo.entity.Region;
import com.example.demo.entity.TiffImage;
import com.example.demo.entity.WetlandInfo;
import com.example.demo.repository.RegionRepository;
import com.example.demo.repository.TiffImageRepository;
import com.example.demo.repository.WetlandInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TiffImageService {
    
    @Value("${app.upload.dir:uploads/tiff}")
    private String uploadDir;
    
    @Autowired
    private TiffImageRepository tiffImageRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private WetlandInfoRepository wetlandInfoRepository;
    
    @Autowired
    private PythonService pythonService;
    
    public TiffImage uploadTiffImage(MultipartFile file, String region, LocalDateTime acquisitionDate, String description) throws IOException {
        Path uploadPath = resolveConfiguredPath(uploadDir);
        Files.createDirectories(uploadPath);
        
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;
        
        Path filePath = uploadPath.resolve(newFilename).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        TiffImage tiffImage = new TiffImage(originalFilename, filePath.toString(), Files.size(filePath));
        Region resolvedRegion = resolveRegion(region);
        tiffImage.setRegion(resolvedRegion);
        tiffImage.setWetlandId(resolveWetlandId(filePath, originalFilename, resolvedRegion));
        tiffImage.setAcquisitionDate(acquisitionDate);
        tiffImage.setDescription(description);
        
        try {
            String metadataJson = pythonService.getTiffMetadata(filePath.toString());
            parseAndSetMetadata(tiffImage, metadataJson);
        } catch (Exception e) {
            System.err.println("无法读取TIFF元数据: " + e.getMessage());
        }
        
        return tiffImageRepository.save(tiffImage);
    }

    public TiffImage registerLocalTiffImage(Path localFilePath, String description) throws IOException {
        Path normalizedPath = localFilePath.normalize().toAbsolutePath();
        if (!Files.exists(normalizedPath) || Files.isDirectory(normalizedPath)) {
            throw new IOException("local TIFF file not found: " + normalizedPath);
        }

        Optional<TiffImage> existing = tiffImageRepository.findByFilePath(normalizedPath.toString());
        if (existing.isPresent()) {
            TiffImage tiffImage = existing.get();
            tiffImage.setFileName(normalizedPath.getFileName().toString());
            tiffImage.setFileSize(Files.size(normalizedPath));
            tiffImage.setWetlandId(resolveWetlandId(normalizedPath, normalizedPath.getFileName().toString(), tiffImage.getRegion()));
            if (description != null && !description.isBlank()) {
                tiffImage.setDescription(description);
            }
            try {
                String metadataJson = pythonService.getTiffMetadata(normalizedPath.toString());
                parseAndSetMetadata(tiffImage, metadataJson);
            } catch (Exception e) {
                System.err.println("鏃犳硶閲嶆柊璇诲彇鏈湴TIFF鍏冩暟鎹? " + e.getMessage());
            }
            return tiffImageRepository.save(tiffImage);
        }

        String fileName = normalizedPath.getFileName().toString();
        TiffImage tiffImage = new TiffImage(fileName, normalizedPath.toString(), Files.size(normalizedPath));
        tiffImage.setWetlandId(resolveWetlandId(normalizedPath, fileName, null));
        tiffImage.setAcquisitionDate(LocalDateTime.now());
        tiffImage.setDescription(description == null ? "LOCAL_IMAGE" : description);

        try {
            String metadataJson = pythonService.getTiffMetadata(normalizedPath.toString());
            parseAndSetMetadata(tiffImage, metadataJson);
        } catch (Exception e) {
            System.err.println("无法读取本地TIFF元数据: " + e.getMessage());
        }

        return tiffImageRepository.save(tiffImage);
    }
    
    private void parseAndSetMetadata(TiffImage tiffImage, String metadataJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> metadata = mapper.readValue(metadataJson, java.util.Map.class);
            
            if (metadata.containsKey("width")) {
                tiffImage.setWidth(((Number) metadata.get("width")).intValue());
            }
            if (metadata.containsKey("height")) {
                tiffImage.setHeight(((Number) metadata.get("height")).intValue());
            }
            if (metadata.containsKey("bandCount")) {
                tiffImage.setBandCount(((Number) metadata.get("bandCount")).intValue());
            }
            // For frontend/map consistency, store displayed bounds CRS in `crs` as EPSG:4326.
            tiffImage.setCrs("EPSG:4326");
            if (metadata.containsKey("boundsCrs")) {
                tiffImage.setBoundsCrs((String) metadata.get("boundsCrs"));
            } else if (tiffImage.getBoundsCrs() == null || tiffImage.getBoundsCrs().isBlank()) {
                tiffImage.setBoundsCrs("EPSG:4326");
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> bounds = (java.util.Map<String, Object>) metadata.get("bounds");
            if (bounds != null) {
                if (bounds.containsKey("left")) {
                    tiffImage.setMinLon(toBigDecimal(bounds.get("left")));
                }
                if (bounds.containsKey("right")) {
                    tiffImage.setMaxLon(toBigDecimal(bounds.get("right")));
                }
                if (bounds.containsKey("bottom")) {
                    tiffImage.setMinLat(toBigDecimal(bounds.get("bottom")));
                }
                if (bounds.containsKey("top")) {
                    tiffImage.setMaxLat(toBigDecimal(bounds.get("top")));
                }
            }
        } catch (Exception e) {
            System.err.println("解析TIFF元数据失败: " + e.getMessage());
        }
    }
    
    public List<TiffImageResponse> getAllTiffImages() {
        return tiffImageRepository.findAllByOrderByUploadTimeDesc().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public Optional<TiffImage> getTiffImageById(Long id) {
        return tiffImageRepository.findById(id);
    }
    
    public List<TiffImageResponse> getTiffImagesByRegion(String region) {
        return tiffImageRepository.findByRegionNameOrderByUploadTimeDesc(region).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<TiffImageResponse> getTiffImagesByBoundingBox(BigDecimal minLon, BigDecimal maxLon, BigDecimal minLat, BigDecimal maxLat) {
        return tiffImageRepository.findByBoundingBox(minLon, maxLon, minLat, maxLat).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    public List<String> getAllRegions() {
        return tiffImageRepository.findAllRegions();
    }
    
    public void deleteTiffImage(Long id) throws IOException {
        Optional<TiffImage> tiffImageOpt = tiffImageRepository.findById(id);
        if (tiffImageOpt.isPresent()) {
            TiffImage tiffImage = tiffImageOpt.get();
            Path filePath = Paths.get(tiffImage.getFilePath()).normalize().toAbsolutePath();
            Files.deleteIfExists(filePath);
            tiffImageRepository.delete(tiffImage);
        }
    }

    private Path resolveConfiguredPath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize().toAbsolutePath();
    }

    private Region resolveRegion(String regionName) {
        if (regionName == null || regionName.isBlank()) {
            return null;
        }

        return regionRepository.findByRegionName(regionName.trim())
            .orElseGet(() -> {
                Region region = new Region();
                region.setRegionName(regionName.trim());
                region.setRegionCode(generateRegionCode(regionName));
                return regionRepository.save(region);
            });
    }

    private Long resolveWetlandId(Path filePath, String fileName, Region region) throws IOException {
        if (region != null && region.getWetlandId() != null) {
            return region.getWetlandId();
        }

        List<WetlandInfo> wetlands = wetlandInfoRepository.findAllActiveWetlands();
        if (wetlands.isEmpty()) {
            throw new IOException("no active wetland found for TIFF: " + fileName);
        }
        if (wetlands.size() == 1) {
            return wetlands.get(0).getWetlandId();
        }

        String normalizedPath = filePath == null ? "" : normalizeToken(filePath.toString());
        String normalizedName = normalizeToken(fileName);

        List<String> candidates = new ArrayList<>();
        if (filePath != null) {
            Path current = filePath.getParent();
            while (current != null) {
                String name = current.getFileName() == null ? "" : current.getFileName().toString();
                if (!name.isBlank()) {
                    candidates.add(name);
                }
                current = current.getParent();
            }
        }
        if (fileName != null && !fileName.isBlank()) {
            candidates.add(fileName);
        }

        for (WetlandInfo wetland : wetlands) {
            String wetlandName = wetland.getWetlandName();
            if (wetlandName == null || wetlandName.isBlank()) {
                continue;
            }
            String normalizedWetland = normalizeToken(wetlandName);
            String simplifiedWetland = simplifyWetlandToken(normalizedWetland);

            if (!normalizedPath.isBlank() && (normalizedPath.contains(normalizedWetland) || normalizedPath.contains(simplifiedWetland))) {
                return wetland.getWetlandId();
            }
            if (!normalizedName.isBlank() && (normalizedName.contains(normalizedWetland) || normalizedName.contains(simplifiedWetland))) {
                return wetland.getWetlandId();
            }
            for (String candidate : candidates) {
                String normalizedCandidate = normalizeToken(candidate);
                if (normalizedCandidate.contains(normalizedWetland)
                    || normalizedCandidate.contains(simplifiedWetland)
                    || normalizedWetland.contains(normalizedCandidate)
                    || simplifiedWetland.contains(normalizedCandidate)) {
                    return wetland.getWetlandId();
                }
            }
        }

        throw new IOException("unable to resolve wetland_id for TIFF: " + fileName);
    }

    private String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
            .replace('\\', '/')
            .replaceAll("\\.tiff?$", "")
            .replaceAll("\\s+", "")
            .toLowerCase(Locale.ROOT);
    }

    private String simplifyWetlandToken(String normalizedWetland) {
        return normalizedWetland
            .replace("湿地", "")
            .replace("景区", "");
    }

    private String generateRegionCode(String regionName) {
        String normalized = regionName.trim()
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "_")
            .replaceAll("^_+|_+$", "")
            .toUpperCase(Locale.ROOT);
        String baseCode = normalized.isBlank()
            ? "REGION_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
            : normalized;
        String candidate = baseCode;
        int suffix = 1;
        while (regionRepository.findByRegionCode(candidate).isPresent()) {
            candidate = baseCode + "_" + suffix++;
        }
        return candidate;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
    
    private TiffImageResponse convertToResponse(TiffImage tiffImage) {
        TiffImageResponse response = new TiffImageResponse();
        response.setId(tiffImage.getId());
        response.setFileName(tiffImage.getFileName());
        response.setRegion(tiffImage.getRegion() != null ? tiffImage.getRegion().getRegionName() : null);
        response.setAcquisitionDate(tiffImage.getAcquisitionDate());
        response.setMinLon(tiffImage.getMinLon());
        response.setMaxLon(tiffImage.getMaxLon());
        response.setMinLat(tiffImage.getMinLat());
        response.setMaxLat(tiffImage.getMaxLat());
        response.setWidth(tiffImage.getWidth());
        response.setHeight(tiffImage.getHeight());
        response.setBandCount(tiffImage.getBandCount());
        response.setUploadTime(tiffImage.getUploadTime());
        response.setDescription(tiffImage.getDescription());
        return response;
    }
}

