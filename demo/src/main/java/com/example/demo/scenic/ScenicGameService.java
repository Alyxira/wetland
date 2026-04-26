package com.example.demo.scenic;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
class ScenicGameService {

    private final ScenicGuideService scenicGuideService;
    private final Map<String, ScenicGameProgress> progressCache = new ConcurrentHashMap<>();

    ScenicGameService(ScenicGuideService scenicGuideService) {
        this.scenicGuideService = scenicGuideService;
    }

    List<ExploreSpotView> listSpots(String scenicId, String category, String search) {
        String resolvedScenicId = resolveScenicId(scenicId);
        String scenicName = scenicGuideService.getScenicName(resolvedScenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        String normalizedCategory = normalize(category);
        String normalizedSearch = normalize(search);

        return scenicGuideService.getSpots(resolvedScenicId).stream()
            .filter(Objects::nonNull)
            .filter(spot -> normalizedCategory.isBlank() || spotCategory(spot).toLowerCase(Locale.ROOT).contains(normalizedCategory))
            .filter(spot -> normalizedSearch.isBlank() || searchableText(spot).contains(normalizedSearch))
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .map(spot -> toSpotView(scenicName, spot, progress))
            .toList();
    }

    SpotsSummaryView getSpotsSummary(String scenicId) {
        List<ExploreSpotView> spots = listSpots(scenicId, "", "");
        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        spots.forEach(spot -> categoryCount.merge(spot.category(), 1, Integer::sum));
        List<CategoryCount> categories = categoryCount.entrySet().stream()
            .map(item -> new CategoryCount(item.getKey(), item.getValue()))
            .toList();
        List<ExploreSpotView> topRated = spots.stream()
            .sorted(Comparator.comparingDouble(ExploreSpotView::rating).reversed())
            .limit(5)
            .toList();
        int visited = (int) spots.stream().filter(ExploreSpotView::isVisited).count();
        return new SpotsSummaryView(spots.size(), visited, categories, topRated);
    }

    List<ExploreSpotView> getFeaturedSpots(String scenicId) {
        return listSpots(scenicId, "", "").stream()
            .filter(ExploreSpotView::isFeatured)
            .sorted(Comparator.comparingDouble(ExploreSpotView::rating).reversed())
            .toList();
    }

    List<ScenicRouteView> listRoutes(String scenicId) {
        resolveScenicId(scenicId);
        return List.of();
    }

    Optional<ScenicRouteView> getRoute(String scenicId, String routeId) {
        return listRoutes(scenicId).stream().filter(item -> Objects.equals(item.id(), routeId)).findFirst();
    }

    List<ScenicTaskView> listTasks(String scenicId) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        List<ScenicTaskView> missionTasks = scenicGuideService.getMissionsPage(resolvedScenicId).missions().stream()
            .filter(Objects::nonNull)
            .filter(MissionData::enabled)
            .map(this::toTaskFromMission)
            .map(task -> withTaskState(task, progress))
            .toList();
        if (!missionTasks.isEmpty()) {
            return missionTasks;
        }

        List<SpotDetail> spots = scenicGuideService.getSpots(resolvedScenicId).stream()
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .limit(3)
            .toList();
        return spots.stream()
            .map(spot -> new ScenicTaskView(
                "task-" + spot.id(),
                "前往 " + spot.name(),
                "完成一次轻量探索并记录该区域的观察内容。",
                "side",
                "探索经验",
                60,
                progress.completedTaskIds.contains("task-" + spot.id()),
                spot.id()
            ))
            .toList();
    }

