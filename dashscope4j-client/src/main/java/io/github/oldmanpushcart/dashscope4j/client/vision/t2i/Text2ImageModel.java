package io.github.oldmanpushcart.dashscope4j.client.vision.t2i;

import io.github.oldmanpushcart.dashscope4j.client.AlgoModel;

import java.util.Set;

public class Text2ImageModel extends AlgoModel {

    public Text2ImageModel(String name, String path, Set<String> tags) {
        super(name, path, tags);
    }

    public Text2ImageModel(String name, String path) {
        super(name, path);
    }

    public static final Text2ImageModel QWEN_IMAGE_PLUS = new Text2ImageModel("qwen-image-plus", "/api/v1/services/aigc/text2image/image-synthesis");
    public static final Text2ImageModel QWEN_IMAGE = new Text2ImageModel("qwen-image", "/api/v1/services/aigc/text2image/image-synthesis");

    public static final Text2ImageModel WAN_T2I = new Text2ImageModel("wan2.6-t2i", "/api/v1/services/aigc/image-generation/generation", Set.of(
            Text2ImageModelTags.COMPAT_TASK_CHAT
    ));

}
