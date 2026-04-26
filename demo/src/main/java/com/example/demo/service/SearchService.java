package com.example.demo.service;

import com.example.demo.dto.SearchResponse;
import com.example.demo.entity.Post;
import com.example.demo.entity.WetlandFloraFauna;
import com.example.demo.entity.WetlandInfo;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.WetlandFloraFaunaRepository;
import com.example.demo.repository.WetlandInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SearchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WetlandInfoRepository wetlandInfoRepository;
    private final PostRepository postRepository;
    private final WetlandFloraFaunaRepository wetlandFloraFaunaRepository;

    public SearchService(
        WetlandInfoRepository wetlandInfoRepository,
        PostRepository postRepository,
        WetlandFloraFaunaRepository wetlandFloraFaunaRepository
    ) {
        this.wetlandInfoRepository = wetlandInfoRepository;
        this.postRepository = postRepository;
        this.wetlandFloraFaunaRepository = wetlandFloraFaunaRepository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String keyword, String type) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedType = type == null ? "all" : type.trim().toLowerCase();

        if (!StringUtils.hasText(normalizedKeyword)) {
            return new SearchResponse(true, "搜索成功", List.of());
        }

        List<SearchResponse.SearchItem> items = new ArrayList<>();
        List<WetlandInfo> matchedWetlands = List.of();

        if ("all".equals(normalizedType) || "wetland".equals(normalizedType)) {
            matchedWetlands = wetlandInfoRepository.searchByKeyword(normalizedKeyword);
            items.addAll(matchedWetlands.stream().map(this::toWetlandItem).toList());
        } else {
            matchedWetlands = wetlandInfoRepository.searchByKeyword(normalizedKeyword);
        }

        if ("all".equals(normalizedType) || "post".equals(normalizedType)) {
            items.addAll(searchPosts(normalizedKeyword, matchedWetlands));
        }

        if ("all".equals(normalizedType) || "flora".equals(normalizedType)) {
            items.addAll(searchFlora(normalizedKeyword, matchedWetlands));
        }

        return new SearchResponse(true, "搜索成功", items);
    }

    private List<SearchResponse.SearchItem> searchPosts(String keyword, List<WetlandInfo> matchedWetlands) {
        Map<Long, Post> collected = new LinkedHashMap<>();

        postRepository.findByTitleContainingOrContentContainingOrderByCreatedAtDesc(keyword, keyword)
            .forEach(post -> collected.put(post.getId(), post));

        if (!matchedWetlands.isEmpty()) {
            Set<String> relatedTerms = matchedWetlands.stream()
                .map(WetlandInfo::getWetlandName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

            if (!relatedTerms.isEmpty()) {
                postRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(post -> matchesAny(post, relatedTerms))
                    .forEach(post -> collected.putIfAbsent(post.getId(), post));
            }
        }

        return collected.values().stream().map(this::toPostItem).toList();
    }

    private List<SearchResponse.SearchItem> searchFlora(String keyword, List<WetlandInfo> matchedWetlands) {
        Map<Long, WetlandFloraFauna> collected = new LinkedHashMap<>();

        wetlandFloraFaunaRepository.searchByKeyword(keyword)
            .forEach(flora -> collected.put(flora.getId(), flora));

        for (WetlandInfo wetland : matchedWetlands) {
            if (wetland.getWetlandId() == null) {
                continue;
            }
            wetlandFloraFaunaRepository.findAllByRelatedWetlandIdOrderByCreatedTimeDesc(wetland.getWetlandId())
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .forEach(item -> collected.putIfAbsent(item.getId(), item));
        }

        return collected.values().stream().map(this::toFloraItem).toList();
    }

    private SearchResponse.SearchItem toWetlandItem(WetlandInfo wetland) {
        SearchResponse.SearchItem item = new SearchResponse.SearchItem();
        item.setType("wetland");
        item.setId(wetland.getWetlandId());
        item.setTitle(wetland.getWetlandName());
        item.setDescription(truncate(wetland.getDescription(), "暂无湿地介绍。", 110));
        item.setImage(normalizeImagePath(wetland.getImagePath()));
        item.setTag(wetland.getTags());
        item.setMeta(wetland.getCoordinateRange());
        item.setPath("/overview");
        return item;
    }

    private SearchResponse.SearchItem toPostItem(Post post) {
        SearchResponse.SearchItem item = new SearchResponse.SearchItem();
        item.setType("post");
        item.setId(post.getId());
        item.setTitle(post.getTitle());
        item.setDescription(truncate(post.getContent(), "暂无帖子内容。", 110));
        item.setImage(normalizeImagePath(post.getImage()));
        item.setTag(post.getTag());
        item.setMeta(post.getCreatedAt() == null ? "社区内容" : DATE_FORMATTER.format(post.getCreatedAt()));
        item.setPath("/community");
        return item;
    }

    private SearchResponse.SearchItem toFloraItem(WetlandFloraFauna flora) {
        Long primaryWetlandId = resolvePrimaryWetlandId(flora.getWetlandId()).orElse(null);
        SearchResponse.SearchItem item = new SearchResponse.SearchItem();
        item.setType("flora");
        item.setId(flora.getId());
        item.setTitle(flora.getName());
        item.setDescription(truncate(flora.getDescription(), "暂无物种介绍。", 110));
        item.setImage(normalizeImagePath(flora.getImagePath()));
        item.setTag("珍稀动植物");
        item.setMeta(resolveWetlandName(flora.getWetlandId()));
        item.setPath(primaryWetlandId == null ? "/flora/" + flora.getId() : "/flora/" + flora.getId() + "?wetlandId=" + primaryWetlandId);
        return item;
    }

    private String truncate(String value, String fallback, int limit) {
        String source = StringUtils.hasText(value) ? value.trim() : fallback;
        return source.length() > limit ? source.substring(0, limit) + "..." : source;
    }

    private boolean matchesAny(Post post, Set<String> terms) {
        String title = safeLower(post.getTitle());
        String content = safeLower(post.getContent());
        String tag = safeLower(post.getTag());

        for (String term : terms) {
            String normalizedTerm = safeLower(term);
            if (!StringUtils.hasText(normalizedTerm)) {
                continue;
            }
            if (title.contains(normalizedTerm) || content.contains(normalizedTerm) || tag.contains(normalizedTerm)) {
                return true;
            }
        }
        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String resolveWetlandName(String wetlandIds) {
        Optional<Long> primaryWetlandId = resolvePrimaryWetlandId(wetlandIds);
        if (primaryWetlandId.isEmpty()) {
            return "珍稀动植物";
        }

        return wetlandInfoRepository.findById(primaryWetlandId.get())
            .filter(WetlandInfo::getActive)
            .map(WetlandInfo::getWetlandName)
            .orElse("珍稀动植物");
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
