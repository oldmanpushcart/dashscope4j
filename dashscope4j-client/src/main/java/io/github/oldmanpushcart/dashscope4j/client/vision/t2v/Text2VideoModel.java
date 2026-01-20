package io.github.oldmanpushcart.dashscope4j.client.vision.t2v;

import io.github.oldmanpushcart.dashscope4j.client.AlgoModel;

import java.util.Set;

public class Text2VideoModel extends AlgoModel {

    protected Text2VideoModel(String name, String path, Set<String> tags) {
        super(name, path, tags);
    }

    public Text2VideoModel(String name, String path) {
        super(name, path);
    }

    public static final Text2VideoModel WAN_T2V = new Text2VideoModel("wan2.6-t2v", "/api/v1/services/aigc/video-generation/video-synthesis");

}
