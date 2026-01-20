package io.github.oldmanpushcart.dashscope4j.client.internal.vision;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2v.Text2VideoOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.vision.VisionOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2i.Text2ImageOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoResponse;

import java.util.concurrent.CompletionStage;

public class VisionOpImpl implements VisionOp {

    private final Text2ImageOp t2iOp;
    private final Text2VideoOp t2vOp;

    public VisionOpImpl(DashscopeClient client, TaskApi taskApi) {
        this.t2iOp = new Text2ImageOpImpl(client, taskApi);
        this.t2vOp = new Text2VideoOpImpl(client, taskApi);
    }

    @Override
    public Text2ImageOp t2i() {
        return t2iOp;
    }

    @Override
    public Text2VideoOp t2v() {
        return t2vOp;
    }

}
