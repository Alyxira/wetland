package com.example.demo.scenic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
class ScenicRegistryService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private static final Map<String, String> SCENIC_ID_ALIAS_MAP = Map.of(
        "muguang-wetland", "jiuzhaigou"
    );

    private volatile ScenicIndexData cachedIndex;

    ScenicRegistryService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    ScenicIndexData getScenicIndex() {
        ScenicIndexData local = cachedIndex;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (cachedIndex == null) {
                cachedIndex = readRequired("classpath:scenics/index.json", ScenicIndexData.class);
            }
            return cachedIndex;
        }
    }

    String getDefaultScenicId() {
        String defaultId = getScenicIndex().defaultScenicId();
        if (defaultId == null || defaultId.isBlank()) {
            throw new IllegalStateException("index.json missing defaultScenicId");
        }
        return defaultId;
    }

    String normalizeScenicId(String scenicId) {
        String value = scenicId == null ? "" : scenicId.trim();
        if (value.isBlank()) {
            return getDefaultScenicId();
        }
        return SCENIC_ID_ALIAS_MAP.getOrDefault(value, value);
    }

    String requireScenicId(String scenicId) {
        String normalized = normalizeScenicId(scenicId);
        boolean exists = getScenicIndex().scenics().stream().anyMatch(item -> Objects.equals(item.id(), normalized));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenic not found");
        }
        return normalized;
    }

    ScenicSummary getScenicSummary(String scenicId) {
        String normalized = requireScenicId(scenicId);
        return getScenicIndex().scenics().stream()
            .filter(item -> Objects.equals(item.id(), normalized))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scenic not found"));
    }

    private <T> T readRequired(String location, Class<T> type) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing config: " + location);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, type);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read config: " + location, ex);
        }
    }
}

@Service
class ScenicGuideService {

    private final ScenicRegistryService scenicRegistryService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String defaultModelUrl;
    private final String defaultWidgetName;
    private final String defaultGreeting;
    private final Map<String, ScenicBundle> scenicBundleCache = new ConcurrentHashMap<>();

    ScenicGuideService(
        ScenicRegistryService scenicRegistryService,
        ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        @Value("${scenic.live2d.model-url:/assets/live2d/model/live2d/main.model3.json}") String defaultModelUrl,
        @Value("${scenic.live2d.widget-name:团子}") String defaultWidgetName,
        @Value("${scenic.live2d.greeting:你好呀，我是团子，可以结合当前景区为你推荐景点与路线。}") String defaultGreeting
    ) {
        this.scenicRegistryService = scenicRegistryService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.defaultModelUrl = defaultModelUrl;
        this.defaultWidgetName = defaultWidgetName;
        this.defaultGreeting = defaultGreeting;
    }

    ScenicIndexData getScenicIndex() {
        return scenicRegistryService.getScenicIndex();
    }

    String getDefaultScenicId() {
        return scenicRegistryService.getDefaultScenicId();
    }

    ScenicDetailResponse getScenic(String scenicId) {
        return getBundle(scenicId).scenic();
    }

    String getScenicName(String scenicId) {
        return getBundle(scenicId).scenic().name();
    }

    HomePageData getHomePage(String scenicId) {
        return getBundle(scenicId).home();
    }

    ExplorePageData getExplorePage(String scenicId) {
        return getBundle(scenicId).explore();
    }

    MissionsPageData getMissionsPage(String scenicId) {
        return getBundle(scenicId).missions();
    }

    RewardsPageData getRewardsPage(String scenicId) {
        return getBundle(scenicId).rewards();
    }

    CloudTourData getCloudTourPage(String scenicId) {
        return getBundle(scenicId).cloud();
    }

    List<SpotDetail> getSpots(String scenicId) {
        return getBundle(scenicId).spots();
    }

    Optional<SpotDetail> getSpotById(String scenicId, String spotId) {
        String target = spotId == null ? "" : spotId.trim();
        if (target.isBlank()) {
            return Optional.empty();
        }
        return getSpots(scenicId).stream().filter(spot -> Objects.equals(spot.id(), target)).findFirst();
    }

    Live2dConfig getLive2dConfig(String scenicId) {
        return getBundle(scenicId).live2d();
    }

    List<SearchResult> search(String scenicId, String keyword) {
        String normalized = normalize(keyword);
        List<SpotDetail> spots = getSpots(scenicId);
        if (spots.isEmpty()) {
            return List.of();
        }
        if (normalized.isBlank()) {
            return spots.stream()
                .sorted(Comparator.comparingInt(SpotDetail::order))
                .limit(10)
                .map(spot -> new SearchResult(
                    spot.id(),
                    spot.name(),
                    safeValue(spot.kind()),
                    safeValue(spot.summary()),
                    safeList(spot.tags()),
                    spot.lng(),
                    spot.lat(),
                    safeValue(spot.image()),
                    "热门推荐"
                ))
                .toList();
        }

        List<ScoredSpot> scored = spots.stream()
            .map(spot -> new ScoredSpot(spot, scoreSpot(spot, normalized)))
            .filter(item -> item.score() > 0)
            .sorted(Comparator.comparingInt(ScoredSpot::score).reversed().thenComparingInt(item -> item.spot().order()))
            .toList();

        return scored.stream()
            .limit(15)
            .map(item -> {
                SpotDetail spot = item.spot();
                return new SearchResult(
                    spot.id(),
                    spot.name(),
                    safeValue(spot.kind()),
                    safeValue(spot.summary()),
                    safeList(spot.tags()),
                    spot.lng(),
                    spot.lat(),
                    safeValue(spot.image()),
                    "相关度 " + item.score()
                );
            })
            .toList();
    }

