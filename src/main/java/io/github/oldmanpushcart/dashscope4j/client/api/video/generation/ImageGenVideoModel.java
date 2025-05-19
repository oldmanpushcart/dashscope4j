package io.github.oldmanpushcart.dashscope4j.client.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

/**
 * 图生视频模型
 *
 * @since 3.1.0
 */
public interface ImageGenVideoModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseImageGenVideoModel extends BaseModel implements ImageGenVideoModel {

        public BaseImageGenVideoModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseImageGenVideoModel(String name, URI remote) {
            super(name, remote);
        }

    }

    ImageGenVideoModel WANX_V2_1_I2V_TURBO = new BaseImageGenVideoModel(
            "wanx2.1-i2v-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

    ImageGenVideoModel WANX_V2_1_I2V_PLUS = new BaseImageGenVideoModel(
            "wanx2.1-i2v-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

}
