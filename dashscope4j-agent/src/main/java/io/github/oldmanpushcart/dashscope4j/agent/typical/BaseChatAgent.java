package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
public abstract class BaseChatAgent implements ChatAgent {

    private final DashscopeClient client;
    private final Memory memory;
    private final List<Interceptor> interceptors;

    protected BaseChatAgent(Builder<?, ?> builder) {
        this.memory = builder.memory;
        this.client = builder.client;
        this.interceptors = unmodifiableList(builder.interceptors);
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::processChatRequestForInterceptors)
                .thenApply(this::processChatRequestForMemoryRecall)
                .thenCompose(this::baseAsync)
                .thenApply(response -> processPersistMemoryFragmentForAsync(request, response));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::processChatRequestForInterceptors)
                .thenApply(this::processChatRequestForMemoryRecall)
                .thenCompose(this::baseFlow)
                .thenApply(responseFlow -> processPersistMemoryFragmentForFlow(request, responseFlow));
    }

    private ChatRequest processChatRequestForInterceptors(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .addInterceptors(interceptors)
                .build();
    }

    private ChatRequest processChatRequestForMemoryRecall(ChatRequest request) {

        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return request;
        }

        return ChatRequest.newBuilder(request)
                .building(builder -> {

                    final List<Message> newMessages = new ArrayList<>();

                    // 先添加SYSTEM
                    request.messages()
                            .stream()
                            .filter(message -> message.role() == Message.Role.SYSTEM)
                            .forEach(newMessages::add);

                    // 然后添加回忆
                    memory.recall(context.sessionId(), context.olderThenFragmentId(), context.newerThenFragmentId())
                            .forEach(fragment -> {
                                newMessages.add(fragment.requestMessage());
                                newMessages.add(fragment.responseMessage());
                            });

                    // 最后添加请求原有的对话信息
                    request.messages()
                            .stream()
                            .filter(message -> message.role() != Message.Role.SYSTEM)
                            .forEach(newMessages::add);

                    // 替换原有的消息列表
                    builder.messages(newMessages);
                })
                .build();
    }

    private ChatResponse processPersistMemoryFragmentForAsync(ChatRequest request, ChatResponse response) {
        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return response;
        }

        final Message requestMessage = request.requireLastMessageFromUser();
        final Message responseMessage = response.output().best().message();
        final Memory.Fragment fragment = new Memory.Fragment()
                .fragmentId(context.newerThenFragmentId())
                .sessionId(context.sessionId())
                .requestMessage(requestMessage)
                .responseMessage(responseMessage)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());

        final long fragmentId = memory.persist(fragment);
        context.newerThenFragmentId(fragmentId);

        return response;
    }

    private Flowable<ChatResponse> processPersistMemoryFragmentForFlow(ChatRequest request, Flowable<ChatResponse> responseFlow) {

        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return responseFlow;
        }

        /*
         * 应答流式输出内容缓存
         * 所以这里需要一个字符串缓存来存储流式输出内容
         */
        final StringBuilder stringBuf = new StringBuilder();

        return responseFlow
                .doOnNext(response -> {

                    /*
                     * 如果不是增量输出，则说明是全量输出
                     * 需要每次均清空缓冲区
                     */
                    final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
                    if (!isIncrementalOutput) {
                        stringBuf.setLength(0);
                    }

                    // 将当前输出添加到输出缓存中
                    final String text = response.output().best().message().text();
                    stringBuf.append(text);

                })

                // 成功完成时触发记忆片段刷新
                .doOnComplete(() -> {

                    final Message requestMessage = request.requireLastMessageFromUser();
                    final Message responseMessage = Message.ofAi(stringBuf.toString());
                    final Memory.Fragment fragment = new Memory.Fragment()
                            .fragmentId(context.newerThenFragmentId())
                            .sessionId(context.sessionId())
                            .requestMessage(requestMessage)
                            .responseMessage(responseMessage)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now());

                    final long fragmentId = memory.persist(fragment);
                    context.newerThenFragmentId(fragmentId);

                });
    }

    abstract protected CompletionStage<ChatResponse> baseAsync(ChatRequest request);

    abstract protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request);

    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private DashscopeClient client;
        private Memory memory;
        private final List<Interceptor> interceptors = new ArrayList<>();

        public Builder() {

        }

        public Builder(BaseChatAgent agent) {
            this.client = agent.client;
            this.memory = agent.memory;
            this.interceptors.addAll(agent.interceptors);
        }

        /**
         * 设置 Dashscope4j 客户端
         *
         * @param client Dashscope4j 客户端
         * @return this
         */
        public B client(DashscopeClient client) {
            this.client = client;
            return self();
        }

        /**
         * 设置记忆体
         *
         * @param memory 记忆体
         * @return this
         */
        public B memory(Memory memory) {
            this.memory = memory;
            return self();
        }

        /**
         * 设置拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B interceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加拦截器
         *
         * @param interceptor 拦截器
         * @return this
         */
        public B addInterceptor(Interceptor interceptor) {
            requireNonNull(interceptor);
            this.interceptors.add(interceptor);
            return self();
        }

        /**
         * 添加拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B addInterceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.addAll(interceptors);
            return self();
        }

    }

}
