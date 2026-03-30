package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具索引器
 */
public class ToolIndexer implements Repository.Indexer<String, Tool> {

    /**
     * 工具路由匹配 PromptMessage
     */
    private static final Message TOOL_ROUTER_MESSAGE = SystemMessage.newBuilder()
            .contents(contents -> {
                final var prompt = PromptTemplate.newBuilder()
                        .template(ToolIndexer.class.getResourceAsStream("/prompt/TOOL_ROUTER.md"))
                        .build()
                        .render();
                final var content = TextContent.newBuilder()
                        .cacheControl(Content.CacheControl.EPHEMERAL)
                        .text(prompt)
                        .build();
                return List.of(content);
            })
            .build();


    /**
     * 工具元信息提取 PromptMessage
     */
    private static final Message TOOL_META_EXTRACTOR_MESSAGE = SystemMessage.newBuilder()
            .contents(contents -> {
                final var prompt = PromptTemplate.newBuilder()
                        .template(ToolIndexer.class.getResourceAsStream("/prompt/TOOL_META_EXTRACTOR.md"))
                        .build()
                        .render();
                final var content = TextContent.newBuilder()
                        .cacheControl(Content.CacheControl.EPHEMERAL)
                        .text(prompt)
                        .build();
                return List.of(content);
            })
            .build();

    /**
     * 路由匹配结果集合类型
     * <p>用于 Jackson 的反序列化</p>
     */
    private static final Type routeMatchListType = new TypeReference<List<RouteMatch>>() {
    }.getType();

    /**
     * Dashscope 客户端
     */
    private final DashscopeClient client;

    /**
     * 索引集合
     */
    private final Map<String, Index> indexes = new ConcurrentHashMap<>();

    public ToolIndexer(DashscopeClient client) {
        this.client = client;
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/repo/tool/indexer";
    }

    @Override
    public CompletionStage<Void> init() {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Set<String>> lookup(UserMessage instant) {
        return match(instant)
                .thenApply(matches ->
                        matches.stream()
                                .map(RouteMatch::name)
                                .collect(Collectors.toSet()));
    }

    private CompletionStage<List<RouteMatch>> match(UserMessage instant) {
        final var request = AigcRequest.newBuilder(ChatModel.QWEN_FLASH)
                .input(ChatModel.Input.newBuilder()
                        .messages(messages -> List.of(
                                TOOL_ROUTER_MESSAGE,
                                Message.user(PromptTemplate.newBuilder()
                                        .template("""
                                                # Input Data
                                                - **用户意图**: ${INTENT}
                                                - **候选工具列表**:
                                                  ${TOOL_METAS}
                                                  (注意：每个工具包含 name, summary, capabilities, constraints 字段)
                                                """)
                                        .variable("INTENT", instant.text())
                                        .variable("TOOL_METAS", JacksonJsonUtils.toJson(indexes.values()))
                                        .build()
                                        .render())
                        ))
                        .build())
                .build();
        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> JacksonJsonUtils.<List<RouteMatch>>toObject(json, routeMatchListType));
    }

    @Override
    public CompletionStage<Void> upsert(String key, Tool item) {

        if (!(item instanceof FunctionTool tool)) {
            throw new IllegalArgumentException("Item is not a function tool");
        }

        final var request = AigcRequest.newBuilder(ChatModel.QWEN_FLASH)
                .input(ChatModel.Input.newBuilder()
                        .messages(messages -> List.of(
                                TOOL_META_EXTRACTOR_MESSAGE,
                                Message.user(PromptTemplate.newBuilder()
                                        .template("""
                                                # Input Data
                                                工具名称：${tool_name}
                                                
                                                工具描述:
                                                ```
                                                ${tool_description}
                                                ```
                                                """)
                                        .variable("tool_name", tool.meta().name())
                                        .variable("tool_description", tool.meta().description())
                                        .build()
                                        .render())
                        ))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> JacksonJsonUtils.toObject(json, Index.Meta.class))
                .thenAccept(meta -> {
                    final var index = new Index(
                            tool.meta().name(),
                            tool.meta().description(),
                            meta
                    );
                    indexes.put(index.key(), index);
                });
    }

    @Override
    public CompletionStage<Void> remove(String key) {
        indexes.remove(key);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public void close() {
    }

    /**
     * 路由匹配结果
     *
     * @param rank   排名
     * @param name   工具名
     * @param score  匹配得分
     * @param reason 匹配理由
     */
    private record RouteMatch(

            @JsonProperty("rank")
            int rank,

            @JsonProperty("name")
            String name,

            @JsonProperty("score")
            float score,

            @JsonProperty("reason")
            String reason

    ) {
    }

    /**
     * 索引项
     *
     * @param key         KEY（工具名）
     * @param description 工具描述
     * @param meta        索引元数据信息
     */
    private record Index(String key, String description, Meta meta) {

        /**
         * 索引元数据
         *
         * @param name         工具名
         * @param summary      描述摘要
         * @param keywords     关键词
         * @param capabilities 能力项：描述工具具有什么能力
         * @param constraints  约束项：描述工具使用上应受什么约束
         */
        public record Meta(

                @JsonProperty("name")
                String name,

                @JsonProperty("summary")
                String summary,

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                @JsonProperty("keywords")
                Set<String> keywords,

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                @JsonProperty("capabilities")
                List<String> capabilities,

                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                @JsonProperty("constraints")
                List<String> constraints

        ) {

        }

    }

}
