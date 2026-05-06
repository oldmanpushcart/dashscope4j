package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class HashMapToolIndexer implements ToolIndexer {

    /**
     * 工具路由匹配 PromptMessage
     */
    private static final Message TOOL_ROUTER_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .template(HashMapToolIndexer.class.getResourceAsStream("/prompt/TOOL_ROUTER.md"))
                    .build()
                    .render())
            .withCache();

    /**
     * 工具元信息提取 PromptMessage
     */
    private static final Message TOOL_META_EXTRACTOR_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .template(HashMapToolIndexer.class.getResourceAsStream("/prompt/TOOL_META_EXTRACTOR.md"))
                    .build()
                    .render())
            .withCache();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DashscopeClient client;
    private final ChatModel model;

    private final IndexCache cache;
    private final Map<String, Document> documentMap = new ConcurrentHashMap<>();

    private HashMapToolIndexer(Builder builder) {
        Objects.requireNonNull(builder.client, "client must not be null!");
        Objects.requireNonNull(builder.model, "model must not be null!");
        this.client = builder.client;
        this.model = builder.model;
        this.cache = new IndexCache(builder.cacheFile);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox/indexer";
    }

    @Override
    public CompletionStage<Void> upsert(Tool tool) {

        /*
         * 更新工具索引的逻辑顺序
         * 1. 计算工具的缓存文档
         * 2. 将工具缓存文档纳入HashMap中
         */
        return cache

                /*
                 * 在计算缓存索引之前，先查找下缓存是否之前已经计算过。
                 * 如果缓存不存在则才会真正计算工具文档
                 */
                .cacheGet(tool, this::extractDocument)

                // 成功计算出工具文档，纳入到文档集中
                .thenAccept(document -> {
                    final var name = tool.meta().name();
                    documentMap.put(name, document);
                })

                // 计算文档失败，则更新工具索引失败
                .exceptionallyCompose(ex -> {
                    final var name = tool.meta().name();
                    return illegalState(ex, "Indexing tool: %s occur error!".formatted(name));
                });
    }


    /**
     * 提取工具文档
     *
     * @param tool 工具
     * @return 工具文档
     */
    private CompletionStage<Document> extractDocument(Tool tool) {

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
                                        .variable("tool_name", tool.meta().name())
                                        .variable("tool_description", tool.meta().description())
                                        .build()
                                        .render())
                        ))
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of(
                            "type", "json_object"
                    ));
                    return parameters;
                })
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> JacksonJsonUtils.toObject(json, Document.Meta.class))
                .thenApply(meta -> new Document(
                        tool.meta().name(),
                        tool.meta().description(),
                        meta
                ));
    }


    @Override
    public void remove(String name) {
        documentMap.remove(name);
    }

    @Override
    public CompletionStage<Set<String>> query(String intent) {
        // 候选工具集合消息
        final var candidateToolsMessage = Message.assistant(Content
                .text("""
                        ### 候选工具列表
                        %s
                        """.formatted(JacksonJsonUtils.toJson(documentMap.values())))
                .withCache());

        // 用户意图消息
        final var userInputMessage = Message.user("""
                ### 用户意图
                %s
                """.formatted(intent));

        final var request = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(TOOL_ROUTER_MESSAGE)
                        .addMessage(candidateToolsMessage)
                        .addMessage(userInputMessage)
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of(
                            "type", "json_object"
                    ));
                    return parameters;
                })
                .build();
        return client.async(request)
                .<Set<String>>thenApply(response -> {

                    final var resultJson = response.output().best().message().text();
                    final var result = JacksonJsonUtils.toObject(resultJson, QueryResult.class);

                    // 处理空结果
                    if (null == result || CommonUtils.isEmpty(result.items())) {
                        return Set.of();
                    }
                    // 提取工具名称
                    return result.items()
                            .stream()
                            .map(QueryResult.Item::name)
                            .collect(Collectors.toSet());

                })
                .exceptionallyCompose(ex -> illegalState(ex, "Indexing tools by intent occur error!"));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private DashscopeClient client;
        private ChatModel model;
        private Path cacheFile;

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Builder cacheFile(Path cacheFile) {
            this.cacheFile = cacheFile;
            return this;
        }

        public HashMapToolIndexer build() {
            return new HashMapToolIndexer(this);
        }

    }


    // ---- 内部类定义 ----

    /**
     * 工具文档
     *
     * @param name        工具名
     * @param description 工具描述
     * @param meta        元数据信息
     */
    private record Document(String name, String description, Document.Meta meta) {

        /**
         * 元数据
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
     * 索引缓存
     */
    private class IndexCache {

        private final Path cacheFile;
        private final Map<String, Entry> entries = new ConcurrentHashMap<>();
        private final Object superThis = HashMapToolIndexer.this;

        private IndexCache(Path cacheFile) {
            this.cacheFile = cacheFile;
            init();
        }

        /**
         * 加载缓存
         */
        private void init() {

            if (null == cacheFile || !Files.exists(cacheFile) || !Files.isReadable(cacheFile)) {
                return;
            }

            try (final var __stream__ = Files.lines(cacheFile)) {
                __stream__
                        .filter(CommonUtils::isNotBlankString)
                        .forEach(line -> {
                            try {
                                final var entry = JacksonJsonUtils.toObject(line, Entry.class);
                                entries.put(entry.key(), entry);
                            } catch (Throwable ex) {
                                logger.warn("{} cache ignored entry, because JSON parse error. line={};", superThis, line, ex);
                            }
                        });
                logger.debug("{} cache loaded. size={};", superThis, entries.size());
            } catch (IOException ioEx) {
                logger.warn("{} cache failed to read file. file={};", superThis, cacheFile, ioEx);
            }

        }

        public CompletionStage<Document> cacheGet(Tool tool, Function<Tool, CompletionStage<Document>> loader) {
            final var key = computeKey(tool);
            final var entry = entries.get(key);
            if (null != entry) {
                return CompletableFuture.completedStage(entry.document());
            }
            return loader.apply(tool)
                    .thenApply(document -> {
                        final var newEntry = new Entry(key, document);
                        entries.put(key, newEntry);
                        persist(newEntry);
                        return document;
                    });
        }

        private static String computeKey(Tool tool) {
            return "%s|%s".formatted(tool.meta().name(), tool.meta().description());
        }

        private void persist(Entry entry) {
            try {
                final var entryJson = JacksonJsonUtils.toJson(entry);
                Files.writeString(cacheFile, entryJson + "\n", UTF_8, APPEND, CREATE);
            } catch (Throwable ex) {
                logger.warn("{} cache persist error! key={};", superThis, entry.key(), ex);
            }
        }

        /**
         * 工具文档缓存项
         *
         * @param key      缓存KEY
         * @param document 工具文档
         */
        private record Entry(

                @JsonProperty("key")
                String key,

                @JsonProperty("document")
                Document document

        ) {

        }

    }

}
