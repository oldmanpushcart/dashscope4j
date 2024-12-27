package io.github.oldmanpushcart.dashscope4j.api.image.generation;

import io.github.oldmanpushcart.dashscope4j.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class GenImageModel implements Model {

    String name;
    URI remote;

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