    String buildGuideReply(String scenicId, String message) {
        List<SearchResult> related = search(scenicId, message);
        if (!related.isEmpty()) {
            String joined = related.stream()
                .limit(3)
                .map(SearchResult::name)
                .collect(Collectors.joining("、"));
            return "我找到了这些相关景点：" + joined + "。如果你愿意，我可以继续给你推荐一条路线。";
        }
        return "我可以为你介绍景区亮点、玩法建议，或按你的偏好推荐一条路线。";
    }

    RoutePlanResponse buildPlan(String scenicId, RouteRequest request) {
        List<SpotDetail> spots = getSpots(scenicId);
        if (spots.isEmpty()) {
            return new RoutePlanResponse("Assistant Route", "No available scenic spots.", List.of(), List.of(), List.of());
        }

        int stopCount = switch (safeValue(request.duration())) {
            case "short" -> 3;
            case "medium" -> 5;
            case "half" -> 7;
            case "full" -> 10;
            default -> 5;
        };

        List<String> interests = safeList(request.interests()).stream()
            .map(this::safeValue)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();

        List<ScoredSpot> scored = spots.stream()
            .map(spot -> new ScoredSpot(spot, scoreForRoute(spot, request, interests)))
            .sorted(Comparator.comparingInt(ScoredSpot::score).reversed().thenComparingInt(item -> item.spot().order()))
            .toList();

        LinkedHashMap<String, SpotDetail> selected = new LinkedHashMap<>();
        for (ScoredSpot item : scored) {
            selected.putIfAbsent(item.spot().id(), item.spot());
            if (selected.size() >= stopCount) {
                break;
            }
        }

        if (selected.isEmpty()) {
            spots.stream().sorted(Comparator.comparingInt(SpotDetail::order)).limit(stopCount).forEach(spot -> selected.put(spot.id(), spot));
        }

        List<RouteStop> routeStops = selected.values().stream()
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .map(spot -> new RouteStop(spot.id(), spot.name(), spot.kind(), spot.lng(), spot.lat()))
            .toList();

        List<String> tags = new ArrayList<>();
        tags.add("duration:" + safeValue(request.duration()));
        tags.add("pace:" + safeValue(request.pace()));
        tags.add("group:" + safeValue(request.group()));
        interests.forEach(item -> tags.add("interest:" + item));

        List<String> reasons = List.of(
            "Stops are selected by your pacing preference.",
            "The sequence keeps transfer distance moderate.",
            "You can start from the first stop and adjust on map."
        );

        String title = switch (safeValue(request.duration())) {
            case "short" -> "短时路线";
            case "medium" -> "半日路线";
            case "half" -> "半天深度路线";
            case "full" -> "全天探索路线";
            default -> "推荐路线";
        };
        String description = "共 " + routeStops.size() + " 站，可在探索地图中一键应用。";
        return new RoutePlanResponse(title, description, reasons, tags, routeStops);
    }

    private int scoreSpot(SpotDetail spot, String keyword) {
        String text = searchableText(spot);
        int score = 0;
        if (normalize(spot.name()).contains(keyword)) score += 10;
        if (normalize(spot.shortName()).contains(keyword)) score += 8;
        if (normalize(spot.kind()).contains(keyword)) score += 6;
        if (normalize(spot.summary()).contains(keyword)) score += 5;
        for (String tag : safeList(spot.tags())) {
            if (normalize(tag).contains(keyword)) {
                score += 4;
            }
        }
        return score;
    }

    private int scoreForRoute(SpotDetail spot, RouteRequest request, List<String> interests) {
        int score = 100 - Math.max(0, spot.order() * 3);
        String text = searchableText(spot);

        String pace = safeValue(request.pace());
        if ("photo".equals(pace) && (text.contains("拍") || text.contains("景") || text.contains("湖"))) score += 15;
        if ("relax".equals(pace) && (text.contains("慢") || text.contains("栈道") || text.contains("湖"))) score += 10;
        if ("learn".equals(pace) && (text.contains("生态") || text.contains("故事") || text.contains("介绍"))) score += 10;

        String group = safeValue(request.group());
        if ("family".equals(group) && !text.contains("高海拔")) score += 8;
        if ("pair".equals(group) && (text.contains("拍") || text.contains("倒影"))) score += 8;

        for (String interest : interests) {
            if (text.contains(normalize(interest))) {
                score += 8;
            }
        }

        return score;
    }

    private String searchableText(SpotDetail spot) {
        return String.join(" ",
            safeValue(spot.name()),
            safeValue(spot.shortName()),
            safeValue(spot.kind()),
            safeValue(spot.summary()),
            safeValue(spot.discoveryTitle()),
            safeValue(spot.discoveryText()),
            String.join(" ", safeList(spot.tags()))
        ).toLowerCase(Locale.ROOT);
    }

    private ScenicBundle getBundle(String scenicId) {
        String normalized = scenicRegistryService.requireScenicId(scenicId);
        return scenicBundleCache.computeIfAbsent(normalized, this::loadBundle);
    }

