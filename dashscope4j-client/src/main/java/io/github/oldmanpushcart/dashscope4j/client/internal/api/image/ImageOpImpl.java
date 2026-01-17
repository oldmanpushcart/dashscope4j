package io.github.oldmanpushcart.dashscope4j.client.internal.api.image;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.image.interceptor.CompatChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.image.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class ImageOpImpl implements ImageOp {

    private final List<TaskInterceptor> interceptors = List.of(
            new UploadFilesInterceptor(),
            new CompatChatInterceptor()
    );

    private final TaskApi taskApi;

    public ImageOpImpl(DashscopeClient client, TaskApi taskApi) {
        this.taskApi = InterceptionTaskApi.group(client, taskApi, interceptors);
    }

    @Override
    public CompletionStage<? extends Task.Half<Text2ImageResponse>> text2image(Text2ImageRequest request) {
        return taskApi.execute(request);
    }

}
