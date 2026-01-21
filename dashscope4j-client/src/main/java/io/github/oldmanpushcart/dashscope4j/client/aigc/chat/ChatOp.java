package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ChatOp {

    CompletionStage<AigcResponse<Output>> async(AigcRequest<?, Output, ChatModel> request);

    CompletionStage<AigcResponse<Output>> async(AigcRequest<?, Output, ChatModel> request, List<AsyncInterceptor> interceptors);

    CompletionStage<? extends Task.Half<AigcResponse<Output>>> task(AigcRequest<?, Output, ChatModel> request);

    CompletionStage<? extends Task.Half<AigcResponse<Output>>> task(AigcRequest<?, Output, ChatModel> request, List<TaskInterceptor> interceptors);

    Flow.Publisher<AigcResponse<Output>> flow(AigcRequest<?, Output, ChatModel> request);

    Flow.Publisher<AigcResponse<Output>> flow(AigcRequest<?, Output, ChatModel> request, List<FlowInterceptor> interceptors);

    static Builder newBuilder() {
        return null;
    }

    interface Builder extends OpBuildable<ChatOp, Builder> {

        Builder interceptors(List<Interceptor> interceptors);

        Builder addInterceptor(Interceptor interceptor);

        Builder addInterceptors(List<Interceptor> interceptors);

    }

}
