package io.github.oldmanpushcart.dashscope4j.client.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 图生视频模型
 *
 * @since 3.1.0
 */
public interface ImageGenVideoModel extends Model {

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class DefaultImageGenVideoModel implements ImageGenVideoModel {
        String name;
        URI remote;
    }

    ImageGenVideoModel WANX_V2_1_I2V_TURBO = new DefaultImageGenVideoModel(
            "wanx2.1-i2v-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

    ImageGenVideoModel WANX_V2_1_I2V_PLUS = new DefaultImageGenVideoModel(
            "wanx2.1-i2v-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

}
