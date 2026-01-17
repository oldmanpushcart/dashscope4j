package io.github.oldmanpushcart.dashscope4j.client.internal.image;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.vision.VisionOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.image.t2i.Text2ImageOpImpl;

public class VisionOpImpl implements VisionOp {

    private final Text2ImageOp t2iOp;

    public VisionOpImpl(DashscopeClient client, TaskApi taskApi) {
        this.t2iOp = new Text2ImageOpImpl(client, taskApi);
    }

    @Override
    public Text2ImageOp t2i() {
        return t2iOp;
    }

}