    ScenicTaskView completeTask(String scenicId, String taskId) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        ScenicTaskView current = listTasks(resolvedScenicId).stream()
            .filter(item -> Objects.equals(item.id(), taskId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到对应任务"));

        if (progress.completedTaskIds.add(current.id())) {
            progress.totalXp.addAndGet(Math.max(10, current.rewardXp()));
            if (current.spotId() != null && !current.spotId().isBlank()) {
                progress.visitedSpotIds.add(current.spotId());
            }
        }
        return withTaskState(current, progress);
    }

    List<ScenicStampView> listStamps(String scenicId) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        List<SpotDetail> spots = scenicGuideService.getSpots(resolvedScenicId).stream()
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .toList();

        List<ScenicStampView> rewardBased = new ArrayList<>();
        List<RewardData> rewards = scenicGuideService.getRewardsPage(resolvedScenicId).rewards();
        for (int i = 0; i < rewards.size(); i += 1) {
            RewardData reward = rewards.get(i);
            SpotDetail linkedSpot = spots.isEmpty() ? null : spots.get(i % spots.size());
            String stampId = reward.id();
            String collectedAt = progress.collectedStampAt.get(stampId);
            rewardBased.add(new ScenicStampView(
                stampId,
                linkedSpot == null ? "" : linkedSpot.id(),
                linkedSpot == null ? scenicGuideService.getScenicName(resolvedScenicId) : linkedSpot.name(),
                reward.title(),
                reward.description(),
                linkedSpot == null ? "" : linkedSpot.image(),
                normalizeRarity(reward.rarity(), i),
                collectedAt != null,
                collectedAt
            ));
        }
        if (!rewardBased.isEmpty()) {
            return rewardBased;
        }

        return spots.stream()
            .map(spot -> {
                String stampId = "stamp-" + spot.id();
                String collectedAt = progress.collectedStampAt.get(stampId);
                return new ScenicStampView(
                    stampId,
                    spot.id(),
                    spot.name(),
                    spot.name() + " 图章",
                    "在 " + spot.name() + " 完成探索后即可收集该图章。",
                    spot.image(),
                    normalizeRarity("", spot.order()),
                    collectedAt != null,
                    collectedAt
                );
            })
            .toList();
    }

    ScenicStampView collectStamp(String scenicId, String stampId) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        ScenicStampView stamp = listStamps(resolvedScenicId).stream()
            .filter(item -> Objects.equals(item.id(), stampId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到对应图章"));

        String now = Instant.now().toString();
        if (progress.collectedStampAt.putIfAbsent(stamp.id(), now) == null) {
            progress.totalXp.addAndGet(30);
            if (stamp.spotId() != null && !stamp.spotId().isBlank()) {
                progress.visitedSpotIds.add(stamp.spotId());
            }
        }
        String collectedAt = progress.collectedStampAt.get(stamp.id());
        return new ScenicStampView(
            stamp.id(),
            stamp.spotId(),
            stamp.spotName(),
            stamp.name(),
            stamp.description(),
            stamp.imageUrl(),
            stamp.rarity(),
            true,
            collectedAt
        );
    }

    List<ScenicEventView> listEvents(String scenicId, String spotId) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        String normalizedSpotId = spotId == null ? "" : spotId.trim();
        return scenicGuideService.getSpots(resolvedScenicId).stream()
            .filter(spot -> normalizedSpotId.isBlank() || Objects.equals(spot.id(), normalizedSpotId))
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .map(spot -> toEventView(spot, progress))
            .toList();
    }

