package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.*;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.*;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.reverseListImmutable;

public class ChatOpImpl implements ChatOp {

    private final AsyncApi asyncApi;
    private final FlowApi flowApi;

    private static final List<Interceptor> interceptors = reverseListImmutable(List.of(

            // 支持仅增量输出模型
            new IncrementalOutputOnlyInterceptor(),

            // 文件上传到默认 OSS 空间
            new UploadFilesInterceptor(),

            // 音视频通过 BASE64 内联
            new InlineFilesInterceptor(),

            // 支持仅流式输出模型
            new FlowOutputOnlyInterceptor(),

            // 支持仅异步输出模型
            new AsyncOutputOnlyInterceptor(),

            // 兼容纯文本协议
            new CompatPlaintextInterceptor(),

            // 兼容 OpenAI 协议
            new CompatOpenAiInterceptor()

    ));

    private static final List<FlowInterceptor> flowInterceptors = interceptors.stream()
            .filter(FlowInterceptor.class::isInstance)
            .map(FlowInterceptor.class::cast)
            .toList();

    private static final List<AsyncInterceptor> asyncInterceptors = interceptors.stream()
            .filter(AsyncInterceptor.class::isInstance)
            .map(AsyncInterceptor.class::cast)
            .toList();

    public ChatOpImpl(DashscopeClient client, AsyncApi asyncApi, FlowApi flowApi) {
        this.asyncApi = InterceptionAsyncApi.group(client, asyncApi, asyncInterceptors);
        this.flowApi = InterceptionFlowApi.group(client, flowApi, flowInterceptors);
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedStage(request)
                .thenCompose(asyncApi::execute)
                .thenCompose(new ToolCallHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        return FlowX.defer(() -> flowApi.execute(request))
                .transform(new ToolCallFlowHandler(this));
    }

}
