package io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.TextEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.VectorUtils;
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

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalStateStage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * 向量工具索引
 */
public class EmbeddingToolIndexer implements ToolIndexer {

    /**
     * 工具元信息提取 PromptMessage
     */
    private static final Message TOOL_META_EXTRACTOR_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/TOOL_META_EXTRACTOR.md")
                    .build()
                    .render())
            .withCache();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DashscopeClient client;
    private final ChatModel model;
    private final TextEmbeddingModel embeddingModel;
    private final IndexCache cache;
    private final Map<String, Document> documentMap = new ConcurrentHashMap<>();

    public EmbeddingToolIndexer(Builder builder) {
        Objects.requireNonNull(builder.client, "client must not be null!");
        Objects.requireNonNull(builder.model, "model must not be null!");
        Objects.requireNonNull(builder.embeddingModel, "embeddingModel must not be null!");
        this.client = builder.client;
        this.model = builder.model;
        this.embeddingModel = builder.embeddingModel;
        this.cache = new IndexCache(builder.storage);
    }


    /**
     * 提取工具文档元数据
     *
     * @param tool 工具
     * @return 工具文档元数据
     */
    private CompletionStage<Document.Meta> extractDocumentMeta(Tool tool) {

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
                .thenApply(json -> JacksonJsonUtils.toObject(json, Document.Meta.class));
    }

    /**
     * 提取工具文档向量
     *
     * @param tool 工具
     * @param meta 元数据信息
     * @return 工具文档向量
     */
    private CompletionStage<Document> extractDocumentVector(Tool tool, Document.Meta meta) {
        final var request = AigcRequest.newBuilder(embeddingModel)
                .input(TextEmbeddingModel.Input.newBuilder()
                        .texts(List.of(
                                JacksonJsonUtils.toJson(meta)
                        ))
                        .build())
                .parameters(parameters -> {
                    parameters.put("text_type", "document");
                    return parameters;
                })
                .build();
        return client.async(request)
                .thenApply(response -> {
                    final var embedding = response.output().embeddings().get(0);
                    return new Document(
                            tool.meta().name(),
                            tool.meta().description(),
                            meta,
                            embedding.vector()
                    );
                });
    }

    /**
     * 提取工具文档
     *
     * @param tool 工具
     * @return 工具文档
     */
    private CompletionStage<Document> extractDocument(Tool tool) {
        return CompletableFuture.completedStage(null)
                .thenCompose(u -> extractDocumentMeta(tool))
                .thenCompose(meta -> extractDocumentVector(tool, meta));
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox/indexer";
    }

    @Override
    public CompletionStage<Set<String>> query(String intent) {
        return embeddingIntent(intent)
                .thenApply(queryVector -> {
                    final var vectorMap = documentMap.values().stream()
                            .collect(Collectors.toMap(
                                    Document::name,
                                    Document::vectors
                            ));
                    return VectorUtils.search(queryVector, vectorMap, VectorUtils.DOT_PRODUCT, 5)
                            .stream()
                            .map(VectorUtils.Matched::key)
                            .collect(Collectors.toSet());
                });
    }

    private CompletionStage<float[]> embeddingIntent(String intent) {
        final var embeddingIntentRequest = AigcRequest.newBuilder(embeddingModel)
                .input(TextEmbeddingModel.Input.newBuilder()
                        .texts(List.of(intent))
                        .build())
                .parameters(parameters -> {
                    parameters.put("text_type", "query");
                    return parameters;
                })
                .build();
        return client.async(embeddingIntentRequest)
                .thenApply(response -> response.output().embeddings().get(0).vector());
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
                    return illegalStateStage(ex, "Indexing tool: %s occur error!".formatted(name));
                });
    }

    @Override
    public void remove(String name) {
        documentMap.remove(name);
    }

    /**
     * 工具文档
     *
     * @param name        工具名
     * @param description 工具描述
     * @param meta        元数据信息
     * @param vectors     向量
     */
    private record Document(String name, String description, Meta meta, float[] vectors) {

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

    private class IndexCache {

        private final Path storage;
        private final Map<String, Entry> entries = new ConcurrentHashMap<>();
        private final Object superThis = EmbeddingToolIndexer.this;

        private IndexCache(Path storage) {
            this.storage = storage;
            init();
        }

        /**
         * 加载缓存
         */
        private void init() {

            if (null == storage) {
                return;
            }

            try (final var __stream__ = Files.lines(storage)) {
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
                logger.warn("{} cache failed to read file. storage={};", superThis, storage, ioEx);
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
            if (null == storage) {
                return;
            }
            try {
                final var entryJson = JacksonJsonUtils.toJson(entry);
                Files.writeString(storage, entryJson + "\n", UTF_8, APPEND, CREATE);
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<EmbeddingToolIndexer, Builder> {

        private DashscopeClient client;
        private ChatModel model = ChatModel.QWEN_FLASH;
        private TextEmbeddingModel embeddingModel = TextEmbeddingModel.TEXT_EMBEDDING_V4;
        private Path storage;

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Builder embeddingModel(TextEmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public Builder storage(Path storage) {
            this.storage = storage;
            return this;
        }

        @Override
        public EmbeddingToolIndexer build() {
            return new EmbeddingToolIndexer(this);
        }

    }

}
