package io.github.oldmanpushcart.dashscope4j.agent.memory.working;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.common.util.flow.FlowX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class WorkingMemorySessionInterceptor implements Interceptor {

    private final WorkingMemory.Session session;
    private final WorkingMemory.RecallOptions options;

    public WorkingMemorySessionInterceptor(WorkingMemory.Session session, WorkingMemory.RecallOptions options) {
        this.session = session;
        this.options = options;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        final var newRequest = AigcRequest.newBuilder(request)
                .input(ChatModel.Input.newBuilder(request.input())
                        .building(builder -> {

                            final var newMessages = new ArrayList<Message>();

                            // STEP-1：添加 SYSTEM
                            request.input().messages().stream()
                                    .filter(message -> message.role() == Message.Role.SYSTEM)
                                    .forEach(newMessages::add);

                            // STEP-2：添加 RECALL
                            session.recall(options)
                                    .stream()
                                    .map(entry -> List.of(entry.inbound(), entry.outbound()))
                                    .forEach(newMessages::addAll);

                            // STEP-3：添加剩余消息
                            request.input().messages().stream()
                                    .filter(message -> message.role() != Message.Role.SYSTEM)
                                    .forEach(newMessages::add);

                            builder.messages(newMessages);

                        })
                        .build())
                .build();

        return chain.proceed(newRequest)
                .thenApply(r -> {
                    final var inbound = request.input().userInputMessage();
                    return switch (chain.type()) {

                        case ASYNC -> {
                            //noinspection unchecked
                            final var response = (AigcResponse<Output>) r;
                            final var outbound = response.output().best().message();
                            final var tokens = response.usage().total(item -> "total".equals(item.name()));
                            session.remember(inbound, outbound, tokens);
                            yield r;
                        }

                        case FLOW -> {
                            //noinspection unchecked
                            final var publisher = (Flow.Publisher<AigcResponse<Output>>) r;
                            final var responseRef = new AtomicReference<AigcResponse<Output>>();
                            yield FlowX.fromPublisher(publisher)
                                    .doOnNext(response -> {

                                        if (responseRef.get() == null) {
                                            responseRef.set(response);
                                        } else {
                                            final var newResponse = responseRef.get().accumulate(response);
                                            responseRef.set(newResponse);
                                        }

                                    })
                                    .doOnComplete(() -> {
                                        final var response = responseRef.get();
                                        final var outbound = response.output().best().message();
                                        final var tokens = response.usage().total(item -> "total".equals(item.name()));
                                        session.remember(inbound, outbound, tokens);
                                    });
                        }

                        case TASK -> {
                            //noinspection unchecked
                            final var half = (Task.Half<AigcResponse<Output>>) r;
                            yield (Task.Half<AigcResponse<Output>>) strategy ->
                                    half.waitingFor(strategy)
                                            .thenApply(response -> {
                                                final var outbound = response.output().best().message();
                                                final var tokens = response.usage().total(item -> "total".equals(item.name()));
                                                session.remember(inbound, outbound, tokens);
                                                return response;
                                            });
                        }

                    };
                });
    }

}