    private ScenicBundle loadBundle(String scenicId) {
        ScenicSummary summary = scenicRegistryService.getScenicSummary(scenicId);
        String dataScenicId = resolveDataScenicId(scenicId);

        ScenicConfigFile scenicConfig = readRequired(dataScenicId, "scenic.json", ScenicConfigFile.class);
        if (scenicConfig.id() != null && !scenicConfig.id().isBlank() && !Objects.equals(scenicConfig.id(), dataScenicId)) {
            throw new IllegalStateException("scenic.json scenicId mismatch: " + dataScenicId);
        }

        SpotsFile spotsFile = readRequired(dataScenicId, "spots.json", SpotsFile.class);
        HomeConfigFile homeConfig = readOptional(dataScenicId, "home.json", HomeConfigFile.class).orElse(null);
        ExploreConfigFile exploreConfig = readOptional(dataScenicId, "explore.json", ExploreConfigFile.class).orElse(null);
        CloudTourConfigFile cloudConfig = readOptional(dataScenicId, "cloud-tour.json", CloudTourConfigFile.class).orElse(null);
        Live2dConfigFile live2dConfigFile = readOptional(dataScenicId, "live2d.json", Live2dConfigFile.class).orElse(null);
        MissionsConfigFile missionsConfig = readOptional(dataScenicId, "missions.json", MissionsConfigFile.class).orElse(null);
        RewardsConfigFile rewardsConfig = readOptional(dataScenicId, "rewards.json", RewardsConfigFile.class).orElse(null);

        ScenicFeatures features = scenicConfig.features() == null ? ScenicFeatures.defaults() : scenicConfig.features();
        List<SpotDetail> spots = safeList(spotsFile.spots()).stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .toList();

        ScenicDetailResponse scenic = new ScenicDetailResponse(
            scenicId,
            coalesce(scenicConfig.name(), summary.name()),
            coalesce(scenicConfig.subtitle(), summary.subtitle()),
            coalesce(scenicConfig.description(), ""),
            scenicConfig.theme(),
            features,
            scenicConfig.navigation(),
            scenicConfig.seo(),
            coalesce(summary.coverImage(), ""),
            coalesce(summary.status(), "online"),
            safeList(summary.tags())
        );

        HomePageData home = buildHomePage(scenicId, scenic, homeConfig, features, spots);
        ExplorePageData explore = buildExplorePage(scenicId, scenic, exploreConfig, features, spots);
        RewardsPageData rewards = buildRewardsPage(scenicId, scenic, rewardsConfig, features);
        MissionsPageData missions = buildMissionsPage(scenicId, scenic, missionsConfig, rewards, features);
        CloudTourData cloud = buildCloudTourPage(scenicId, scenic, cloudConfig, features);
        Live2dConfig live2d = buildLive2dConfig(features, live2dConfigFile);

        return new ScenicBundle(scenic, home, explore, missions, rewards, cloud, live2d, spots);
    }

    private HomePageData buildHomePage(String scenicId, ScenicDetailResponse scenic, HomeConfigFile homeConfig, ScenicFeatures features, List<SpotDetail> spots) {
        HomeHero hero = homeConfig == null ? null : homeConfig.hero();
        String subtitle = hero == null ? scenic.subtitle() : coalesce(hero.subtitle(), scenic.subtitle());
        String description = hero == null ? scenic.description() : coalesce(hero.description(), scenic.description());
        String heroImage = hero == null ? scenic.coverImage() : coalesce(hero.image(), scenic.coverImage());
        List<String> stats = hero == null ? List.of() : safeList(hero.stats());

        List<EntryCard> entries = safeList(homeConfig == null ? null : homeConfig.entries()).stream()
            .filter(Objects::nonNull)
            .filter(item -> features.enabled(item.feature()))
            .map(item -> new EntryCard(
                coalesce(item.key(), ""),
                coalesce(item.icon(), ""),
                coalesce(item.title(), "入口"),
                coalesce(item.description(), ""),
                buildTargetHref(scenicId, item.target())
            ))
            .toList();

        if (entries.isEmpty()) {
            entries = List.of(
                new EntryCard("explore", "map", "景区探索", "进入地图探索", "/scenic/" + urlEncode(scenicId) + "/explore"),
                new EntryCard("cloud-tour", "cloud", "沉浸云游", "浏览全景视角", "/scenic/" + urlEncode(scenicId) + "/cloud-tour")
            );
        }

        List<HighlightCard> highlights = safeList(homeConfig == null ? null : homeConfig.highlights());
        if (highlights.isEmpty()) {
            highlights = spots.stream()
                .limit(4)
                .map(spot -> new HighlightCard(spot.name(), "精选", coalesce(spot.summary(), ""), spot.id(), coalesce(spot.image(), scenic.coverImage())))
                .toList();
        }

        List<InfoCard> infoCards = safeList(homeConfig == null ? null : homeConfig.infoCards());
        if (infoCards.isEmpty()) {
            infoCards = List.of(
                new InfoCard("建议时长", "1-2 天", "可按体力与兴趣自由拆分行程"),
                new InfoCard("推荐方式", "慢游 + 观景", "建议分段停留，避免一次性赶点"),
                new InfoCard("数字导览", "地图 + 助手", "可在探索页一键应用推荐路线")
            );
        }

        return new HomePageData(
            scenicId,
            scenic.name(),
            subtitle,
            description,
            heroImage,
            stats,
            entries,
            highlights,
            infoCards
        );
    }

