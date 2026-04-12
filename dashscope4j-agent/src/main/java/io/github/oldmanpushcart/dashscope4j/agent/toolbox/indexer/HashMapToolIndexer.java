package io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer;

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

/**
 * 基于 HashMap 的工具索引器
 * <p>
 * 使用 LLM 对工具进行智能索引和检索，核心功能包括：
 * <ul>
 *     <li><b>元信息提取</b>：自动从工具描述中提取摘要、关键词、能力和约束</li>
 *     <li><b>意图匹配</b>：根据用户意图智能匹配最相关的工具</li>
 *     <li><b>并发安全</b>：使用 ConcurrentHashMap 保证线程安全</li>
 * </ul>
 * </p>
 *
 * @see ToolIndexer
 */
public class HashMapToolIndexer implements ToolIndexer {

    /**
     * DashScope 客户端
     */
    private final DashscopeClient client;
    
    /**
     * 用于索引的聊天模型
     */
    private final ChatModel model;

    /**
     * 工具索引表：工具名 -> 索引实体
     */
    private final Map<String, Entity> entities = new ConcurrentHashMap<>();

    /**
     * 工具路由匹配 PromptMessage
     */
    private static final Message TOOL_ROUTER_MESSAGE = SystemMessage.newBuilder()
            .contents(contents -> {
                final var prompt = PromptTemplate.newBuilder()
                        .template(HashMapToolIndexer.class.getResourceAsStream("/prompt/TOOL_ROUTER.md"))
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
                        .template(HashMapToolIndexer.class.getResourceAsStream("/prompt/TOOL_META_EXTRACTOR.md"))
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
     * 构造 HashMap 工具索引器
     *
     * @param builder 构建器
     */
    private HashMapToolIndexer(Builder builder) {
        Objects.requireNonNull(builder.client, "client must not be null");
        Objects.requireNonNull(builder.model, "model must not be null");
        this.client = builder.client;
        this.model = builder.model;
    }

    /**
     * 插入或更新工具索引
     * <p>
     * 调用 LLM 提取工具的元信息（摘要、关键词、能力、约束），
     * 并将提取结果存储到索引表中。
     * </p>
     *
     * @param name 工具名称
     * @param tool 工具实例（必须是 FunctionTool）
     * @return 完成时的 CompletionStage
     * @throws IllegalArgumentException 如果工具不是 FunctionTool 类型
     */
    @Override
    public CompletionStage<Void> upsert(String name, Tool tool) {

        if (!(tool instanceof FunctionTool functionTool)) {
            throw new IllegalArgumentException("tool must be FunctionTool");
        }

        // 构建元信息提取请求
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
                    // 创建索引实体并存储
                    final var index = new Entity(
                            functionTool.meta().name(),
                            functionTool.meta().description(),
                            meta
                    );
                    entities.put(index.name(), index);
                });
    }

    /**
     * 移除工具索引
     *
     * @param name 工具名称
     * @return 完成时的 CompletionStage
     */
    @Override
    public CompletionStage<Void> remove(String name) {
        entities.remove(name);
        return CompletableFuture.completedStage(null);
    }

    /**
     * 根据用户意图查询匹配的工具
     * <p>
     * 调用 LLM 对用户意图进行分析，从候选工具列表中筛选出最相关的工具。
     * 返回匹配的工具名称集合。
     * </p>
     *
     * @param instant 用户意图描述
     * @return 匹配的工具名称集合
     */
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
                    // 处理空结果
                    if (null == result || CommonUtils.isEmpty(result.items())) {
                        return Set.of();
                    }
                    // 提取工具名称
                    return result.items()
                            .stream()
                            .map(QueryResult.Item::name)
                            .collect(Collectors.toSet());
                });
    }

    /**
     * 关闭索引器
     * <p>
     * 当前实现无需释放资源，方法为空。
     * </p>
     */
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

    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * HashMapToolIndexer 构建器
     * <p>
     * 使用 Builder 模式配置工具索引器。
     * </p>
     */
    public static class Builder implements Buildable<HashMapToolIndexer, Builder> {

        /**
         * DashScope 客户端
         */
        private DashscopeClient client;
        
        /**
         * 聊天模型
         */
        private ChatModel model;

        /**
         * 设置 DashScope 客户端
         *
         * @param client DashScope 客户端
         * @return 当前构建器
         */
        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        /**
         * 设置聊天模型
         *
         * @param model 聊天模型
         * @return 当前构建器
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * 构建 HashMap 工具索引器
         *
         * @return 新创建的索引器实例
         */
        @Override
        public HashMapToolIndexer build() {
            return new HashMapToolIndexer(this);
        }

    }

}
