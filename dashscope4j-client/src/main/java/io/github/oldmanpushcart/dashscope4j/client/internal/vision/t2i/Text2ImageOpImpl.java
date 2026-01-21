package io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2i;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2i.interceptor.CompatChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2i.interceptor.UploadFilesInterceptor;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.reverseListImmutable;

public class Text2ImageOpImpl implements Text2ImageOp {

    private final List<TaskInterceptor> interceptors = reverseListImmutable(List.of(
            new UploadFilesInterceptor(),
            new CompatChatInterceptor()
    ));

    private final TaskApi taskApi;

    public Text2ImageOpImpl(DashscopeClient client, TaskApi taskApi) {
        this.taskApi = InterceptionTaskApi.group(client, taskApi, interceptors);
    }

    @Override
    public CompletionStage<? extends Task.Half<Text2ImageResponse>> task(Text2ImageRequest request) {
        return taskApi.execute(request);
    }

}