    private ExplorePageData buildExplorePage(String scenicId, ScenicDetailResponse scenic, ExploreConfigFile exploreConfig, ScenicFeatures features, List<SpotDetail> spots) {
        String title = coalesce(exploreConfig == null ? null : exploreConfig.title(), "景区探索");
        String subtitle = coalesce(exploreConfig == null ? null : exploreConfig.subtitle(), scenic.subtitle());
        MapView map = normalizeMap(exploreConfig == null ? null : exploreConfig.map());
        List<String> quickSearches = safeList(exploreConfig == null ? null : exploreConfig.quickSearches());
        if (quickSearches.isEmpty()) {
            quickSearches = List.of("热门", "拍照", "亲子", "生态");
        }
        return new ExplorePageData(scenicId, scenic.name(), title, subtitle, map, spots, quickSearches, features);
    }

    private MissionsPageData buildMissionsPage(
        String scenicId,
        ScenicDetailResponse scenic,
        MissionsConfigFile missionsConfig,
        RewardsPageData rewardsPage,
        ScenicFeatures features
    ) {
        EmptyStateData defaultEmptyState = new EmptyStateData("暂未开放任务", "当前景区尚未配置探索任务。");
        EmptyStateData emptyState = missionsConfig == null || missionsConfig.emptyState() == null
            ? defaultEmptyState
            : missionsConfig.emptyState();
        boolean enabled = features.mission();

        Map<String, RewardData> rewardCatalog = rewardsPage.rewards().stream()
            .collect(Collectors.toMap(RewardData::id, item -> item, (a, b) -> a, LinkedHashMap::new));

        List<MissionData> missions = safeList(missionsConfig == null ? null : missionsConfig.missions()).stream()
            .filter(Objects::nonNull)
            .map(mission -> normalizeMission(mission, rewardCatalog))
            .toList();

        return new MissionsPageData(
            scenicId,
            scenic.name(),
            coalesce(missionsConfig == null ? null : missionsConfig.title(), "探索任务"),
            coalesce(missionsConfig == null ? null : missionsConfig.subtitle(), "完成任务解锁图鉴奖励"),
            enabled,
            emptyState,
            missions
        );
    }

    private MissionData normalizeMission(MissionConfigFile mission, Map<String, RewardData> rewardCatalog) {
        String missionId = coalesce(mission.id(), "mission-" + Math.abs(Objects.hash(mission.title(), mission.description())));
        List<MissionStepData> steps = safeList(mission.steps()).stream()
            .filter(Objects::nonNull)
            .map(item -> new MissionStepData(
                coalesce(item.id(), missionId + "-step"),
                coalesce(item.type(), "visitSpot"),
                coalesce(item.spotId(), ""),
                coalesce(item.title(), "任务步骤"),
                coalesce(item.description(), "")
            ))
            .toList();

        List<MissionRewardData> rewards = safeList(mission.rewards()).stream()
            .filter(Objects::nonNull)
            .map(item -> normalizeMissionReward(item, rewardCatalog))
            .toList();

        return new MissionData(
            missionId,
            coalesce(mission.title(), "任务"),
            coalesce(mission.description(), ""),
            coalesce(mission.type(), "side"),
            mission.enabled() == null || mission.enabled(),
            steps,
            rewards
        );
    }

    private MissionRewardData normalizeMissionReward(MissionRewardConfigFile rewardRef, Map<String, RewardData> rewardCatalog) {
        String rewardId = coalesce(rewardRef.rewardId(), "reward-" + Math.abs(Objects.hash(rewardRef.title(), rewardRef.description())));
        RewardData reward = rewardCatalog.get(rewardId);
        return new MissionRewardData(
            coalesce(rewardRef.type(), reward == null ? "reward" : reward.type()),
            rewardId,
            coalesce(rewardRef.title(), reward == null ? "任务奖励" : reward.title()),
            coalesce(rewardRef.description(), reward == null ? "完成任务后可领取该奖励。" : reward.description()),
            coalesce(rewardRef.icon(), reward == null ? "gift" : reward.icon()),
            reward == null ? null : reward.rarity()
        );
    }

    private RewardsPageData buildRewardsPage(String scenicId, ScenicDetailResponse scenic, RewardsConfigFile rewardsConfig, ScenicFeatures features) {
        EmptyStateData defaultEmptyState = new EmptyStateData("暂未开放奖励图鉴", "当前景区尚未配置奖励内容。");
        EmptyStateData emptyState = rewardsConfig == null || rewardsConfig.emptyState() == null
            ? defaultEmptyState
            : rewardsConfig.emptyState();
        boolean enabled = features.collection();

        List<RewardData> rewards = safeList(rewardsConfig == null ? null : rewardsConfig.rewards()).stream()
            .filter(Objects::nonNull)
            .map(item -> new RewardData(
                coalesce(item.id(), "reward-" + Math.abs(Objects.hash(item.title(), item.description()))),
                coalesce(item.type(), "reward"),
                coalesce(item.title(), "奖励"),
                coalesce(item.description(), ""),
                coalesce(item.icon(), "gift"),
                coalesce(item.rarity(), "common")
            ))
            .toList();

        return new RewardsPageData(
            scenicId,
            scenic.name(),
            coalesce(rewardsConfig == null ? null : rewardsConfig.title(), "奖励图鉴"),
            coalesce(rewardsConfig == null ? null : rewardsConfig.subtitle(), "完成探索后逐步解锁奖励"),
            enabled,
            emptyState,
            rewards
        );
    }

