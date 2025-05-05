package io.github.oldmanpushcart.dashscope4j.client.api.image.generation;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

public interface GenImageModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseGenImageModel extends BaseModel implements GenImageModel {

        public BaseGenImageModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseGenImageModel(String name, URI remote) {
            super(name, remote);
        }

    }

    /**
     * WANX-V1
     * <p>通义万相-文本生成图像</p>
     * <p>
     * 基于自研的Composer组合生成框架的AI绘画创作大模型，能够根据用户输入的文字内容，生成符合语义描述的不同风格的图像。
     * 通过知识重组与可变维度扩散模型，加速收敛并提升最终生成图片的效果, 结果自然、细节丰富。支持中英文双语输入。
     * </p>
     */
    GenImageModel WANX_V1 = new BaseGenImageModel(
            "wanx-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
    );

    /**
     * WANX-V2.1-TURBO
     * <p>生成速度更快，通用生成模型。</p>
     *
     * @since 3.1.0
     */
    GenImageModel WANX_V2_1_TURBO = new BaseGenImageModel(
            "wanx2.1-t2i-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
    );

    /**
     * WANX-V2.1-PLUS
     * <p>生成图像细节更丰富，速度稍慢，通用生成模型。</p>
     *
     * @since 3.1.0
     */
    GenImageModel WANX_V2_1_PLUS = new BaseGenImageModel(
            "wanx2.1-t2i-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
    );

}
