package com.example.demo.scenic;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/consult")
class ScenicApiController {

    private final ScenicGuideService scenicGuideService;
    private final Live2dChatService live2dChatService;
    private final ScenicGameService scenicGameService;
    private final WetlandMapService wetlandMapService;

    ScenicApiController(
        ScenicGuideService scenicGuideService,
        Live2dChatService live2dChatService,
        ScenicGameService scenicGameService,
        WetlandMapService wetlandMapService
    ) {
        this.scenicGuideService = scenicGuideService;
        this.live2dChatService = live2dChatService;
        this.scenicGameService = scenicGameService;
        this.wetlandMapService = wetlandMapService;
    }

    @GetMapping("/scenics")
    ApiResponse<ScenicIndexData> getScenics() {
        return ApiResponse.success(scenicGuideService.getScenicIndex());
    }

    @GetMapping("/scenics/{scenicId}")
    ApiResponse<ScenicDetailResponse> getScenic(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getScenic(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/site/home")
    ApiResponse<HomePageData> getHomePage(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getHomePage(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/site/explore")
    ApiResponse<ExplorePageData> getExplorePage(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getExplorePage(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/missions")
    ApiResponse<MissionsPageData> getMissions(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getMissionsPage(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/rewards")
    ApiResponse<RewardsPageData> getRewards(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getRewardsPage(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/site/cloud-tour")
    ApiResponse<CloudTourData> getCloudTourPage(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getCloudTourPage(scenicId));
    }

    @GetMapping("/wetlands/spots")
    ApiResponse<WetlandMapPayload> getWetlandMapSpots() {
        return ApiResponse.success(wetlandMapService.getWetlandMapPayload());
    }

    @GetMapping("/scenics/{scenicId}/spots")
    ApiResponse<java.util.List<SpotDetail>> getSpots(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getSpots(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/spots/summary")
    ApiResponse<SpotsSummaryView> getSpotsSummary(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGameService.getSpotsSummary(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/spots/featured")
    ApiResponse<java.util.List<ExploreSpotView>> getFeaturedSpots(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGameService.getFeaturedSpots(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/spots/{spotId}")
    ApiResponse<SpotDetail> getSpot(@PathVariable String scenicId, @PathVariable String spotId) {
        return ApiResponse.success(scenicGuideService.getSpotById(scenicId, spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spot not found")));
    }

    @GetMapping("/scenics/{scenicId}/spots/search")
    ApiResponse<java.util.List<SearchResult>> searchSpots(@PathVariable String scenicId, @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(scenicGuideService.search(scenicId, keyword));
    }

    @PostMapping("/scenics/{scenicId}/routes/customize")
    ApiResponse<RoutePlanResponse> customizeRoute(@PathVariable String scenicId, @Valid @RequestBody RouteRequest request) {
        return ApiResponse.success(scenicGuideService.buildPlan(scenicId, request));
    }

    @GetMapping("/scenics/{scenicId}/routes")
    ApiResponse<java.util.List<ScenicRouteView>> listRoutes(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGameService.listRoutes(scenicId));
    }

    @GetMapping("/scenics/{scenicId}/tasks")
    ApiResponse<java.util.List<ScenicTaskView>> listTasks(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGameService.listTasks(scenicId));
    }

    @PostMapping("/scenics/{scenicId}/tasks/{taskId}/complete")
    ApiResponse<ScenicTaskView> completeTask(@PathVariable String scenicId, @PathVariable String taskId) {
        return ApiResponse.success(scenicGameService.completeTask(scenicId, taskId));
    }

    @GetMapping("/scenics/{scenicId}/stamps")
    ApiResponse<java.util.List<ScenicStampView>> listStamps(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGameService.listStamps(scenicId));
    }

    @PostMapping("/scenics/{scenicId}/stamps/{stampId}/collect")
    ApiResponse<ScenicStampView> collectStamp(@PathVariable String scenicId, @PathVariable String stampId) {
        return ApiResponse.success(scenicGameService.collectStamp(scenicId, stampId));
    }

    @GetMapping("/scenics/{scenicId}/events")
    ApiResponse<java.util.List<ScenicEventView>> listEvents(@PathVariable String scenicId, @RequestParam(defaultValue = "") String spotId) {
        return ApiResponse.success(scenicGameService.listEvents(scenicId, spotId));
    }

    @PostMapping("/scenics/{scenicId}/events/{eventId}/interact")
    ApiResponse<ScenicEventInteractResponse> interactEvent(
        @PathVariable String scenicId,
        @PathVariable String eventId,
        @RequestBody ScenicEventInteractRequest request
    ) {
        return ApiResponse.success(scenicGameService.interactEvent(scenicId, eventId, request));
    }

    @PostMapping("/scenics/{scenicId}/guide/chat")
    ApiResponse<ScenicGuideChatResponse> guideChat(@PathVariable String scenicId, @RequestBody ScenicGuideChatRequest request) {
        return ApiResponse.success(scenicGameService.guideChat(scenicId, request));
    }

    @GetMapping("/scenics/{scenicId}/live2d/config")
    ApiResponse<Live2dConfig> getLive2dConfig(@PathVariable String scenicId) {
        return ApiResponse.success(scenicGuideService.getLive2dConfig(scenicId));
    }

    @PostMapping("/scenics/{scenicId}/live2d/chat")
    ApiResponse<Live2dChatResponse> chat(@PathVariable String scenicId, @Valid @RequestBody Live2DChatRequest request) {
        return ApiResponse.success(live2dChatService.chat(withScenicId(request, scenicId)));
    }

    @GetMapping("/site/home")
    ApiResponse<HomePageData> getDefaultHomePage() {
        return ApiResponse.success(scenicGuideService.getHomePage(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/site/explore")
    ApiResponse<ExplorePageData> getDefaultExplorePage() {
        return ApiResponse.success(scenicGuideService.getExplorePage(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/missions")
    ApiResponse<MissionsPageData> getDefaultMissions() {
        return ApiResponse.success(scenicGuideService.getMissionsPage(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/rewards")
    ApiResponse<RewardsPageData> getDefaultRewards() {
        return ApiResponse.success(scenicGuideService.getRewardsPage(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/site/cloud-tour")
    ApiResponse<CloudTourData> getDefaultCloudTourPage() {
        return ApiResponse.success(scenicGuideService.getCloudTourPage(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/spots")
    ApiResponse<java.util.List<SpotDetail>> getDefaultSpots() {
        return ApiResponse.success(scenicGuideService.getSpots(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/spots/summary")
    ApiResponse<SpotsSummaryView> getDefaultSpotsSummary() {
        return ApiResponse.success(scenicGameService.getSpotsSummary(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/spots/featured")
    ApiResponse<java.util.List<ExploreSpotView>> getDefaultFeaturedSpots() {
        return ApiResponse.success(scenicGameService.getFeaturedSpots(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/spots/{spotId}")
    ApiResponse<SpotDetail> getDefaultSpot(@PathVariable String spotId) {
        String scenicId = scenicGuideService.getDefaultScenicId();
        return ApiResponse.success(scenicGuideService.getSpotById(scenicId, spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spot not found")));
    }

    @GetMapping("/spots/search")
    ApiResponse<java.util.List<SearchResult>> searchDefaultSpots(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(scenicGuideService.search(scenicGuideService.getDefaultScenicId(), keyword));
    }

    @PostMapping("/routes/customize")
    ApiResponse<RoutePlanResponse> customizeDefaultRoute(@Valid @RequestBody RouteRequest request) {
        return ApiResponse.success(scenicGuideService.buildPlan(scenicGuideService.getDefaultScenicId(), request));
    }

    @GetMapping("/routes")
    ApiResponse<java.util.List<ScenicRouteView>> listDefaultRoutes() {
        return ApiResponse.success(scenicGameService.listRoutes(scenicGuideService.getDefaultScenicId()));
    }

    @GetMapping("/tasks")
    ApiResponse<java.util.List<ScenicTaskView>> listDefaultTasks() {
        return ApiResponse.success(scenicGameService.listTasks(scenicGuideService.getDefaultScenicId()));
    }

    @PostMapping("/tasks/{taskId}/complete")
    ApiResponse<ScenicTaskView> completeDefaultTask(@PathVariable String taskId) {
        return ApiResponse.success(scenicGameService.completeTask(scenicGuideService.getDefaultScenicId(), taskId));
    }

    @GetMapping("/stamps")
    ApiResponse<java.util.List<ScenicStampView>> listDefaultStamps() {
        return ApiResponse.success(scenicGameService.listStamps(scenicGuideService.getDefaultScenicId()));
    }

    @PostMapping("/stamps/{stampId}/collect")
    ApiResponse<ScenicStampView> collectDefaultStamp(@PathVariable String stampId) {
        return ApiResponse.success(scenicGameService.collectStamp(scenicGuideService.getDefaultScenicId(), stampId));
    }

    @GetMapping("/events")
    ApiResponse<java.util.List<ScenicEventView>> listDefaultEvents(@RequestParam(defaultValue = "") String spotId) {
        return ApiResponse.success(scenicGameService.listEvents(scenicGuideService.getDefaultScenicId(), spotId));
    }

    @PostMapping("/events/{eventId}/interact")
    ApiResponse<ScenicEventInteractResponse> interactDefaultEvent(@PathVariable String eventId, @RequestBody ScenicEventInteractRequest request) {
        return ApiResponse.success(scenicGameService.interactEvent(scenicGuideService.getDefaultScenicId(), eventId, request));
    }

    @PostMapping("/guide/chat")
    ApiResponse<ScenicGuideChatResponse> defaultGuideChat(@RequestBody ScenicGuideChatRequest request) {
        return ApiResponse.success(scenicGameService.guideChat(scenicGuideService.getDefaultScenicId(), request));
    }

    @GetMapping("/live2d/config")
    ApiResponse<Live2dConfig> getDefaultLive2dConfig() {
        return ApiResponse.success(scenicGuideService.getLive2dConfig(scenicGuideService.getDefaultScenicId()));
    }

    @PostMapping("/live2d/chat")
    ApiResponse<Live2dChatResponse> defaultChat(@Valid @RequestBody Live2DChatRequest request) {
        String scenicId = scenicGuideService.getDefaultScenicId();
        return ApiResponse.success(live2dChatService.chat(withScenicId(request, scenicId)));
    }

    private Live2DChatRequest withScenicId(Live2DChatRequest request, String scenicId) {
        return new Live2DChatRequest(request.message(), scenicId, request.page(), request.history());
    }
}
