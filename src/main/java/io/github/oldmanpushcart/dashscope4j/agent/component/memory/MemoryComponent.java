package io.github.oldmanpushcart.dashscope4j.agent.component.memory;

import io.github.oldmanpushcart.dashscope4j.agent.component.Component;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.reactivex.rxjava3.core.Flowable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 记忆体组件
 */
public class MemoryComponent implements Component {

    private final Memory memory;
    private final Integer maxCount;
    private final Integer maxTokens;
    private final Duration maxDuration;

    private MemoryComponent(Builder builder) {
        this.memory = builder.memory;
        this.maxCount = builder.maxCount;
        this.maxTokens = builder.maxTokens;
        this.maxDuration = builder.maxDuration;
    }

    @Override
    public CompletionStage<ChatResponse> onAsync(Processor<ChatResponse> processor) {
        return CompletableFuture.completedFuture(processor.request())
                .thenApply(this::processRecallForChatRequest)
                .thenCompose(processor::process)
                .thenApply(response -> processPersistForAsync(processor.request(), response));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> onFlow(Processor<Flowable<ChatResponse>> processor) {
        return CompletableFuture.completedFuture(processRecallForChatRequest(processor.request()))
                .thenApply(this::processRecallForChatRequest)
                .thenCompose(processor::process)
                .thenApply(response -> processPersistForFlow(processor.request(), response));
    }


    /*
     * 在对话列表中添加回忆部分
     * SYSTEM
     * HISTORY
     * LAST_USER_INPUT
     */
    private ChatRequest processRecallForChatRequest(ChatRequest request) {

        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Objects.isNull(context)
            || Objects.isNull(context.conversationId())) {
            return request;
        }

        final List<Message> newMessages = new ArrayList<>();

        // 先添加SYSTEM
        request.messages()
                .stream()
                .filter(message -> message.role() == Message.Role.SYSTEM)
                .forEach(newMessages::add);

        final long beginId = Optional
                .ofNullable(context.currentId())
                .orElse(Long.MAX_VALUE);

        final Memory.Condition condition = new Memory.Condition()
                .maxCount(maxCount)
                .maxTokens(maxTokens)
                .maxDuration(maxDuration)
                .beginId(beginId);

        // 然后添加回忆
        memory.recall(context.conversationId(), condition)
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
        return ChatRequest.newBuilder(request)
                .messages(newMessages)
                .build();

    }

    // 处理异步请求的记忆片段存储
    private ChatResponse processPersistForAsync(ChatRequest request, ChatResponse response) {

        // 如果没有记忆体则不需要处理
        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Objects.isNull(context)
            || Objects.isNull(context.conversationId())) {
            return response;
        }

        // 持久化记忆片段
        final Message requestMessage = request.requireLastMessageFromUser();
        final Message responseMessage = response.output().best().message();
        final Memory.Fragment fragment = new Memory.Fragment()
                .fragmentId(context.currentId())
                .conversationId(context.conversationId())
                .requestMessage(requestMessage)
                .responseMessage(responseMessage)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
        final long fragmentId = memory.persist(fragment);
        context.currentId(fragmentId);

        return response;
    }

    // 处理流式请求的记忆片段存储
    private Flowable<ChatResponse> processPersistForFlow(ChatRequest request, Flowable<ChatResponse> responseFlow) {

        // 如果没有记忆体则不需要处理
        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Objects.isNull(context)
            || Objects.isNull(context.conversationId())) {
            return responseFlow;
        }

        /*
         * 应答流式输出内容缓存
         * 所以这里需要一个字符串缓存来存储流式输出内容
         */
        final StringBuilder stringBuf = new StringBuilder();

        /*
         * 从流式回复中截留应答文本
         * 将应答文本存储到记忆体中
         */
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
                            .fragmentId(context.currentId())
                            .conversationId(context.conversationId())
                            .requestMessage(requestMessage)
                            .responseMessage(responseMessage)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now());

                    final long fragmentId = memory.persist(fragment);
                    context.currentId(fragmentId);

                });
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<MemoryComponent, Builder> {

        private Memory memory;
        private Integer maxCount;
        private Integer maxTokens;
        private Duration maxDuration;

        /**
         * 设置记忆体
         *
         * @param memory 记忆体
         * @return this
         */
        public Builder memory(Memory memory) {
            this.memory = memory;
            return this;
        }

        /**
         * 设置最大记忆片段数量
         *
         * @param maxCount 最大记忆片段数量
         * @return this
         */
        public Builder maxCount(Integer maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        /**
         * 设置最大记忆片段 TOKENS
         *
         * @param maxTokens 最大记忆片段 TOKENS
         * @return this
         */
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 设置最大记忆片段持续时间
         *
         * @param maxDuration 持续时间
         * @return this
         */
        public Builder maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }

        @Override
        public MemoryComponent build() {
            return new MemoryComponent(this);
        }

    }

}
