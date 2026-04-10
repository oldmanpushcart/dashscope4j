package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class MemoryInterceptor implements ChatInterceptor {

    private final String sessionId;
    private final Memory memory;

    public MemoryInterceptor(String sessionId, Memory memory) {
        this.sessionId = sessionId;
        this.memory = memory;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        return CompletableFuture.completedStage(request)
                .thenCompose(this::recall)
                .thenCompose(chain::proceed)
                .thenCompose(r -> switch (chain.type()) {
                    case ASYNC -> {
                        //noinspection unchecked
                        final var response = (AigcResponse<Output>) r;
                        yield rememberAsync(request, response)
                                .thenApply(Function.identity());
                    }
                    case FLOW -> {
                        //noinspection unchecked
                        final var flow = (Publisher<AigcResponse<Output>>) r;
                        yield rememberFlow(request, flow)
                                .thenApply(Function.identity());
                    }
                    case TASK -> {
                        //noinspection unchecked
                        final var half = (Task.Half<AigcResponse<Output>>) r;
                        yield rememberTask(request, half)
                                .thenApply(Function.identity());
                    }
                });

    }

    private CompletionStage<AigcRequest<Input, Output>> recall(AigcRequest<Input, Output> request) {
        final var instant = request.input().userInputMessage();
        return memory.recall(sessionId, instant)
                .thenApply(recallMessages -> AigcRequest.newBuilder(request)
                        .input(input -> Input.newBuilder(input)
                                .messages(message -> {

                                    final var systemMessages = input.messages().stream()
                                            .filter(m -> m instanceof SystemMessage)
                                            .toList();

                                    final var newMessages = new ArrayList<Message>();
                                    newMessages.addAll(systemMessages);
                                    newMessages.addAll(recallMessages);
                                    newMessages.add(instant);
                                    return newMessages;
                                })
                                .build())
                        .interceptors(interceptors -> {
                            interceptors.add(new MemoryInterceptor(sessionId, memory));
                            return interceptors;
                        })
                        .build());
    }

    private CompletionStage<AigcResponse<Output>> rememberAsync(AigcRequest<Input, Output> request, AigcResponse<Output> response) {
        final var inbound = request.input().userInputMessage();
        final var outbound = response.output().best().message();
        return memory.remember(sessionId, List.of(inbound, outbound))
                .thenApply(unused -> response);
    }

    private CompletionStage<Publisher<AigcResponse<Output>>> rememberFlow(AigcRequest<Input, Output> request, Publisher<AigcResponse<Output>> flow) {
        final var responseRef = new AtomicReference<AigcResponse<Output>>();

        // 在流的末尾拼接一个由 memory.remember 构成的空流
        final var wrapFlow = Flux.from(flow)
                .doOnNext(response -> responseRef.updateAndGet(current -> current == null ? response : current.accumulate(response)))
                .concatWith(Flux.defer(() -> {
                    // 流完成后，执行记忆操作并返回空流
                    final var accumulatedResponse = responseRef.get();
                    if (accumulatedResponse != null) {
                        final var inbound = request.input().userInputMessage();
                        final var outbound = accumulatedResponse.output().best().message();
                        // 将异步的记忆操作转换为 Mono，记忆完成后返回空流
                        return Mono
                                .fromCompletionStage(memory.remember(sessionId, List.of(inbound, outbound)))
                                .thenMany(Flux.empty());
                    } else {
                        return Flux.empty();
                    }
                }));
        return CompletableFuture.completedStage(wrapFlow);
    }

    private CompletionStage<Task.Half<AigcResponse<Output>>> rememberTask(AigcRequest<Input, Output> request, Task.Half<AigcResponse<Output>> half) {
        final Task.Half<AigcResponse<Output>> wrapHalf = strategy -> half.waitingFor(strategy)
                .thenCompose(response -> {
                    final var inbound = request.input().userInputMessage();
                    final var outbound = response.output().best().message();
                    return memory.remember(sessionId, List.of(inbound, outbound))
                            .thenApply(unused -> response);
                });
        return CompletableFuture.completedStage(wrapHalf);
    }

}
