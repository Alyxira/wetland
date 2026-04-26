package com.example.demo.scenic;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
class ScenicApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + "：" + error.getDefaultMessage())
            .orElse("请求参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse<Object>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "请求失败" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Object>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("服务暂时不可用，请稍后重试"));
    }
}

record ApiResponse<T>(boolean success, T data, String message) {
    static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "ok");
    }

    static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}

record ScenicIndexData(int version, String defaultScenicId, List<ScenicSummary> scenics) {}

record ScenicSummary(
    String id,
    String name,
    String subtitle,
    String coverImage,
    String status,
    List<String> tags,
    ScenicFeatures features
) {}

record ScenicDetailResponse(
    String id,
    String name,
    String subtitle,
    String description,
    ScenicTheme theme,
    ScenicFeatures features,
    ScenicNavigation navigation,
    ScenicSeo seo,
    String coverImage,
    String status,
    List<String> tags
) {}

record HomePageData(
    String scenicId,
    String scenicName,
    String subtitle,
    String description,
    String heroImage,
    List<String> stats,
    List<EntryCard> entries,
    List<HighlightCard> highlights,
    List<InfoCard> infoCards
) {}

record EntryCard(String key, String icon, String title, String description, String href) {}

record HighlightCard(String title, String tag, String summary, String spotId, String image) {}

record InfoCard(String label, String title, String description) {}

record ExplorePageData(
    String scenicId,
    String scenicName,
    String title,
    String subtitle,
    MapView map,
    List<SpotDetail> spots,
    List<String> quickSearches,
    ScenicFeatures features
) {}

record MapView(double lng, double lat, double zoom, String style) {}

record SpotDetail(
    String id,
    int order,
    String name,
    String shortName,
    String kind,
    String emoji,
    double lng,
    double lat,
    int radius,
    List<String> tags,
    String summary,
    String discoveryTitle,
    String discoveryText,
    String image
) {}

record SearchResult(
    String id,
    String name,
    String kind,
    String summary,
    List<String> tags,
    double lng,
    double lat,
    String image,
    String reason
) {}

record RouteRequest(
    @NotBlank(message = "请选择游玩时长") String duration,
    @NotBlank(message = "请选择游览节奏") String pace,
    @NotBlank(message = "请选择同行方式") String group,
    List<String> interests
) {}

record RoutePlanResponse(
    String title,
    String description,
    List<String> reasons,
    List<String> tags,
    List<RouteStop> stops
) {}

record RouteStop(String id, String name, String kind, double lng, double lat) {}

record WetlandMapSpot(
    int wetlandId,
    String id,
    String name,
    String pinyin,
    double lat,
    double lng,
    String type,
    String description,
    String imageHint,
    String imagePath,
    String coordinateRange,
    String tags
) {}

record WetlandMapPayload(
    String sourceFile,
    int totalRows,
    int parsedRows,
    int skippedRows,
    List<WetlandMapSpot> spots
) {}

record EmptyStateData(String title, String description) {}

record MissionsPageData(
    String scenicId,
    String scenicName,
    String title,
    String subtitle,
    boolean enabled,
    EmptyStateData emptyState,
    List<MissionData> missions
) {}

record MissionData(
    String id,
    String title,
    String description,
    String type,
    boolean enabled,
    List<MissionStepData> steps,
    List<MissionRewardData> rewards
) {}

record MissionStepData(String id, String type, String spotId, String title, String description) {}

record MissionRewardData(String type, String rewardId, String title, String description, String icon, String rarity) {}

record RewardsPageData(
    String scenicId,
    String scenicName,
    String title,
    String subtitle,
    boolean enabled,
    EmptyStateData emptyState,
    List<RewardData> rewards
) {}

record RewardData(String id, String type, String title, String description, String icon, String rarity) {}

record CloudTourData(
    String scenicId,
    String scenicName,
    String title,
    String subtitle,
    String viewerUrl,
    List<CloudScene> scenes,
    boolean enabled,
    EmptyStateData emptyState
) {}

record CloudScene(String title, String summary, String image, String href) {}

record Live2dConfig(
    boolean enabled,
    String widgetName,
    String greeting,
    String modelUrl,
    int width,
    int height,
    double scale,
    List<String> promptHints
) {}

record Live2DChatRequest(
    @NotBlank(message = "请输入消息内容") String message,
    String scenicId,
    String page,
    List<ChatMessage> history
) {}

record ChatMessage(String role, String content) {}

record Live2dChatResponse(
    String reply,
    boolean configured,
    String provider,
    List<SearchResult> relatedSpots,
    RoutePlanResponse routePlan
) {}

record SpotLocationView(double lat, double lng, String address) {}

record ExploreSpotView(
    String id,
    String name,
    String description,
    String longDescription,
    String category,
    List<String> tags,
    String imageUrl,
    List<String> images,
    double rating,
    int visitCount,
    String openHours,
    String ticketPrice,
    SpotLocationView location,
    String virtualTourUrl,
    boolean isVisited,
    boolean isFeatured,
    String createdAt
) {}

record CategoryCount(String name, int count) {}

