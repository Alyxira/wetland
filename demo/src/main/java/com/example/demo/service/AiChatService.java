package com.example.demo.service;

import com.example.demo.dto.AiChatRequest;
import com.example.demo.dto.AiChatResponse;
import com.example.demo.entity.WetlandFloraFauna;
import com.example.demo.entity.WetlandInfo;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.WetlandFloraFaunaRepository;
import com.example.demo.repository.WetlandInfoRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AiChatService {

    private static final List<String> FEATURED_WETLAND_KEYWORDS = List.of("九寨沟", "红海滩", "沉湖", "上涉湖");
    private static final List<String> PLANT_HINTS = List.of("植物", "树", "花", "草", "竹", "杉", "桐");
    private static final List<String> ANIMAL_HINTS = List.of("动物", "鸟", "兽", "鱼", "猴", "熊猫", "羚", "麝", "鹤", "鸥");

    private static final String SYSTEM_PROMPT = """
        你是 Wetland OS 内置的湿地助手。
        你的回答目标是帮助用户完成湿地科普问答、游览建议、系统功能说明、社区内容理解和基础生态知识解释。
        回答要求：
        1. 默认使用简体中文。
        2. 语气准确、自然、直接，像日常可用的 AI 助手，不要写成系统公告。
        2.1 多用直接回答，少用“如下”“目前可查看到”“已命中”等检索式表达。
        3. 如果系统提供了检索结果，优先基于这些结果回答，并明确区分“已确认的信息”和“补充说明”。
        4. 如果可用信息有限，就直接说“现在能确定的主要是……”，不要把未确认内容说成已经确定。
        5. 如果用户问路线、游玩季节、什么时候适合去、系统功能，优先结合 Wetland OS 当前页面能力、已知湿地信息和常识性建议回答。
        6. 不要把社区帖子内容当成湿地事实来源，不要用帖子内容回答湿地科普问题。
        7. 涉及珍稀动植物、湿地介绍、湿地推荐时，优先依据已检索到的字段生成答案，不要扩写为未提供的具体物种名单。
        6. 不要输出 markdown 标题，除非用户明确要求。
        7. 如果你不确定事实，明确说明不确定，不要编造。
        """;

    private static final String SUGGESTION_PROMPT = """
        你是 Wetland OS 的追问推荐生成器。
        请基于当前问答内容，生成 4 条自然、简短、适合继续追问的问题。
        约束：
        1. 每条都要像用户会直接点击发送的问题。
        2. 要与当前主题强相关，避免重复表达。
        3. 控制在 24 个字以内。
        4. 只返回 JSON 数组，不要返回其他文字。
        """;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final WetlandInfoRepository wetlandInfoRepository;
    private final WetlandFloraFaunaRepository wetlandFloraFaunaRepository;

    @Value("${ollama.api:http://localhost:11434/api/chat}")
    private String ollamaApi;

    @Value("${ollama.model:qwen:1.8b}")
    private String ollamaModel;

    public AiChatService(
        ObjectMapper objectMapper,
        WetlandInfoRepository wetlandInfoRepository,
        WetlandFloraFaunaRepository wetlandFloraFaunaRepository
    ) {
        this.objectMapper = objectMapper;
        this.wetlandInfoRepository = wetlandInfoRepository;
        this.wetlandFloraFaunaRepository = wetlandFloraFaunaRepository;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        String latestUserMessage = null;
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", SYSTEM_PROMPT));

        for (AiChatRequest.Message message : request.getMessages()) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            String trimmed = message.getContent().trim();
            messages.add(new OllamaMessage(message.getRole(), trimmed));
            if ("user".equalsIgnoreCase(message.getRole())) {
                latestUserMessage = trimmed;
            }
        }

        if (messages.size() <= 1 || !StringUtils.hasText(latestUserMessage)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "至少需要一条有效消息");
        }

        AiIntent intent = detectIntent(latestUserMessage);
        AiContext context = buildAiContext(latestUserMessage, intent);

        try {
            String reply = generateReply(latestUserMessage, messages, context);
            List<String> suggestedQuestions = generateSuggestedQuestions(latestUserMessage, reply, intent, context);

            return new AiChatResponse(
                true,
                "AI 回复成功",
                reply,
                intent.name().toLowerCase(Locale.ROOT),
                context.cards(),
                suggestedQuestions
            );
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 服务响应解析失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 服务调用被中断");
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 服务配置无效");
        }
    }

    private AiIntent detectIntent(String userMessage) {
        String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);

        if ((normalized.contains("推荐") && containsAny(normalized, "湿地", "景区", "科普浏览", "类似"))
            || containsAny(normalized, "推荐湿地", "推荐一个湿地", "推荐几个湿地", "推荐景点", "推荐去哪里", "适合去哪个湿地", "这四个湿地", "哪一个更适合")) {
            return AiIntent.RECOMMEND;
        }
        if (containsAny(normalized, "怎么用", "如何用", "系统", "功能", "页面", "登录", "注册", "模块", "上传", "搜索")) {
            return AiIntent.SYSTEM;
        }
        if (containsAny(normalized, "路线", "游览", "游玩", "行程", "怎么走", "怎么玩", "攻略", "推荐路线",
            "适合什么季节", "什么季节去", "什么时候去", "几月去", "几月份去", "最佳季节", "最佳时间", "适合去吗")) {
            return AiIntent.ROUTE;
        }
        if (containsAny(normalized, "社区", "帖子", "动态", "分享", "评论", "用户")) {
            return AiIntent.COMMUNITY;
        }
        if (containsAny(normalized, "动物", "植物", "鸟", "物种", "珍稀", "科普", "麋鹿", "白鹳")) {
            return AiIntent.SPECIES;
        }
        if (containsAny(normalized, "湿地", "景区", "介绍", "概况", "信息", "坐标", "位置", "特点")) {
            return AiIntent.WETLAND;
        }
        return AiIntent.GENERAL;
    }

    private AiContext buildAiContext(String userMessage, AiIntent intent) {
        QueryFocus queryFocus = detectQueryFocus(userMessage);
        List<WetlandInfo> wetlands = intent == AiIntent.RECOMMEND ? collectFeaturedWetlands() : collectWetlands(userMessage);
        List<WetlandFloraFauna> floraRecords = shouldCollectFlora(intent, queryFocus)
            ? collectFlora(userMessage, wetlands, intent, queryFocus)
            : List.of();

        String promptContext = buildPromptContext(userMessage, intent, wetlands, floraRecords);
        List<AiChatResponse.ReplyCard> cards = buildReplyCards(userMessage, intent, wetlands, floraRecords, queryFocus);

        return new AiContext(intent, queryFocus, promptContext, cards, wetlands, floraRecords);
    }

    private boolean shouldCollectFlora(AiIntent intent, QueryFocus queryFocus) {
        return intent == AiIntent.SPECIES
            || intent == AiIntent.WETLAND
            || intent == AiIntent.RECOMMEND
            || queryFocus != QueryFocus.GENERAL;
    }

    private List<WetlandInfo> collectWetlands(String userMessage) {
        List<WetlandInfo> activeWetlands = wetlandInfoRepository.findAllActiveWetlands();
        LinkedHashMap<Long, WetlandInfo> result = new LinkedHashMap<>();

        for (WetlandInfo wetland : activeWetlands) {
            if (matchesWetland(userMessage, wetland)) {
                result.put(wetland.getWetlandId(), wetland);
            }
        }

        wetlandInfoRepository.searchByKeyword(userMessage.trim()).stream()
            .limit(4)
            .forEach((wetland) -> result.putIfAbsent(wetland.getWetlandId(), wetland));

        return result.values().stream().limit(4).toList();
    }

    private List<WetlandFloraFauna> collectFlora(String userMessage, List<WetlandInfo> wetlands, AiIntent intent) {
        return collectFlora(userMessage, wetlands, intent, QueryFocus.GENERAL);
    }

    private List<WetlandFloraFauna> collectFlora(String userMessage, List<WetlandInfo> wetlands, AiIntent intent, QueryFocus queryFocus) {
        LinkedHashMap<Long, WetlandFloraFauna> result = new LinkedHashMap<>();

        for (WetlandInfo wetland : wetlands) {
            wetlandFloraFaunaRepository.findAllByRelatedWetlandIdOrderByCreatedTimeDesc(wetland.getWetlandId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .forEach((item) -> result.putIfAbsent(item.getId(), item));
        }

        if (intent == AiIntent.SPECIES || result.isEmpty()) {
            wetlandFloraFaunaRepository.searchByKeyword(userMessage.trim()).stream()
                .limit(6)
                .forEach((item) -> result.putIfAbsent(item.getId(), item));
        }

        List<WetlandFloraFauna> sorted = result.values().stream()
            .sorted(buildFloraComparator(wetlands, queryFocus))
            .limit(intent == AiIntent.SPECIES ? 8 : 4)
            .toList();

        if (queryFocus == QueryFocus.PLANT) {
            List<WetlandFloraFauna> plants = sorted.stream().filter(this::isPlant).filter(item -> !isAnimal(item)).limit(intent == AiIntent.SPECIES ? 8 : 4).toList();
            if (!plants.isEmpty()) {
                return plants;
            }
        }

        if (queryFocus == QueryFocus.ANIMAL) {
            List<WetlandFloraFauna> animals = sorted.stream().filter(this::isAnimal).filter(item -> !isPlant(item)).limit(intent == AiIntent.SPECIES ? 8 : 4).toList();
            if (!animals.isEmpty()) {
                return animals;
            }
        }

        return sorted;
    }

    private String buildPromptContext(
        String userMessage,
        AiIntent intent,
        List<WetlandInfo> wetlands,
        List<WetlandFloraFauna> floraRecords
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前识别的用户意图：").append(intent.name()).append("\n");
        builder.append("用户当前问题：").append(userMessage.trim()).append("\n");
        builder.append("以下是当前检索到的相关信息，请优先基于这些信息回答。\n");

        if (!wetlands.isEmpty()) {
            builder.append("湿地数据：\n");
            for (WetlandInfo wetland : wetlands) {
                builder.append("- 湿地：").append(defaultText(wetland.getWetlandName(), "未知湿地"))
                    .append("；类型标签：").append(defaultText(wetland.getTags(), "暂无标签"))
                    .append("；坐标：").append(defaultText(wetland.getCoordinateRange(), "暂无坐标"))
                    .append("；简介：").append(defaultText(wetland.getDescription(), "暂无简介"))
                    .append("；生态信息：").append(defaultText(wetland.getFloraFaunaInfo(), "暂无生态信息"))
                    .append("\n");
            }
        }

        if (!floraRecords.isEmpty()) {
            builder.append("珍稀动植物数据：\n");
            for (WetlandFloraFauna flora : floraRecords) {
                builder.append("- 物种：").append(defaultText(flora.getName(), "未命名物种"))
                    .append("；所属湿地：").append(resolveWetlandName(flora.getWetlandId()))
                    .append("；记录时间：").append(flora.getCreatedTime() == null ? "未知" : DATE_FORMATTER.format(flora.getCreatedTime().toLocalDate()))
                    .append("；介绍：").append(defaultText(flora.getDescription(), "暂无介绍"))
                    .append("\n");
            }
        }

        builder.append("附加回答要求：\n");
        builder.append("- 如果用户问“有哪些”“拥有哪几种”等列表型问题，优先列出已检索到的名称。\n");
        builder.append("- 如果用户要求推荐湿地，只推荐系统首页固定展示的四个湿地：九寨沟、红海滩、沉湖、上涉湖。\n");
        builder.append("- 如果是游玩季节、路线、攻略类问题，可以结合景区特点和常识给出建议，不必只重复湿地资料字段。\n");
        builder.append("- 如果问题更偏建议、比较、体验感受，不必强行罗列物种或坐标。\n");
        builder.append("- 如果是系统功能问题，优先说明用户可以访问的页面、模块和可执行路径。\n");
        return builder.toString();
    }

    private List<AiChatResponse.ReplyCard> buildReplyCards(
        String userMessage,
        AiIntent intent,
        List<WetlandInfo> wetlands,
        List<WetlandFloraFauna> floraRecords,
        QueryFocus queryFocus
    ) {
        if (!shouldPushCards(userMessage, intent, queryFocus, wetlands, floraRecords)) {
            return List.of();
        }

        List<AiChatResponse.ReplyCard> cards = new ArrayList<>();
        Long preferredWetlandId = wetlands.isEmpty() ? null : wetlands.get(0).getWetlandId();
        String preferredWetlandName = wetlands.isEmpty() ? null : wetlands.get(0).getWetlandName();

        switch (intent) {
            case SPECIES -> {
                floraRecords.stream().limit(4).forEach((item) -> cards.add(toFloraCard(item, preferredWetlandId, preferredWetlandName)));
            }
            case WETLAND -> {
                wetlands.stream().limit(1).forEach((item) -> cards.add(toWetlandDetailCard(item)));
            }
            case RECOMMEND -> {
                wetlands.stream().limit(4).forEach((item) -> cards.add(toWetlandDetailCard(item)));
            }
            case ROUTE -> {
                wetlands.stream().limit(1).forEach((item) -> cards.add(toWetlandDetailCard(item)));
            }
            case SYSTEM -> {
                cards.add(toStaticCard("system", "系统总览", "进入湿地总览页浏览全部湿地信息。", null, "Overview", "系统模块", "/overview"));
                cards.add(toStaticCard("system", "统一检索", "通过关键词检索湿地、珍稀动植物和社区内容。", null, "Search", "系统模块", "/search"));
                cards.add(toStaticCard("system", "AI 问答", "继续通过 AI 助手进行湿地科普、路线和功能咨询。", null, "AI Chat", "系统模块", "/ai"));
            }
            case COMMUNITY, GENERAL -> {
                // no-op
            }
        }

        return cards.stream().limit(4).toList();
    }

    private boolean shouldPushCards(
        String userMessage,
        AiIntent intent,
        QueryFocus queryFocus,
        List<WetlandInfo> wetlands,
        List<WetlandFloraFauna> floraRecords
    ) {
        if (intent == AiIntent.RECOMMEND) {
            return !wetlands.isEmpty();
        }
        if (intent == AiIntent.SPECIES) {
            return !floraRecords.isEmpty();
        }
        if (intent == AiIntent.WETLAND) {
            String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
            return !wetlands.isEmpty() && containsAny(normalized, "详情", "展开", "看看", "相关内容", "卡片", "页面");
        }
        if (intent == AiIntent.ROUTE) {
            String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
            return !wetlands.isEmpty() && containsAny(normalized, "路线", "攻略", "怎么走", "页面");
        }
        return false;
    }

    private List<String> generateSuggestedQuestions(String userMessage, String reply, AiIntent intent, AiContext context)
        throws IOException, InterruptedException {
        try {
            List<OllamaMessage> suggestionMessages = List.of(
                new OllamaMessage("system", SUGGESTION_PROMPT),
                new OllamaMessage(
                    "user",
                    """
                    当前意图：%s
                    用户问题：%s
                    助手回复：%s
                    关联湿地：%s
                    关联物种：%s
                    请生成 4 条更自然的追问。
                    """.formatted(
                        intent.name(),
                        userMessage,
                        reply,
                        joinShortWetlandNames(context.wetlands()),
                        joinFloraNames(context.floraRecords())
                    )
                )
            );

            String content = invokeChat(suggestionMessages);
            List<String> items = objectMapper.readValue(content, new TypeReference<List<String>>() {});
            List<String> cleaned = items.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(4)
                .toList();

            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        } catch (Exception ignored) {
            // Fallback to rules below.
        }

        return buildFallbackSuggestions(intent, context);
    }

    private List<String> buildFallbackSuggestions(AiIntent intent, AiContext context) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        String wetlandName = context.wetlands().stream()
            .map(this::shortWetlandName)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
        String speciesName = context.floraRecords().stream()
            .map(WetlandFloraFauna::getName)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);

        switch (intent) {
            case SPECIES -> {
                if (wetlandName != null) {
                    suggestions.add(wetlandName + "还有哪些重点植物？");
                    suggestions.add(wetlandName + "的生态环境有什么特点？");
                }
                if (speciesName != null) {
                    suggestions.add(speciesName + "在湿地里起什么作用？");
                }
                suggestions.add("这些珍稀动物适合在哪个季节观察？");
            }
            case WETLAND -> {
                if (wetlandName != null) {
                    suggestions.add(wetlandName + "有哪些珍稀动物？");
                    suggestions.add(wetlandName + "适合什么季节去？");
                    suggestions.add(wetlandName + "的生态特点是什么？");
                }
                suggestions.add("推荐一个类似的湿地景区");
            }
            case RECOMMEND -> {
                suggestions.add("这四个湿地分别有什么特点？");
                suggestions.add("哪一个更适合看珍稀动物？");
                suggestions.add("哪一个更适合第一次了解湿地？");
                suggestions.add("九寨沟有哪些珍稀动植物？");
            }
            case ROUTE -> {
                if (wetlandName != null) {
                    suggestions.add(wetlandName + "适合玩多久？");
                    suggestions.add(wetlandName + "适合什么时候去？");
                }
                suggestions.add("如果带老人小孩怎么安排路线？");
                suggestions.add("想看鸟类的话路线怎么调整？");
            }
            case SYSTEM -> {
                suggestions.add("我该从哪个页面开始使用系统？");
                suggestions.add("怎么快速查某个湿地的资料？");
                suggestions.add("AI 能帮我做哪些事情？");
                suggestions.add("珍稀动植物数据在哪里看？");
            }
            case COMMUNITY -> {
                suggestions.add("这个湿地有哪些值得关注的物种？");
                suggestions.add("可以直接看看该湿地详情吗？");
                suggestions.add("这个湿地适合什么季节去？");
                suggestions.add("还有哪些相近的湿地可以了解？");
            }
            case GENERAL -> {
                if (wetlandName != null) suggestions.add("继续介绍一下" + wetlandName);
                if (speciesName != null) suggestions.add("继续介绍一下" + speciesName);
                suggestions.add("推荐一个适合科普浏览的湿地");
                suggestions.add("湿地里的珍稀动物一般怎么保护？");
            }
        }

        return suggestions.stream().filter(StringUtils::hasText).limit(4).toList();
    }

    private String invokeChat(List<OllamaMessage> messages) throws IOException, InterruptedException {
        OllamaChatRequest ollamaRequest = new OllamaChatRequest(ollamaModel, messages, false);
        String payload = objectMapper.writeValueAsString(ollamaRequest);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(ollamaApi))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 服务调用失败");
        }

        OllamaChatResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaChatResponse.class);
        String reply = ollamaResponse != null && ollamaResponse.message != null ? ollamaResponse.message.content : null;
        if (!StringUtils.hasText(reply)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 服务未返回有效内容");
        }
        return reply.trim();
    }

    private boolean matchesWetland(String userMessage, WetlandInfo wetland) {
        if (wetland == null || !Boolean.TRUE.equals(wetland.getActive()) || !StringUtils.hasText(wetland.getWetlandName())) {
            return false;
        }

        String normalizedMessage = userMessage == null ? "" : userMessage.trim();
        String name = wetland.getWetlandName().trim();
        if (normalizedMessage.contains(name)) {
            return true;
        }

        for (String alias : buildWetlandAliases(name)) {
            if (alias.length() >= 2 && normalizedMessage.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> buildWetlandAliases(String name) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(name);
        aliases.add(name.replace("国家重要湿地", ""));
        aliases.add(name.replace("国家湿地公园", ""));
        aliases.add(name.replace("湿地公园", ""));
        aliases.add(name.replace("自然保护区", ""));
        aliases.add(name.replace("湿地", ""));
        String coreName = extractCoreWetlandName(name);
        if (StringUtils.hasText(coreName)) {
            aliases.add(coreName);
            aliases.add(coreName.replace("湿地", ""));
            aliases.add(coreName.replace("国家重要湿地", ""));
            aliases.add(coreName.replace("国家湿地公园", ""));
            aliases.add(coreName.replace("湿地公园", ""));
            aliases.add(coreName.replace("自然保护区", ""));
        }
        aliases.removeIf(alias -> !StringUtils.hasText(alias));
        return aliases;
    }

    private String extractCoreWetlandName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }

        String normalized = name.trim();
        String[] separators = {"特别行政区", "自治区", "自治州", "地区", "盟", "省", "市", "县", "区"};
        int cutIndex = -1;
        for (String separator : separators) {
            int index = normalized.lastIndexOf(separator);
            if (index >= 0) {
                cutIndex = Math.max(cutIndex, index + separator.length());
            }
        }

        if (cutIndex >= 0 && cutIndex < normalized.length()) {
            return normalized.substring(cutIndex).trim();
        }
        return normalized;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }

    private String generateReply(String userMessage, List<OllamaMessage> messages, AiContext context) throws IOException, InterruptedException {
        String directReply = buildDirectReply(userMessage, context);
        if (StringUtils.hasText(directReply)) {
            return directReply;
        }

        if (StringUtils.hasText(context.promptContext())) {
            messages.add(1, new OllamaMessage("system", context.promptContext()));
        }
        return invokeChat(messages);
    }

    private String buildDirectReply(String userMessage, AiContext context) {
        return switch (context.intent()) {
            case SPECIES -> buildSpeciesReply(context);
            case WETLAND -> shouldUseWetlandDirectReply(userMessage, context) ? buildWetlandReply(context) : null;
            case RECOMMEND -> buildRecommendReply(context);
            case COMMUNITY -> buildCommunityRedirectReply(context);
            case GENERAL -> shouldUseGeneralDirectReply(userMessage, context) ? buildGeneralReply(context) : null;
            default -> null;
        };
    }

    private boolean shouldUseWetlandDirectReply(String userMessage, AiContext context) {
        if (context.wetlands().isEmpty()) {
            return true;
        }
        String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        return !containsAny(normalized, "适合什么季节", "什么季节去", "什么时候去", "几月去", "几月份去", "最佳季节", "最佳时间", "适合去吗");
    }

    private boolean shouldUseGeneralDirectReply(String userMessage, AiContext context) {
        String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "适合什么季节", "什么季节去", "什么时候去", "几月去", "几月份去", "最佳季节", "最佳时间", "适合去吗",
            "路线", "游览", "游玩", "行程", "怎么走", "怎么玩", "攻略", "推荐路线")) {
            return false;
        }
        return !context.wetlands().isEmpty() || !context.floraRecords().isEmpty();
    }

    private String buildSpeciesReply(AiContext context) {
        if (context.wetlands().isEmpty() && context.floraRecords().isEmpty()) {
            return "我这边暂时没找到和你问题直接对应的珍稀动植物信息。你可以换一个湿地名称，或者直接问某个物种。";
        }

        StringBuilder builder = new StringBuilder();
        if (!context.wetlands().isEmpty()) {
            WetlandInfo wetland = context.wetlands().get(0);
            builder.append(shortWetlandName(wetland))
                .append("比较值得关注的")
                .append(context.queryFocus() == QueryFocus.PLANT ? "珍稀植物" : context.queryFocus() == QueryFocus.ANIMAL ? "珍稀动物" : "珍稀动植物")
                .append("有：");
        } else {
            builder.append("和你问题相关的珍稀动植物有：");
        }
        builder.append("\n");

        if (!context.floraRecords().isEmpty()) {
            for (int i = 0; i < Math.min(context.floraRecords().size(), 4); i++) {
                WetlandFloraFauna flora = context.floraRecords().get(i);
                builder.append(i + 1)
                    .append(". ")
                    .append(defaultText(flora.getName(), "未命名物种"))
                    .append("：")
                    .append(defaultText(flora.getDescription(), "暂时没有更详细介绍。"))
                    .append("\n");
            }
        } else if (!context.wetlands().isEmpty()) {
            builder.append(defaultText(context.wetlands().get(0).getFloraFaunaInfo(), "暂未补充更具体的物种清单。"))
                .append("\n");
        }

        builder.append("我再给你推送几张对应的动植物卡片，你可以直接点开看详情。");
        return builder.toString().trim();
    }

    private String buildWetlandReply(AiContext context) {
        if (context.wetlands().isEmpty()) {
            return "我这边暂时没找到和你问题直接对应的湿地信息。你可以换一个湿地名称再问我。";
        }

        WetlandInfo wetland = context.wetlands().get(0);
        StringBuilder builder = new StringBuilder();
        builder.append("如果你想先快速了解 ")
            .append(shortWetlandName(wetland))
            .append("，可以先看这几点：\n");
        builder.append("1. 它的基本情况是：").append(defaultText(wetland.getDescription(), "暂时还没有更完整的简介。")).append("\n");
        builder.append("2. 位置范围：").append(defaultText(wetland.getCoordinateRange(), "暂时还没有更具体的坐标信息。")).append("\n");
        builder.append("3. 生态特点：").append(defaultText(wetland.getFloraFaunaInfo(), "暂时还没有补充更详细的生态信息。"));

        if (!context.floraRecords().isEmpty()) {
            builder.append("\n4. 你还可以重点看看这些珍稀动植物：").append(joinFloraNames(context.floraRecords()));
        }

        return builder.toString().trim();
    }

    private String buildRecommendReply(AiContext context) {
        if (context.wetlands().isEmpty()) {
            return "如果你想先挑几个代表性湿地开始看，我建议先看 Overview 首屏固定的四个：九寨沟、红海滩、沉湖、上涉湖。";
        }

        StringBuilder builder = new StringBuilder("如果你想先从代表性湿地开始看，可以先看这四个：\n");
        for (int i = 0; i < context.wetlands().size(); i++) {
            WetlandInfo wetland = context.wetlands().get(i);
            builder.append(i + 1)
                .append(". ")
                .append(extractCoreWetlandName(wetland.getWetlandName()))
                .append("：")
                .append(truncate(wetland.getDescription(), "适合做湿地科普浏览。", 44));
            if (i < context.wetlands().size() - 1) {
                builder.append("\n");
            }
        }
        builder.append("\n我把这四个湿地的详情卡片一起推给你。");
        return builder.toString();
    }

    private String buildCommunityRedirectReply(AiContext context) {
        if (!context.wetlands().isEmpty() || !context.floraRecords().isEmpty()) {
            return buildGeneralReply(context);
        }
        return "如果你想了解具体湿地和珍稀动植物，我建议直接问湿地名称、物种名称，或者让我推荐一个适合科普浏览的湿地。";
    }

    private String buildGeneralReply(AiContext context) {
        if (!context.wetlands().isEmpty()) {
            return buildWetlandReply(context);
        }
        if (!context.floraRecords().isEmpty()) {
            return buildSpeciesReply(context);
        }
        return "你可以直接问我湿地介绍、珍稀动植物、湿地推荐或者页面功能。比如“九寨沟有哪些珍稀动植物？”这样问就可以。";
    }

    private AiChatResponse.ReplyCard toFloraCard(WetlandFloraFauna flora) {
        return toFloraCard(flora, null, null);
    }

    private AiChatResponse.ReplyCard toFloraCard(WetlandFloraFauna flora, Long preferredWetlandId, String preferredWetlandName) {
        Long primaryWetlandId = resolvePrimaryWetlandId(flora.getWetlandId()).orElse(null);
        Long resolvedWetlandId = preferredWetlandId != null && belongsToWetland(flora.getWetlandId(), preferredWetlandId)
            ? preferredWetlandId
            : primaryWetlandId;
        AiChatResponse.ReplyCard card = new AiChatResponse.ReplyCard();
        card.setType("flora");
        card.setId(flora.getId());
        card.setTitle(defaultText(flora.getName(), "珍稀动植物"));
        card.setDescription(truncate(flora.getDescription(), "查看该珍稀动植物档案。", 84));
        card.setImage(normalizeImagePath(flora.getImagePath()));
        card.setTag("珍稀动植物");
        card.setMeta(StringUtils.hasText(preferredWetlandName) && resolvedWetlandId != null && preferredWetlandId != null && preferredWetlandId.equals(resolvedWetlandId)
            ? preferredWetlandName
            : resolveWetlandName(flora.getWetlandId(), resolvedWetlandId));
        card.setPath(resolvedWetlandId == null ? "/flora/" + flora.getId() : "/flora/" + flora.getId() + "?wetlandId=" + resolvedWetlandId);
        return card;
    }

    private AiChatResponse.ReplyCard toWetlandScienceCard(WetlandInfo wetland) {
        AiChatResponse.ReplyCard card = new AiChatResponse.ReplyCard();
        card.setType("wetland");
        card.setId(wetland.getWetlandId());
        card.setTitle(defaultText(wetland.getWetlandName(), "湿地档案"));
        card.setDescription(truncate(wetland.getFloraFaunaInfo(), "查看该湿地的珍稀动植物与生态信息。", 84));
        card.setImage(normalizeImagePath(wetland.getImagePath()));
        card.setTag("湿地科普");
        card.setMeta(defaultText(wetland.getTags(), "湿地信息"));
        card.setPath("/science?wetlandId=" + wetland.getWetlandId());
        return card;
    }

    private AiChatResponse.ReplyCard toWetlandDetailCard(WetlandInfo wetland) {
        AiChatResponse.ReplyCard card = new AiChatResponse.ReplyCard();
        card.setType("wetland");
        card.setId(wetland.getWetlandId());
        card.setTitle(defaultText(wetland.getWetlandName(), "湿地档案"));
        card.setDescription(truncate(wetland.getDescription(), "查看该湿地详情。", 84));
        card.setImage(normalizeImagePath(wetland.getImagePath()));
        card.setTag("湿地详情");
        card.setMeta(defaultText(wetland.getCoordinateRange(), defaultText(wetland.getTags(), "湿地信息")));
        card.setPath("/detail/" + wetland.getWetlandId());
        return card;
    }

    private AiChatResponse.ReplyCard toStaticCard(
        String type,
        String title,
        String description,
        Long id,
        String tag,
        String meta,
        String path
    ) {
        AiChatResponse.ReplyCard card = new AiChatResponse.ReplyCard();
        card.setType(type);
        card.setId(id);
        card.setTitle(title);
        card.setDescription(description);
        card.setImage(null);
        card.setTag(tag);
        card.setMeta(meta);
        card.setPath(path);
        return card;
    }

    private String resolveWetlandName(String wetlandIds) {
        return resolveWetlandName(wetlandIds, resolvePrimaryWetlandId(wetlandIds).orElse(null));
    }

    private String resolveWetlandName(String wetlandIds, Long preferredWetlandId) {
        Long targetWetlandId = preferredWetlandId != null ? preferredWetlandId : resolvePrimaryWetlandId(wetlandIds).orElse(null);
        if (targetWetlandId == null) {
            return "湿地档案";
        }

        return wetlandInfoRepository.findById(targetWetlandId)
            .filter(WetlandInfo::getActive)
            .map(WetlandInfo::getWetlandName)
            .orElse("湿地档案");
    }

    private String truncate(String value, String fallback, int limit) {
        String source = defaultText(value, fallback);
        return source.length() > limit ? source.substring(0, limit) + "..." : source;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
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

    private boolean looksLikeLocalFile(String pathValue) {
        Path path = Paths.get(pathValue).normalize();
        return path.isAbsolute();
    }

    private QueryFocus detectQueryFocus(String userMessage) {
        String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        boolean asksPlant = containsAny(normalized, PLANT_HINTS.toArray(String[]::new));
        boolean asksAnimal = containsAny(normalized, ANIMAL_HINTS.toArray(String[]::new));
        if (asksPlant && !asksAnimal) {
            return QueryFocus.PLANT;
        }
        if (asksAnimal && !asksPlant) {
            return QueryFocus.ANIMAL;
        }
        return QueryFocus.GENERAL;
    }

    private Comparator<WetlandFloraFauna> buildFloraComparator(List<WetlandInfo> wetlands, QueryFocus queryFocus) {
        Set<Long> wetlandIds = wetlands.stream()
            .map(WetlandInfo::getWetlandId)
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> priorityNames = wetlands.stream()
            .map(WetlandInfo::getFloraFaunaInfo)
            .filter(StringUtils::hasText)
            .flatMap(info -> splitFloraFaunaInfo(info).stream())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return Comparator
            .comparingInt((WetlandFloraFauna item) -> scoreFlora(item, wetlandIds, priorityNames, queryFocus))
            .reversed()
            .thenComparing(WetlandFloraFauna::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int scoreFlora(WetlandFloraFauna item, Set<Long> wetlandIds, Set<String> priorityNames, QueryFocus queryFocus) {
        int score = 0;
        if (!wetlandIds.isEmpty() && wetlandIds.stream().anyMatch(id -> belongsToWetland(item.getWetlandId(), id))) {
            score += 100;
        }
        if (priorityNames.contains(defaultText(item.getName(), ""))) {
            score += 120;
        }

        int relationCount = countRelatedWetlands(item.getWetlandId());
        score += Math.max(0, 30 - relationCount);

        if (queryFocus == QueryFocus.PLANT && isPlant(item)) {
            score += 80;
        }
        if (queryFocus == QueryFocus.ANIMAL && isAnimal(item)) {
            score += 80;
        }
        if (queryFocus == QueryFocus.PLANT && isAnimal(item)) {
            score -= 60;
        }
        if (queryFocus == QueryFocus.ANIMAL && isPlant(item)) {
            score -= 60;
        }
        return score;
    }

    private boolean belongsToWetland(String wetlandIds, Long wetlandId) {
        if (wetlandId == null || !StringUtils.hasText(wetlandIds)) {
            return false;
        }
        return ("," + wetlandIds + ",").contains("," + wetlandId + ",");
    }

    private int countRelatedWetlands(String wetlandIds) {
        if (!StringUtils.hasText(wetlandIds)) {
            return 0;
        }
        int count = 0;
        for (String part : wetlandIds.split(",")) {
            if (StringUtils.hasText(part)) {
                count += 1;
            }
        }
        return count;
    }

    private boolean isPlant(WetlandFloraFauna item) {
        String text = ((item.getName() == null ? "" : item.getName()) + " " + (item.getDescription() == null ? "" : item.getDescription())).toLowerCase(Locale.ROOT);
        return containsAny(text, "植物", "草本", "灌木", "乔木", "竹", "树", "花", "杉", "桐", "草", "藓", "苇", "蒲", "茄", "红树", "竹类");
    }

    private boolean isAnimal(WetlandFloraFauna item) {
        String text = ((item.getName() == null ? "" : item.getName()) + " " + (item.getDescription() == null ? "" : item.getDescription())).toLowerCase(Locale.ROOT);
        return containsAny(text, "动物", "鸟类", "涉禽", "鱼", "兽", "猴", "熊猫", "羚", "麝", "鹭", "鹤", "鸥", "鸭", "蛙", "蟹", "豹", "鹿");
    }

    private List<String> splitFloraFaunaInfo(String info) {
        if (!StringUtils.hasText(info)) {
            return List.of();
        }
        String[] parts = info.split("[、,，；;：:]");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private List<WetlandInfo> collectFeaturedWetlands() {
        List<WetlandInfo> activeWetlands = wetlandInfoRepository.findAllActiveWetlands();
        List<WetlandInfo> featured = new ArrayList<>();
        for (String keyword : FEATURED_WETLAND_KEYWORDS) {
            activeWetlands.stream()
                .filter(wetland -> StringUtils.hasText(wetland.getWetlandName()) && wetland.getWetlandName().contains(keyword))
                .findFirst()
                .ifPresent(featured::add);
        }
        return featured;
    }

    private String joinWetlandNames(List<WetlandInfo> wetlands) {
        List<String> names = wetlands.stream()
            .map(WetlandInfo::getWetlandName)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        return names.isEmpty() ? "无" : String.join("、", names);
    }

    private String joinShortWetlandNames(List<WetlandInfo> wetlands) {
        List<String> names = wetlands.stream()
            .map(this::shortWetlandName)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        return names.isEmpty() ? "无" : String.join("、", names);
    }

    private String joinFloraNames(List<WetlandFloraFauna> floraRecords) {
        List<String> names = floraRecords.stream()
            .map(WetlandFloraFauna::getName)
            .filter(StringUtils::hasText)
            .distinct()
            .limit(6)
            .toList();
        return names.isEmpty() ? "无" : String.join("、", names);
    }

    private String shortWetlandName(WetlandInfo wetland) {
        if (wetland == null) {
            return "";
        }
        String core = extractCoreWetlandName(wetland.getWetlandName());
        return StringUtils.hasText(core) ? core : defaultText(wetland.getWetlandName(), "该湿地");
    }

    private record OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {
    }

    private record OllamaMessage(String role, String content) {
    }

    private record AiContext(
        AiIntent intent,
        QueryFocus queryFocus,
        String promptContext,
        List<AiChatResponse.ReplyCard> cards,
        List<WetlandInfo> wetlands,
        List<WetlandFloraFauna> floraRecords
    ) {
    }

    private enum AiIntent {
        SPECIES,
        WETLAND,
        RECOMMEND,
        ROUTE,
        SYSTEM,
        COMMUNITY,
        GENERAL
    }

    private enum QueryFocus {
        GENERAL,
        PLANT,
        ANIMAL
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaChatResponse {
        private OllamaMessageResponse message;

        public OllamaMessageResponse getMessage() {
            return message;
        }

        public void setMessage(OllamaMessageResponse message) {
            this.message = message;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaMessageResponse {
        private String role;

        @JsonProperty("content")
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
