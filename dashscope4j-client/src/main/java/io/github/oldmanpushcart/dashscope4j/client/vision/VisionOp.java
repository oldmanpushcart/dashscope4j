package io.github.oldmanpushcart.dashscope4j.client.vision;

import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2v.Text2VideoOp;

public interface VisionOp {

    Text2ImageOp t2i();

    Text2VideoOp t2v();

}
