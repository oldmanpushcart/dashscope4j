package io.github.ompc.dashscope4j.image.generation;

import io.github.ompc.dashscope4j.Model;

import java.net.URI;

/**
 * 文生图模型
 *
 * @param name   模型名称
 * @param remote 模型地址
 */
public record GenImageModel(String name, URI remote) implements Model {

    /**
     * WANX-V1
     * <p>通义万相-文本生成图像</p>
     * <p>
     * 基于自研的Composer组合生成框架的AI绘画创作大模型，能够根据用户输入的文字内容，生成符合语义描述的不同风格的图像。
     * 通过知识重组与可变维度扩散模型，加速收敛并提升最终生成图片的效果, 结果自然、细节丰富。支持中英文双语输入。
     * </p>
     */
    public static final GenImageModel WANX_V1 = new GenImageModel(
            "wanx-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
    );

}
