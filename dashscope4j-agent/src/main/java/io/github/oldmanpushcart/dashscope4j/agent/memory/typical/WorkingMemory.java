package io.github.oldmanpushcart.dashscope4j.agent.memory.typical;

import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.memory.store.MemoryStore;
import io.github.oldmanpushcart.dashscope4j.agent.memory.store.MemoryStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorkingMemory implements Memory {

    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    private final MemoryStore store;
    private final DashscopeClient client;
    private final ChatModel model;
    private final int maxTokens;
    private final int retainTokens;

    public WorkingMemory(Builder builder) {

        Objects.requireNonNull(builder.client, "client must not be null");
        Objects.requireNonNull(builder.model, "model must not be null");
        Objects.requireNonNull(builder.store, "store must not be null");
        CheckUtils.require(builder.maxTokens, t -> t > 0, "maxTokens must be greater than 0");
        CheckUtils.require(builder.gcRatio, t -> t > 0 && t < 1, "gcRatio must in (0,1)");

        this.store = builder.store;
        this.client = builder.client;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.retainTokens = (int) (maxTokens * builder.gcRatio);
    }

    @Override
    public CompletionStage<List<Message>> recall(String sessionId, UserMessage instant) {
        return sessionMap.computeIfAbsent(sessionId, k -> new Session())
                .cacheGet(() -> {
                    final var tokensRef = new AtomicInteger();
                    return Flux.from(store.flow(sessionId, Long.MAX_VALUE))
                            .takeWhile(fragment -> tokensRef.addAndGet(fragment.tokens()) <= maxTokens)
                            .collectList()
                            .toFuture();
                });
    }

    @Override
    public CompletionStage<Void> remember(String sessionId, List<Message> messages) {

        final var session = sessionMap.get(sessionId);
        if (null == session) {
            return CompletableFuture.completedStage(null);
        }

        return store.upsert(sessionId, messages)
                .thenCompose(fragment -> session.push(maxTokens, fragment, this::compressHistory));
    }

    private CompletionStage<Session.CompressResult> compressHistory(List<Fragment> _fragments) {
        final var fragments = new ArrayList<>(_fragments);
        final var evictions = compact(retainTokens, fragments);

        final var history = evictions.stream()
                .sorted((o1, o2) -> Long.compare(o2.fragmentId(), o1.fragmentId()))
                .flatMap(f -> f.messages().stream())
                .toList();

        final var request = AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .addMessages(history)
                        .addMessage(Message.user("""
                                你是一个专业的对话摘要助手。请总结对话历史，生成一个简洁但全面的摘要。摘要应该：
                                1. 保留关键信息和重要细节
                                2. 忽略寒暄和无关内容
                                3. 用简洁的语言总结主要话题和结论
                                4. 保持在 200-500 字以内
                                5. 只输出摘要内容，不要添加任何解释或额外说明
                                """))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message())
                .thenApply(message -> new Session.CompressResult(fragments, message));
    }

    private static List<Fragment> compact(int retainTokens, List<Fragment> fragments) {
        final var evictions = new ArrayList<Fragment>();
        int tokens = 0;
        boolean evictFlag = false;
        final var removeIt = fragments.iterator();
        while (removeIt.hasNext()) {
            final var fragment = removeIt.next();
            if (!evictFlag && !(evictFlag = !(tokens + fragment.tokens() <= retainTokens))) {
                tokens += fragment.tokens();
            } else {
                removeIt.remove();
                evictions.add(0, fragment);
            }
        }
        return evictions;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(store);
    }

    private static class Session {

        private final List<Fragment> fragments = new ArrayList<>();
        private final AtomicReference<Message> summaryRef = new AtomicReference<>();
        private volatile boolean loaded = false;

        public CompletionStage<List<Message>> cacheGet(Supplier<CompletionStage<List<Fragment>>> loader) {
            final var getF = new CompletableFuture<List<Fragment>>();

            // 先从缓存中获取，若缓存没有则调用loader进行加载
            synchronized (this) {
                if (!loaded) {
                    Objects.requireNonNull(loader.get())
                            .whenComplete((fragments, ex) -> {
                                if (ex == null) {
                                    synchronized (this) {
                                        if (!loaded) {
                                            loaded = true;
                                            this.fragments.addAll(fragments);
                                        }
                                    }
                                    getF.complete(this.fragments);
                                } else {
                                    getF.completeExceptionally(ex);
                                }
                            });
                } else {
                    getF.complete(this.fragments);
                }
            }

            // 获取到数据后拼接为消息列表
            return getF
                    .thenApply(fragments -> {
                        final var messages = new ArrayList<Message>();

                        // 先添加摘要
                        final var summary = summaryRef.get();
                        if (null != summary) {
                            messages.add(summary);
                        }

                        // 倒序添加片段
                        for (int i = fragments.size() - 1; i >= 0; i--) {
                            messages.addAll(fragments.get(i).messages());
                        }

                        return messages;
                    })
                    .thenApply(Collections::unmodifiableList);
        }

        public CompletionStage<Void> push(int maxTokens, Fragment fragment, Function<List<Fragment>, CompletionStage<CompressResult>> compress) {
            fragments.add(0, fragment);

            // 计算缓存中的tokens
            final var tokens = fragments.stream()
                    .map(Fragment::tokens)
                    .reduce(Integer::sum)
                    .orElse(0);

            if (tokens <= maxTokens) {
                return CompletableFuture.completedStage(null);
            }

            return compress.apply(fragments)
                    .thenAccept(result -> {
                        synchronized (this) {
                            fragments.clear();
                            fragments.addAll(result.compacts());
                            summaryRef.set(result.summary());
                        }
                    });

        }

        public record CompressResult(List<Fragment> compacts, Message summary) {

        }

    }

    public static class Builder implements Buildable<WorkingMemory, Builder> {

        private MemoryStore store;
        private DashscopeClient client;
        private ChatModel model;
        private int maxTokens;
        private double gcRatio;

        public Builder store(MemoryStore store) {
            this.store = store;
            return this;
        }

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder gcRatio(double gcRatio) {
            this.gcRatio = gcRatio;
            return this;
        }


        @Override
        public WorkingMemory build() {
            return new WorkingMemory(this);
        }
    }

}
