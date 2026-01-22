package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface RewriteUserInputInterceptor extends AsyncInterceptor, FlowInterceptor, TaskInterceptor {

    @Override
    default CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {
        if (chain.request() instanceof AigcRequest<?, ?, ?> aigcRequest
                && aigcRequest.model() instanceof ChatModel model) {
            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> rewriteAigcRequest(chain, aigcRequest.as(model)))
                    .thenCompose(chain::proceed);
        } else {
            return chain.proceed(chain.request());
        }
    }

    @Override
    default CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {
        if (chain.request() instanceof AigcRequest<?, ?, ?> aigcRequest
                && aigcRequest.model() instanceof ChatModel model) {
            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> rewriteAigcRequest(chain, aigcRequest.as(model)))
                    .thenCompose(chain::proceed);
        } else {
            return chain.proceed(chain.request());
        }
    }

    @Override
    default CompletionStage<? extends Task.Half<?>> intercept(TaskInterceptor.Chain chain) {
        if (chain.request() instanceof AigcRequest<?, ?, ?> aigcRequest
                && aigcRequest.model() instanceof ChatModel model) {
            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> rewriteAigcRequest(chain, aigcRequest.as(model)))
                    .thenCompose(chain::proceed);
        } else {
            return chain.proceed(chain.request());
        }
    }

    private CompletionStage<AigcRequest<Input, Output, ChatModel>> rewriteAigcRequest(Interceptor.Chain chain, AigcRequest<Input, Output, ChatModel> request) {
        final var inputMessage = request.input().userInputMessage();
        return CompletableFuture.completedStage(null)
                .thenCompose(v -> rewriteUserInputMessage(chain, inputMessage))
                .thenApply(newInputMessage ->
                        AigcRequest.newBuilder(request)
                                .input(Input.newBuilder(request.input())
                                        .messages(request.input().historyMessages())
                                        .addMessage(newInputMessage)
                                        .build())
                                .build());
    }

    CompletionStage<Message> rewriteUserInputMessage(Interceptor.Chain chain, AigcRequest<Input, Output, ChatModel> request, UserMessage message);


}
