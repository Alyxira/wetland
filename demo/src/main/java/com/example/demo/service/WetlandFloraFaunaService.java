package com.example.demo.service;

import com.example.demo.dto.FloraFaunaResponse;
import com.example.demo.entity.WetlandFloraFauna;
import com.example.demo.entity.WetlandInfo;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.WetlandFloraFaunaRepository;
import com.example.demo.repository.WetlandInfoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

@Service
public class WetlandFloraFaunaService {

    private final WetlandFloraFaunaRepository wetlandFloraFaunaRepository;
    private final WetlandInfoRepository wetlandInfoRepository;

    public WetlandFloraFaunaService(
        WetlandFloraFaunaRepository wetlandFloraFaunaRepository,
        WetlandInfoRepository wetlandInfoRepository
    ) {
        this.wetlandFloraFaunaRepository = wetlandFloraFaunaRepository;
        this.wetlandInfoRepository = wetlandInfoRepository;
    }

    @Transactional(readOnly = true)
    public FloraFaunaResponse getByWetlandId(Long wetlandId) {
        WetlandInfo wetland = wetlandInfoRepository.findById(wetlandId)
            .filter(WetlandInfo::getActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "湿地信息不存在"));

        return new FloraFaunaResponse(
            true,
            "获取珍稀动植物列表成功",
            wetlandFloraFaunaRepository.findAllByRelatedWetlandIdOrderByCreatedTimeDesc(wetlandId)
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(item -> toData(item, wetland.getWetlandName()))
                .toList()
        );
    }

    @Transactional(readOnly = true)
    public FloraFaunaResponse getDetail(Long id) {
        WetlandFloraFauna entity = wetlandFloraFaunaRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "珍稀动植物信息不存在"));

        String wetlandName = resolvePrimaryWetlandId(entity.getWetlandId())
            .flatMap(wetlandInfoRepository::findById)
            .filter(WetlandInfo::getActive)
            .map(WetlandInfo::getWetlandName)
            .orElse("未知景区");

        return new FloraFaunaResponse(true, "获取珍稀动植物详情成功", toData(entity, wetlandName));
    }

    private FloraFaunaResponse.FloraFaunaData toData(WetlandFloraFauna entity, String wetlandName) {
        FloraFaunaResponse.FloraFaunaData data = new FloraFaunaResponse.FloraFaunaData();
        data.setId(entity.getId());
        data.setWetlandId(entity.getWetlandId());
        data.setWetlandName(wetlandName);
        data.setName(entity.getName());
        data.setDescription(entity.getDescription());
        data.setImagePath(normalizeImagePath(entity.getImagePath()));
        data.setActive(entity.getActive());
        data.setCreatedTime(entity.getCreatedTime());
        return data;
    }

    private Optional<Long> resolvePrimaryWetlandId(String wetlandIds) {
        if (!StringUtils.hasText(wetlandIds)) {
            return Optional.empty();
        }

        for (String part : wetlandIds.split(",")) {
            String candidate = part.trim();
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            try {
                return Optional.of(Long.parseLong(candidate));
            } catch (NumberFormatException ignored) {
                // Ignore invalid ids and continue scanning the remaining values.
            }
        }
        return Optional.empty();
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
