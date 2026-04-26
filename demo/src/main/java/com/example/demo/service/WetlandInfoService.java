package com.example.demo.service;

import com.example.demo.dto.WetlandRequest;
import com.example.demo.dto.WetlandResponse;
import com.example.demo.entity.WetlandInfo;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.WetlandInfoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

@Service
public class WetlandInfoService {

    private final WetlandInfoRepository wetlandInfoRepository;

    public WetlandInfoService(WetlandInfoRepository wetlandInfoRepository) {
        this.wetlandInfoRepository = wetlandInfoRepository;
    }

    @Transactional(readOnly = true)
    public WetlandResponse getWetlands(String keyword, String tag) {
        List<WetlandInfo> wetlands;
        if (StringUtils.hasText(keyword) && StringUtils.hasText(tag)) {
            wetlands = wetlandInfoRepository.findByTagAndNameKeyword(tag.trim(), keyword.trim());
        } else if (StringUtils.hasText(keyword)) {
            wetlands = wetlandInfoRepository.findByWetlandNameContainingAndActiveTrueOrderByCreatedTimeDesc(keyword.trim());
        } else if (StringUtils.hasText(tag)) {
            wetlands = wetlandInfoRepository.findByTagsContainingAndActiveTrueOrderByCreatedTimeDesc(tag.trim());
        } else {
            wetlands = wetlandInfoRepository.findAllActiveWetlands();
        }

        return new WetlandResponse(true, "获取湿地列表成功", wetlands.stream().map(this::toData).toList());
    }

    @Transactional(readOnly = true)
    public WetlandResponse getWetland(Long wetlandId) {
        WetlandInfo wetland = wetlandInfoRepository.findById(wetlandId)
            .filter(WetlandInfo::getActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "湿地信息不存在"));
        return new WetlandResponse(true, "获取湿地详情成功", toData(wetland));
    }

    @Transactional
    public WetlandResponse createWetland(WetlandRequest request) {
        if (wetlandInfoRepository.existsByWetlandName(request.getWetlandName().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "湿地名称已存在");
        }

        WetlandInfo wetland = new WetlandInfo();
        applyRequest(wetland, request);
        WetlandInfo savedWetland = wetlandInfoRepository.save(wetland);
        return new WetlandResponse(true, "湿地信息创建成功", toData(savedWetland));
    }

    @Transactional
    public WetlandResponse updateWetland(Long wetlandId, WetlandRequest request) {
        WetlandInfo wetland = wetlandInfoRepository.findById(wetlandId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "湿地信息不存在"));

        String requestedName = request.getWetlandName().trim();
        if (!wetland.getWetlandName().equals(requestedName) && wetlandInfoRepository.existsByWetlandName(requestedName)) {
            throw new ApiException(HttpStatus.CONFLICT, "湿地名称已存在");
        }

        applyRequest(wetland, request);
        WetlandInfo savedWetland = wetlandInfoRepository.save(wetland);
        return new WetlandResponse(true, "湿地信息更新成功", toData(savedWetland));
    }

    @Transactional
    public WetlandResponse deleteWetland(Long wetlandId) {
        WetlandInfo wetland = wetlandInfoRepository.findById(wetlandId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "湿地信息不存在"));
        wetland.setActive(false);
        wetlandInfoRepository.save(wetland);
        return new WetlandResponse(true, "湿地信息删除成功");
    }

    private void applyRequest(WetlandInfo wetland, WetlandRequest request) {
        wetland.setWetlandName(request.getWetlandName().trim());
        wetland.setImagePath(normalizeImagePath(request.getImagePath()));
        wetland.setCoordinateRange(request.getCoordinateRange());
        wetland.setDescription(request.getDescription());
        wetland.setFloraFaunaInfo(request.getFloraFaunaInfo());
        wetland.setTags(request.getTags());
        if (wetland.getActive() == null) {
            wetland.setActive(true);
        }
    }

    private WetlandResponse.WetlandData toData(WetlandInfo wetland) {
        WetlandResponse.WetlandData data = new WetlandResponse.WetlandData();
        data.setId(wetland.getWetlandId());
        data.setWetlandName(wetland.getWetlandName());
        data.setImagePath(normalizeImagePath(wetland.getImagePath()));
        data.setCoordinateRange(wetland.getCoordinateRange());
        data.setDescription(wetland.getDescription());
        data.setFloraFaunaInfo(wetland.getFloraFaunaInfo());
        data.setTags(wetland.getTags());
        data.setActive(wetland.getActive());
        data.setCreatedTime(wetland.getCreatedTime());
        return data;
    }

    private String normalizeImagePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return rawPath;
        }

        String normalized = rawPath.trim().replace('\\', '/');
        String lowerCasePath = normalized.toLowerCase(Locale.ROOT);

        if (lowerCasePath.startsWith("http://") || lowerCasePath.startsWith("https://")) {
            return normalized;
        }

        int markerIndex = lowerCasePath.indexOf("/src/upload/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "/src/upload/".length()));
        }

        markerIndex = lowerCasePath.indexOf("src/upload/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "src/upload/".length()));
        }

        markerIndex = lowerCasePath.indexOf("/uploads/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "/uploads/".length()));
        }

        markerIndex = lowerCasePath.indexOf("uploads/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "uploads/".length()));
        }

        markerIndex = lowerCasePath.indexOf("/upload/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "/upload/".length()));
        }

        markerIndex = lowerCasePath.indexOf("upload/");
        if (markerIndex >= 0) {
            return toUploadUrl(normalized.substring(markerIndex + "upload/".length()));
        }

        if (looksLikeLocalFile(normalized)) {
            return null;
        }

        return normalized.startsWith("/") ? normalized : toUploadUrl(normalized);
    }

    private String toUploadUrl(String relativePath) {
        String cleaned = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.isEmpty() ? null : "/uploads/" + cleaned;
    }

    private boolean looksLikeLocalFile(String pathValue) {
        Path path = Paths.get(pathValue).normalize();
        return path.isAbsolute();
    }
}