    ScenicEventInteractResponse interactEvent(String scenicId, String eventId, ScenicEventInteractRequest request) {
        String resolvedScenicId = resolveScenicId(scenicId);
        ScenicGameProgress progress = progressOf(resolvedScenicId);
        ScenicEventView current = listEvents(resolvedScenicId, "").stream()
            .filter(item -> Objects.equals(item.id(), eventId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到对应互动事件"));

        String answer = request == null || request.answer() == null ? "" : request.answer().trim();
        boolean isQuiz = "知识问答".equals(current.type());
        boolean correct = !isQuiz || answer.isBlank() || current.options().isEmpty() || Objects.equals(answer, current.options().get(0));
        String message = correct
            ? "互动完成，已为你记录这次探索进度。"
            : "答案还不完全正确，继续观察周边线索后再尝试一次。";

        if (correct && progress.completedEventIds.add(current.id())) {
            progress.totalXp.addAndGet(Math.max(20, current.rewardXp()));
            if (current.spotId() != null && !current.spotId().isBlank()) {
                progress.visitedSpotIds.add(current.spotId());
            }
        }

        ScenicEventView updated = toEventView(
            scenicGuideService.getSpotById(resolvedScenicId, current.spotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到事件关联景点")),
            progress
        );

        return new ScenicEventInteractResponse(true, correct, message, correct ? updated.rewardXp() : 0, updated);
    }

    ScenicGuideChatResponse guideChat(String scenicId, ScenicGuideChatRequest request) {
        String resolvedScenicId = resolveScenicId(scenicId);
        String message = extractUserMessage(request);
        if (message.isBlank()) {
            message = "请推荐一个适合慢游的路线。";
        }
        String reply = scenicGuideService.buildGuideReply(resolvedScenicId, message);
        List<String> suggestedSpotIds = scenicGuideService.search(resolvedScenicId, message).stream()
            .map(SearchResult::id)
            .limit(3)
            .toList();
        String suggestedRouteId = listRoutes(resolvedScenicId).stream()
            .filter(ScenicRouteView::isRecommended)
            .map(ScenicRouteView::id)
            .findFirst()
            .orElse("");
        return new ScenicGuideChatResponse(reply, suggestedSpotIds, suggestedRouteId);
    }

    private ScenicTaskView withTaskState(ScenicTaskView source, ScenicGameProgress progress) {
        return new ScenicTaskView(
            source.id(),
            source.title(),
            source.description(),
            source.type(),
            source.reward(),
            source.rewardXp(),
            source.isCompleted() || progress.completedTaskIds.contains(source.id()),
            source.spotId()
        );
    }

    private ScenicTaskView toTaskFromMission(MissionData mission) {
        int rewardXp = 70 + mission.steps().size() * 20;
        String reward = mission.rewards().isEmpty()
            ? "探索经验"
            : mission.rewards().stream().map(MissionRewardData::title).filter(Objects::nonNull).collect(Collectors.joining(" / "));
        String spotId = mission.steps().stream()
            .map(MissionStepData::spotId)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse("");
        return new ScenicTaskView(
            mission.id(),
            mission.title(),
            mission.description(),
            mission.type(),
            reward,
            rewardXp,
            false,
            spotId
        );
    }

    private ScenicRouteView createRoute(
        String id,
        String name,
        String description,
        String duration,
        String difficulty,
        List<SpotDetail> routeSpots,
        String scenicName,
        ScenicGameProgress progress,
        boolean recommended
    ) {
        List<ExploreSpotView> spots = routeSpots.stream().map(spot -> toSpotView(scenicName, spot, progress)).toList();
        List<String> spotIds = routeSpots.stream().map(SpotDetail::id).toList();
        String distance = estimateDistance(routeSpots) + " km";
        return new ScenicRouteView(id, name, description, duration, difficulty, spotIds, spots, distance, recommended);
    }

    private List<SpotDetail> pickRouteByKeywords(List<SpotDetail> spots, List<String> keywords, int size) {
        List<SpotDetail> matched = spots.stream()
            .filter(spot -> {
                String text = searchableText(spot);
                return keywords.stream().map(this::normalize).anyMatch(text::contains);
            })
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .limit(size)
            .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return spots.stream().limit(Math.min(size, spots.size())).toList();
    }

    private ScenicEventView toEventView(SpotDetail spot, ScenicGameProgress progress) {
        String eventId = "event-" + spot.id();
        String type = switch (normalize(spot.kind())) {
            case "animal" -> "知识问答";
            case "plant" -> "拍照打卡";
            case "story" -> "故事传说";
            default -> "寻宝互动";
        };
        List<String> options = "知识问答".equals(type)
            ? List.of(spot.name() + " 周边湿地", "高原荒漠", "火山地貌")
            : List.of();
        return new ScenicEventView(
            eventId,
            spot.id(),
            spot.name(),
            type,
            type + " · " + spot.name(),
            spot.discoveryText() == null || spot.discoveryText().isBlank() ? spot.summary() : spot.discoveryText(),
            options,
            Math.max(60, spot.radius()),
            "探索经验",
            40 + Math.max(0, spot.order()) * 5,
            progress.completedEventIds.contains(eventId)
        );
    }

    private ExploreSpotView toSpotView(String scenicName, SpotDetail spot, ScenicGameProgress progress) {
        double rating = Math.min(5.0, 4.2 + (spot.order() % 4) * 0.2);
        int visitCount = 1200 + Math.max(0, spot.order()) * 167;
        boolean featured = spot.order() <= 2 || searchableText(spot).contains("热门");
        boolean visited = progress.visitedSpotIds.contains(spot.id())
            || progress.completedEventIds.contains("event-" + spot.id())
            || progress.collectedStampAt.keySet().stream().anyMatch(id -> id.endsWith(spot.id()));

        return new ExploreSpotView(
            spot.id(),
            spot.name(),
            spot.summary(),
            spot.discoveryText(),
            spotCategory(spot),
            spot.tags() == null ? List.of() : spot.tags(),
            spot.image(),
            List.of(spot.image()),
            rating,
            visitCount,
            "08:30 - 18:00",
            "¥0",
            new SpotLocationView(spot.lat(), spot.lng(), scenicName + " · " + spot.name()),
            "",
            visited,
            featured,
            "2026-01-01T00:00:00Z"
        );
    }

    private String spotCategory(SpotDetail spot) {
        return switch (normalize(spot.kind())) {
            case "animal" -> "自然风光";
            case "plant" -> "自然风光";
            case "story" -> "人文景观";
            case "item" -> "互动体验";
            default -> "景区看点";
        };
    }

    private String normalizeRarity(String value, int index) {
        String key = normalize(value);
        if ("legendary".equals(key) || "传说".equals(key)) return "传说";
        if ("rare".equals(key) || "稀有".equals(key)) return "稀有";
        if ("common".equals(key) || "普通".equals(key)) return "普通";
        return switch (Math.floorMod(index, 3)) {
            case 1 -> "稀有";
            case 2 -> "传说";
            default -> "普通";
        };
    }

    private String estimateDistance(List<SpotDetail> routeSpots) {
        if (routeSpots.size() <= 1) return "1.2";
        double meters = 0;
        for (int i = 1; i < routeSpots.size(); i += 1) {
            SpotDetail prev = routeSpots.get(i - 1);
            SpotDetail curr = routeSpots.get(i);
            double dx = (curr.lng() - prev.lng()) * 93000;
            double dy = (curr.lat() - prev.lat()) * 111000;
            meters += Math.sqrt((dx * dx) + (dy * dy));
        }
        double km = Math.max(0.8, meters / 1000.0);
        return String.format(Locale.ROOT, "%.1f", km);
    }

    private String searchableText(SpotDetail spot) {
        return String.join(" ",
            safe(spot.name()),
            safe(spot.shortName()),
            safe(spot.kind()),
            safe(spot.summary()),
            safe(spot.discoveryTitle()),
            safe(spot.discoveryText()),
            String.join(" ", spot.tags() == null ? List.of() : spot.tags())
        ).toLowerCase(Locale.ROOT);
    }

    private String extractUserMessage(ScenicGuideChatRequest request) {
        if (request == null || request.data() == null || request.data().messages() == null) {
            return "";
        }
        return request.data().messages().stream()
            .filter(Objects::nonNull)
            .filter(item -> "user".equalsIgnoreCase(safe(item.role())))
            .map(ChatMessage::content)
            .filter(Objects::nonNull)
            .reduce((first, second) -> second)
            .orElse("");
    }

    private ScenicGameProgress progressOf(String scenicId) {
        return progressCache.computeIfAbsent(scenicId, key -> new ScenicGameProgress());
    }

    private String resolveScenicId(String scenicId) {
        return scenicGuideService.getScenic(scenicId).id();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

class ScenicGameProgress {
    final Set<String> completedTaskIds = ConcurrentHashMap.newKeySet();
    final Set<String> completedEventIds = ConcurrentHashMap.newKeySet();
    final Map<String, String> collectedStampAt = new ConcurrentHashMap<>();
    final Set<String> visitedSpotIds = ConcurrentHashMap.newKeySet();
    final AtomicInteger totalXp = new AtomicInteger(0);
}
