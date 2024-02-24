package io.github.ompc.dashscope4j.image.generation;

import io.github.ompc.dashscope4j.Model;

import java.net.URI;

/**
 * 文生图模型
 */
public class GenImageModel extends Model {

    /**
     * 构造文生图模型
     *
     * @param name   模型名称
     * @param remote 模型地址
     */
    public GenImageModel(String name, URI remote) {
        super(name, remote);
    }

    public static final GenImageModel WANX_V1 = new GenImageModel(
            "wanx-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
    );

}
