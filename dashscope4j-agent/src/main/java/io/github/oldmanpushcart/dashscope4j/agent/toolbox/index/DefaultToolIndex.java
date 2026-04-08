package io.github.oldmanpushcart.dashscope4j.agent.toolbox.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultToolIndex implements ToolIndex {

    private final DashscopeClient client;
    private final ChatModel model;

    private final Map<String, Entity> entities = new ConcurrentHashMap<>();

    /**
     * 工具路由匹配 PromptMessage
     */
    private static final Message TOOL_ROUTER_MESSAGE = SystemMessage.newBuilder()
            .contents(contents -> {
                final var prompt = PromptTemplate.newBuilder()
                        .template(DefaultToolIndex.class.getResourceAsStream("/prompt/TOOL_ROUTER.md"))
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
                        .template(DefaultToolIndex.class.getResourceAsStream("/prompt/TOOL_META_EXTRACTOR.md"))
                        .build()
                        .render();
                final var content = TextContent.newBuilder()
                        .cacheControl(Content.CacheControl.EPHEMERAL)
                        .text(prompt)
                        .build();
                return List.of(content);
            })
            .build();

    private DefaultToolIndex(Builder builder) {
        Objects.requireNonNull(builder.client, "client must not be null");
        Objects.requireNonNull(builder.model, "model must not be null");
        this.client = builder.client;
        this.model = builder.model;
    }

    @Override
    public CompletionStage<Void> upsert(String name, Tool tool) {

        if (!(tool instanceof FunctionTool functionTool)) {
            throw new IllegalArgumentException("tool must be FunctionTool");
        }

        final var request = AigcRequest.newBuilder(model)
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
                                        .variable("tool_name", functionTool.meta().name())
                                        .variable("tool_description", functionTool.meta().description())
                                        .build()
                                        .render())
                        ))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> JacksonJsonUtils.toObject(json, Entity.Meta.class))
                .thenAccept(meta -> {
                    final var index = new Entity(
                            functionTool.meta().name(),
                            functionTool.meta().description(),
                            meta
                    );
                    entities.put(index.name(), index);
                });
    }

    @Override
    public CompletionStage<Void> remove(String name) {
        entities.remove(name);
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Set<String>> query(String instant) {
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
                                        .variable("INTENT", instant)
                                        .variable("TOOL_METAS", JacksonJsonUtils.toJson(entities.values()))
                                        .build()
                                        .render())
                        ))
                        .build())
                .build();
        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> JacksonJsonUtils.toObject(json, QueryResult.class))
                .thenApply(result -> {
                    if (null == result || CommonUtils.isEmpty(result.items())) {
                        return Set.of();
                    }
                    return result.items()
                            .stream()
                            .map(QueryResult.Item::name)
                            .collect(Collectors.toSet());
                });
    }

    @Override
    public void close() {

    }

    /**
     * 搜索结果
     *
     * @param items 搜索结果集
     */
    private record QueryResult(

            @JsonProperty("items")
            List<Item> items

    ) {

        /**
         * 搜索数据
         *
         * @param rank   排名
         * @param name   工具名
         * @param score  搜索得分
         * @param reason 得分理由
         */
        public record Item(

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

    }


    /**
     * 索引项
     *
     * @param name        工具名
     * @param description 工具描述
     * @param meta        索引元数据信息
     */
    private record Entity(String name, String description, Meta meta) {

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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<DefaultToolIndex, Builder> {

        private DashscopeClient client;
        private ChatModel model;

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        @Override
        public DefaultToolIndex build() {
            return new DefaultToolIndex(this);
        }

    }

}
