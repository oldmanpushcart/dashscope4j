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

            new IncrementalOutputOnlyInterceptor(),

            // 文件上传到默认 OSS 空间
            new UploadFilesInterceptor(),

            // 音视频通过 BASE64 内联
            new InlineImageFilesInterceptor(),

            // 纯文本内容过滤（部分对话模型只支持纯文本内容）
            new TextOnlyInterceptor(),

            // 流桥接
            new FlowOnlyInterceptor(),

            // 兼容 OpenAI 协议
            new OpenAiCompatInterceptor()

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
                .transform(new ToolCallFlowHandler(this))
                .publisher();
    }

}
