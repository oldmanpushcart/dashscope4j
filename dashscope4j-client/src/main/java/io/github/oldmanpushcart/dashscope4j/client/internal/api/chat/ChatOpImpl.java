package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class ChatOpImpl implements ChatOp {

    private static final List<Interceptor> interceptors = Arrays.asList(
            new ProcessAutoUploadForChatMessageInterceptor(),
            new ProcessContentForQwenLongInterceptor(),
            new ProcessToolCallForChatInterceptor()
    );
    private final ApiOp apiOp;

    public ChatOpImpl(ApiOp apiOp) {
        this.apiOp = apiOp;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .addInterceptors(interceptors)
                .build();
        return apiOp.executeAsync(newRequest);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .addInterceptors(interceptors)
                .build();
        return apiOp.executeFlow(newRequest);
    }

}