record SpotsSummaryView(
    int total,
    int visited,
    List<CategoryCount> categories,
    List<ExploreSpotView> topRated
) {}

record ScenicRouteView(
    String id,
    String name,
    String description,
    String duration,
    String difficulty,
    List<String> spotIds,
    List<ExploreSpotView> spots,
    String distance,
    boolean isRecommended
) {}

record ScenicTaskView(
    String id,
    String title,
    String description,
    String type,
    String reward,
    int rewardXp,
    boolean isCompleted,
    String spotId
) {}

record ScenicStampView(
    String id,
    String spotId,
    String spotName,
    String name,
    String description,
    String imageUrl,
    String rarity,
    boolean isCollected,
    String collectedAt
) {}

record ScenicEventView(
    String id,
    String spotId,
    String spotName,
    String type,
    String title,
    String content,
    List<String> options,
    double triggerRadius,
    String reward,
    int rewardXp,
    boolean isCompleted
) {}

record ScenicEventInteractRequest(String answer) {}

record ScenicEventInteractResponse(
    boolean success,
    boolean correct,
    String message,
    int rewardXp,
    ScenicEventView event
) {}

record ScenicGuideChatPayload(List<ChatMessage> messages, String currentSpotId) {}

record ScenicGuideChatRequest(ScenicGuideChatPayload data) {}

record ScenicGuideChatResponse(
    String reply,
    List<String> suggestedSpotIds,
    String suggestedRouteId
) {}

record SearchScore(SpotDetail spot, int score) {}

record ScoredSpot(SpotDetail spot, int score) {}

record ScenicConfigFile(
    int version,
    String id,
    String name,
    String subtitle,
    String description,
    ScenicTheme theme,
    ScenicFeatures features,
    ScenicNavigation navigation,
    ScenicSeo seo
) {}

record ScenicTheme(String brand, String accent, String background, String surfaceMode) {}

record ScenicFeatures(
    boolean home,
    boolean explore,
    boolean cloudTour,
    boolean live2d,
    boolean routeCustomize,
    boolean collection,
    boolean mission,
    boolean adventureMode
) {
    static ScenicFeatures defaults() {
        return new ScenicFeatures(true, true, true, true, true, true, false, false);
    }

    boolean enabled(String featureKey) {
        String key = featureKey == null ? "" : featureKey.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "", "default", "always" -> true;
            case "home" -> home;
            case "explore" -> explore;
            case "cloudtour", "cloud-tour", "cloud" -> cloudTour;
            case "live2d" -> live2d;
            case "routecustomize", "route-customize", "route" -> routeCustomize;
            case "collection" -> collection;
            case "mission" -> mission;
            case "reward", "rewards" -> mission;
            case "adventuremode", "adventure-mode", "adventure" -> adventureMode;
            default -> true;
        };
    }
}

record ScenicNavigation(Boolean showOnIndex, Integer sortOrder) {}

record ScenicSeo(String title, List<String> keywords) {}

record HomeConfigFile(
    int version,
    HomeHero hero,
    List<HomeEntryConfig> entries,
    List<HighlightCard> highlights,
    List<InfoCard> infoCards
) {}

record HomeHero(String title, String subtitle, String description, String image, List<String> stats) {}

record HomeEntryConfig(String key, String icon, String title, String description, RouteTarget target, String feature) {}

record RouteTarget(String type, String spotId, String panel, String missionId, String rewardId) {}

record ExploreConfigFile(
    int version,
    String title,
    String subtitle,
    MapView map,
    List<String> quickSearches,
    PanelState defaultPanel,
    ExploreUiConfig ui
) {}

record PanelState(String left, String right) {}

record ExploreUiConfig(boolean showCollection, boolean showRoutePanel, boolean showMissionPanel, boolean showAdventureModeEntry) {}

record CloudTourConfigFile(
    int version,
    String title,
    String subtitle,
    String viewerUrl,
    List<CloudSceneConfig> scenes,
    EmptyStateData emptyState
) {}

record CloudSceneConfig(String title, String summary, String image, RouteTarget target) {}

record SpotsFile(int version, List<SpotDetail> spots) {}

record Live2dConfigFile(
    int version,
    String widgetName,
    String greeting,
    String modelUrl,
    Integer width,
    Integer height,
    Double scale,
    List<String> promptHints
) {}


record MissionsConfigFile(
    int version,
    String title,
    String subtitle,
    EmptyStateData emptyState,
    List<MissionConfigFile> missions
) {}

record MissionConfigFile(
    String id,
    String title,
    String description,
    String type,
    Boolean enabled,
    List<MissionStepConfigFile> steps,
    List<MissionRewardConfigFile> rewards
) {}

record MissionStepConfigFile(
    String id,
    String type,
    String spotId,
    String title,
    String description
) {}

record MissionRewardConfigFile(String type, String rewardId, String title, String description, String icon) {}

record RewardsConfigFile(
    int version,
    String title,
    String subtitle,
    EmptyStateData emptyState,
    List<RewardConfigFile> rewards
) {}

record RewardConfigFile(
    String id,
    String type,
    String title,
    String description,
    String icon,
    String rarity
) {}
