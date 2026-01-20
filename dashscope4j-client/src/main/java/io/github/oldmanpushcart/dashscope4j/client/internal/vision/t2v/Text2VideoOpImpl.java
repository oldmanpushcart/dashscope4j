package io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2v;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2v.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoResponse;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.reverseListImmutable;

public class Text2VideoOpImpl implements Text2VideoOp {

    private static final List<TaskInterceptor> interceptors = reverseListImmutable(List.of(
            new UploadFilesInterceptor()
    ));

    private final TaskApi taskApi;

    public Text2VideoOpImpl(DashscopeClient client, TaskApi taskApi) {
        this.taskApi = InterceptionTaskApi.group(client, taskApi, interceptors);
    }

    @Override
    public CompletionStage<? extends Task.Half<Text2VideoResponse>> task(Text2VideoRequest request) {
        return taskApi.execute(request);
    }

}