    private CloudTourData buildCloudTourPage(String scenicId, ScenicDetailResponse scenic, CloudTourConfigFile cloudConfig, ScenicFeatures features) {
        EmptyStateData defaultEmptyState = new EmptyStateData("暂未开放云游", "当前景区尚未配置可用的云游资源。");
        EmptyStateData emptyState = cloudConfig == null || cloudConfig.emptyState() == null
            ? defaultEmptyState
            : cloudConfig.emptyState();
        boolean enabled = features.cloudTour() && cloudConfig != null && cloudConfig.viewerUrl() != null && !cloudConfig.viewerUrl().isBlank();

        List<CloudScene> scenes = enabled
            ? safeList(cloudConfig.scenes()).stream()
                .filter(Objects::nonNull)
                .map(item -> new CloudScene(
                    coalesce(item.title(), "场景"),
                    coalesce(item.summary(), ""),
                    coalesce(item.image(), scenic.coverImage()),
                    buildTargetHref(scenicId, item.target())
                ))
                .toList()
            : List.of();

        return new CloudTourData(
            scenicId,
            scenic.name(),
            coalesce(cloudConfig == null ? null : cloudConfig.title(), "沉浸云游"),
            coalesce(cloudConfig == null ? null : cloudConfig.subtitle(), scenic.subtitle()),
            enabled ? cloudConfig.viewerUrl() : "",
            scenes,
            enabled,
            emptyState
        );
    }

    private Live2dConfig buildLive2dConfig(ScenicFeatures features, Live2dConfigFile file) {
        boolean enabled = features.live2d();
        return new Live2dConfig(
            enabled,
            coalesce(file == null ? null : file.widgetName(), defaultWidgetName),
            coalesce(file == null ? null : file.greeting(), defaultGreeting),
            coalesce(file == null ? null : file.modelUrl(), defaultModelUrl),
            file == null || file.width() == null || file.width() <= 0 ? 240 : file.width(),
            file == null || file.height() == null || file.height() <= 0 ? 320 : file.height(),
            file == null || file.scale() == null || file.scale() <= 0 ? 0.18 : file.scale(),
            safeList(file == null ? null : file.promptHints())
        );
    }

    private String resolveDataScenicId(String scenicId) {
        if (resourceExists("classpath:scenics/" + scenicId + "/scenic.json")) {
            return scenicId;
        }
        return scenicRegistryService.getDefaultScenicId();
    }

    private boolean resourceExists(String location) {
        return resourceLoader.getResource(location).exists();
    }

    private <T> T readRequired(String scenicId, String filename, Class<T> type) {
        String location = "classpath:scenics/" + scenicId + "/" + filename;
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing scenic config: " + location);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, type);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read scenic config: " + location, ex);
        }
    }

    private <T> Optional<T> readOptional(String scenicId, String filename, Class<T> type) {
        String location = "classpath:scenics/" + scenicId + "/" + filename;
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream input = resource.getInputStream()) {
            return Optional.of(objectMapper.readValue(input, type));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private MapView normalizeMap(MapView raw) {
        if (raw == null) {
            return new MapView(103.9272, 33.2306, 13.8, "amap://styles/fresh");
        }
        double zoom = raw.zoom() > 0 ? raw.zoom() : 13.8;
        return new MapView(raw.lng(), raw.lat(), zoom, raw.style());
    }

    private String buildTargetHref(String scenicId, RouteTarget target) {
        String base = "/scenic/" + urlEncode(scenicId);
        if (target == null || target.type() == null || target.type().isBlank()) {
            return base;
        }

        String path = switch (normalize(target.type())) {
            case "explore" -> base + "/explore";
            case "cloud", "cloudtour", "cloud-tour" -> base + "/cloud-tour";
            default -> base;
        };

        List<String> queries = new ArrayList<>();
        if (target.spotId() != null && !target.spotId().isBlank()) {
            queries.add("spot=" + urlEncode(target.spotId()));
        }
        if (target.panel() != null && !target.panel().isBlank()) {
            queries.add("panel=" + urlEncode(target.panel()));
        }
        if (target.missionId() != null && !target.missionId().isBlank()) {
            queries.add("missionId=" + urlEncode(target.missionId()));
        }
        if (target.rewardId() != null && !target.rewardId().isBlank()) {
            queries.add("rewardId=" + urlEncode(target.rewardId()));
        }

        return queries.isEmpty() ? path : (path + "?" + String.join("&", queries));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(coalesce(value, ""), StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String coalesce(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}

@Service
class Live2dChatService {
    private static final Logger log = LoggerFactory.getLogger(Live2dChatService.class);
    private final ScenicGuideService scenicGuideService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    @Value("${scenic.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${scenic.ai.base-url:}")
    private String aiBaseUrl;

    @Value("${scenic.ai.api-key:}")
    private String apiKey;

    @Value("${scenic.ai.model:}")
    private String model;

    @Value("${scenic.ai.system-prompt:You are a scenic guide assistant.}")
    private String systemPrompt;

    Live2dChatService(ScenicGuideService scenicGuideService, ObjectMapper objectMapper) {
        this.scenicGuideService = scenicGuideService;
        this.objectMapper = objectMapper;
    }

    Live2dChatResponse chat(Live2DChatRequest request) {
        String scenicId = request.scenicId();
        String message = request.message() == null ? "" : request.message().trim();
        List<SearchResult> related = scenicGuideService.search(scenicId, message).stream().limit(4).toList();
        if (!aiEnabled || aiBaseUrl == null || aiBaseUrl.isBlank() || model == null || model.isBlank()) {
            return new Live2dChatResponse(scenicGuideService.buildGuideReply(scenicId, message), false, "local-guide", related, null);
        }

        try {
            ParsedAssistantReply parsed = parseAssistantReply(callAi(request), scenicId);
            String reply = parsed.reply();
            if (reply == null || reply.isBlank()) {
                reply = scenicGuideService.buildGuideReply(scenicId, message);
            }
            RoutePlanResponse routePlan = parsed.routePlan();
            if (routePlan == null && shouldAttachRoutePlan(message, reply)) {
                routePlan = buildSuggestedRoutePlan(scenicId, message, reply);
            }
            return new Live2dChatResponse(reply, true, "server-proxy", related, routePlan);
        } catch (Exception ex) {
            log.warn("Live2D AI request failed for scenicId={} providerUrl={} message={}", scenicId, aiBaseUrl, abbreviateForLog(message), ex);
            return new Live2dChatResponse(scenicGuideService.buildGuideReply(scenicId, message), false, "fallback", related, null);
        }
    }

    private boolean shouldAttachRoutePlan(String message, String reply) {
        String text = (safeText(message) + " " + safeText(reply)).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return false;
        }
        return List.of(
            "路线", "行程", "怎么走", "游玩顺序", "推荐一条", "推荐个路线", "路线规划",
            "route", "plan", "itinerary"
        ).stream().anyMatch(text::contains);
    }

    private RoutePlanResponse buildSuggestedRoutePlan(String scenicId, String message, String reply) {
        RouteRequest routeRequest = new RouteRequest(
            inferDuration(message, reply),
            inferPace(message, reply),
            inferGroup(message, reply),
            inferInterests(message, reply)
        );
        return scenicGuideService.buildPlan(scenicId, routeRequest);
    }

    private String inferDuration(String message, String reply) {
        String text = (safeText(message) + " " + safeText(reply)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "全天", "一天", "一日", "full day", "full-day")) return "full";
        if (containsAny(text, "半天深度", "深度")) return "half";
        if (containsAny(text, "半天", "半日", "medium")) return "medium";
        if (containsAny(text, "短时", "短线", "一两个小时", "两三个小时", "quick", "short")) return "short";
        return "medium";
    }

    private String inferPace(String message, String reply) {
        String text = (safeText(message) + " " + safeText(reply)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "拍照", "摄影", "出片", "photo")) return "photo";
        if (containsAny(text, "讲解", "科普", "生态", "学习", "learn")) return "learn";
        if (containsAny(text, "慢游", "轻松", "休闲", "不赶", "relax")) return "relax";
        return "relax";
    }

    private String inferGroup(String message, String reply) {
        String text = (safeText(message) + " " + safeText(reply)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "亲子", "孩子", "小朋友", "family")) return "family";
        if (containsAny(text, "情侣", "两人", "约会", "pair", "couple")) return "pair";
        return "family";
    }

    private List<String> inferInterests(String message, String reply) {
        String text = (safeText(message) + " " + safeText(reply)).toLowerCase(Locale.ROOT);
        List<String> interests = new ArrayList<>();
        if (containsAny(text, "拍照", "摄影", "倒影")) interests.add("拍照");
        if (containsAny(text, "瀑布")) interests.add("瀑布");
        if (containsAny(text, "湖", "海子", "倒影")) interests.add("湖泊");
        if (containsAny(text, "生态", "动物", "植物")) interests.add("生态");
        if (containsAny(text, "轻松", "慢游", "休闲")) interests.add("慢游");
        return interests.stream().distinct().toList();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && text.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String abbreviateForLog(String value) {
        String text = safeText(value).replaceAll("\\s+", " ");
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }

    private ParsedAssistantReply parseAssistantReply(String rawReply, String scenicId) {
        String content = rawReply == null ? "" : rawReply.trim();
        if (content.isBlank()) {
            return new ParsedAssistantReply("", null);
        }

        JsonNode payload = tryParseAssistantPayload(content);
        if (payload != null && payload.isObject()) {
            JsonNode dataNode = payload.path("data");
            String reply = sanitizeAiReply(firstNonBlank(
                extractTextNode(payload.path("reply")),
                extractTextNode(payload.path("message")),
                extractTextNode(payload.path("text")),
                extractTextNode(payload.path("content")),
                extractTextNode(dataNode.path("reply")),
                extractTextNode(dataNode.path("message")),
                extractTextNode(dataNode.path("text")),
                extractTextNode(dataNode.path("content"))
            ));
            JsonNode routePlanNode = payload.path("routePlan");
            if ((routePlanNode == null || routePlanNode.isMissingNode() || routePlanNode.isNull()) && dataNode.isObject()) {
                routePlanNode = dataNode.path("routePlan");
            }
            RoutePlanResponse routePlan = parseRoutePlanFromPayload(routePlanNode, scenicId);
            return new ParsedAssistantReply(reply, routePlan);
        }

        return new ParsedAssistantReply(sanitizeAiReply(content), null);
    }

    private JsonNode tryParseAssistantPayload(String content) {
        JsonNode direct = tryReadJson(content);
        if (direct != null && direct.isObject()) {
            return direct;
        }

        String candidate = extractJsonObjectCandidate(content);
        if (candidate.isBlank()) {
            return null;
        }
        JsonNode extracted = tryReadJson(candidate);
        return extracted != null && extracted.isObject() ? extracted : null;
    }

    private JsonNode tryReadJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractTextNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText("").trim();
        }
        if (node.isArray()) {
            List<String> chunks = new ArrayList<>();
            for (JsonNode item : node) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String chunk = firstNonBlank(
                    extractTextNode(item.path("text")),
                    extractTextNode(item.path("content")),
                    extractTextNode(item.path("value")),
                    item.isContainerNode() ? "" : extractTextNode(item)
                );
                if (!chunk.isBlank()) {
                    chunks.add(chunk);
                }
            }
            return String.join("\n", chunks).trim();
        }
        if (node.isObject()) {
            return firstNonBlank(
                extractTextNode(node.path("text")),
                extractTextNode(node.path("content")),
                extractTextNode(node.path("value")),
                extractTextNode(node.path("message")),
                extractTextNode(node.path("reply"))
            );
        }
        return "";
    }

    private String extractJsonObjectCandidate(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            int firstBreak = normalized.indexOf('\n');
            int lastFence = normalized.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                normalized = normalized.substring(firstBreak + 1, lastFence).trim();
            }
        }
        int firstBrace = normalized.indexOf('{');
        int lastBrace = normalized.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            return "";
        }
        return normalized.substring(firstBrace, lastBrace + 1).trim();
    }

    private RoutePlanResponse parseRoutePlanFromPayload(JsonNode routePlanNode, String scenicId) {
        if (routePlanNode != null && routePlanNode.isTextual()) {
            JsonNode parsed = tryReadJson(routePlanNode.asText(""));
            if (parsed != null) {
                routePlanNode = parsed;
            }
        }
        if (routePlanNode == null || !routePlanNode.isObject()) {
            return null;
        }

        List<SpotDetail> scenicSpots = scenicGuideService.getSpots(scenicId);
        if (scenicSpots == null || scenicSpots.isEmpty()) {
            return null;
        }

        Map<String, SpotDetail> spotById = new LinkedHashMap<>();
        Map<String, SpotDetail> spotByName = new LinkedHashMap<>();
        scenicSpots.forEach((spot) -> {
            if (spot == null) return;
            String idKey = normalizeSpotToken(spot.id());
            String nameKey = normalizeSpotToken(spot.name());
            if (!idKey.isBlank()) {
                spotById.putIfAbsent(idKey, spot);
            }
            if (!nameKey.isBlank()) {
                spotByName.putIfAbsent(nameKey, spot);
            }
        });

        LinkedHashMap<String, RouteStop> resolvedStops = new LinkedHashMap<>();
        JsonNode stopsNode = routePlanNode.path("stops");
        if (stopsNode.isArray()) {
            for (JsonNode stopNode : stopsNode) {
                String candidateId;
                String candidateName;
                if (stopNode != null && stopNode.isTextual()) {
                    candidateId = stopNode.asText("");
                    candidateName = stopNode.asText("");
                } else {
                    candidateId = firstNonBlank(
                        stopNode.path("id").asText(""),
                        stopNode.path("spotId").asText("")
                    );
                    candidateName = stopNode.path("name").asText("");
                }
                SpotDetail matched = resolveRouteSpot(spotById, spotByName, candidateId, candidateName);
                if (matched == null) {
                    continue;
                }
                resolvedStops.putIfAbsent(
                    matched.id(),
                    new RouteStop(matched.id(), matched.name(), matched.kind(), matched.lng(), matched.lat())
                );
            }
        }

        if (resolvedStops.isEmpty()) {
            return null;
        }

        String title = firstNonBlank(routePlanNode.path("title").asText(""), "Assistant Suggested Route");
        String description = firstNonBlank(routePlanNode.path("description").asText(""), "Generated based on your request.");
        List<String> reasons = extractStringArray(routePlanNode.path("reasons"), 6);
        List<String> tags = extractStringArray(routePlanNode.path("tags"), 8);
        List<RouteStop> stops = new ArrayList<>(resolvedStops.values());
        return new RoutePlanResponse(title, description, reasons, tags, stops);
    }

    private SpotDetail resolveRouteSpot(
        Map<String, SpotDetail> spotById,
        Map<String, SpotDetail> spotByName,
        String candidateId,
        String candidateName
    ) {
        String idKey = normalizeSpotToken(candidateId);
        if (!idKey.isBlank()) {
            SpotDetail byId = spotById.get(idKey);
            if (byId != null) {
                return byId;
            }
        }
        String nameKey = normalizeSpotToken(candidateName);
        if (!nameKey.isBlank()) {
            return spotByName.get(nameKey);
        }
        return null;
    }

    private String normalizeSpotToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private List<String> extractStringArray(JsonNode node, int limit) {
        if (node == null || !node.isArray() || limit <= 0) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String text = item == null ? "" : item.asText("");
            if (text == null || text.isBlank()) {
                continue;
            }
            values.add(text.trim());
            if (values.size() >= limit) {
                break;
            }
        }
        return values;
    }

    private String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String callAi(Live2DChatRequest request) throws IOException, InterruptedException {
        try {
            return callAiOnce(request, true);
        } catch (IOException firstError) {
            return callAiOnce(request, false);
        }
    }

    private String callAiOnce(Live2DChatRequest request, boolean forceJsonObject) throws IOException, InterruptedException {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPromptV2(request.scenicId(), request.page())));

        if (request.history() != null) {
            request.history().stream()
                .filter(item -> item != null && item.content() != null && !item.content().isBlank())
                .limit(8)
                .forEach(item -> {
                    String historyText = item.content() == null ? "" : item.content().trim();
                    if (historyText.isBlank()) {
                        return;
                    }
                    messages.add(Map.of(
                        "role", sanitizeRole(item.role()),
                        "content", historyText
                    ));
                });
        }

        String userMessage = request.message() == null ? "" : request.message().trim();
        if (userMessage.isBlank()) {
            userMessage = "Please introduce the current scenic area.";
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        if (forceJsonObject) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(aiBaseUrl))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
            .build();
        if (apiKey != null && !apiKey.isBlank()) {
            httpRequest = HttpRequest.newBuilder(httpRequest.uri())
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        }

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body() == null ? "" : response.body().replaceAll("\\s+", " ").trim();
            if (body.length() > 240) {
                body = body.substring(0, 240) + "...";
            }
            throw new IOException("AI service response error: " + response.statusCode() + (body.isBlank() ? "" : (" body=" + body)));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractAiContent(root);
        if (content == null || content.isBlank()) {
            content = root.path("output").path("text").asText("");
        }
        return content;
    }

    private String extractAiContent(JsonNode root) {
        if (root == null || root.isNull()) {
            return "";
        }

        String directContent = firstNonBlank(
            extractTextNode(root.path("content")),
            extractTextNode(root.path("output_text")),
            extractTextNode(root.path("text"))
        );
        if (!directContent.isBlank()) {
            return directContent;
        }

        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            String content = firstNonBlank(
                extractTextNode(first.path("message").path("content")),
                extractTextNode(first.path("delta").path("content")),
                extractTextNode(first.path("text"))
            );
            if (!content.isBlank()) {
                return content;
            }
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                String content = firstNonBlank(
                    extractTextNode(item.path("content")),
                    extractTextNode(item.path("text"))
                );
                if (!content.isBlank()) {
                    return content;
                }
            }
        } else if (output.isObject()) {
            String content = firstNonBlank(
                extractTextNode(output.path("content")),
                extractTextNode(output.path("text"))
            );
            if (!content.isBlank()) {
                return content;
            }
        }

        JsonNode data = root.path("data");
        if (data.isObject()) {
            String content = firstNonBlank(
                extractTextNode(data.path("reply")),
                extractTextNode(data.path("message")),
                extractTextNode(data.path("content")),
                extractTextNode(data.path("text"))
            );
            if (!content.isBlank()) {
                return content;
            }
        }

        return "";
    }

    private String buildSystemPromptV2(String scenicId, String page) {
        String scenicName = scenicGuideService.getScenicName(scenicId);
        String scene = switch (page == null ? "" : page.trim().toLowerCase(Locale.ROOT)) {
            case "home" -> "home page";
            case "explore" -> "explore map";
            case "cloud" -> "cloud tour page";
            default -> "scenic page";
        };
        String knownSpots = scenicGuideService.getSpots(scenicId).stream()
            .sorted(Comparator.comparingInt(SpotDetail::order))
            .limit(12)
            .map(SpotDetail::name)
            .filter(Objects::nonNull)
            .filter(name -> !name.isBlank())
            .collect(Collectors.joining(", "));

        StringBuilder builder = new StringBuilder();
        builder.append("You are Tuanzi, a scenic guide assistant. Keep responses friendly, concise, and practical.");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.append(' ').append(systemPrompt.trim());
        }
        builder.append(" Current scenic area: ").append(scenicName).append('.');
        builder.append(" Current page: ").append(scene).append('.');
        if (!knownSpots.isBlank()) {
            builder.append(" Known spots: ").append(knownSpots).append('.');
        }
        builder.append(" Output must be a JSON object only. Do not output Markdown.");
        builder.append(" The field \"reply\" is required and should be natural Chinese text.");
        builder.append(" Return JSON only with shape: {\"reply\":\"...\",\"routePlan\":null or {\"title\":\"...\",\"description\":\"...\",\"reasons\":[\"...\"],\"tags\":[\"...\"],\"stops\":[{\"spotId\":\"...\" or \"id\":\"...\" or \"name\":\"...\"}]}}.");
        builder.append(" If route is not requested, set routePlan to null.");
        builder.append(" Never invent scenic spots beyond the known spot list.");
        return builder.toString();
    }

    private String sanitizeAiReply(String reply) {
        if (reply == null) {
            return "";
        }

        String normalized = reply.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        List<String> cleaned = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.matches("^[-*_]{3,}$")) {
                continue;
            }

            line = line.replaceAll("^#{1,6}\\s*", "");
            line = line.replaceAll("^>\\s*", "");
            line = line.replaceAll("^[-*+]\\s+", "");
            line = line.replaceAll("^\\d+[.)]\\s*", "");
            line = line.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "$1");
            line = line
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("#", "");

            line = line.trim();
            if (!line.isBlank()) {
                cleaned.add(line);
            }
        }

        String merged = String.join("\n", cleaned);
        return merged.replaceAll("\n{3,}", "\n\n").trim();
    }

    private record ParsedAssistantReply(String reply, RoutePlanResponse routePlan) {}

    private String sanitizeRole(String role) {
        String value = role == null ? "user" : role.trim().toLowerCase(Locale.ROOT);
        return List.of("system", "assistant", "user").contains(value) ? value : "user";
    }
}

record ScenicBundle(
    ScenicDetailResponse scenic,
    HomePageData home,
    ExplorePageData explore,
    MissionsPageData missions,
    RewardsPageData rewards,
    CloudTourData cloud,
    Live2dConfig live2d,
    List<SpotDetail> spots
) {}
