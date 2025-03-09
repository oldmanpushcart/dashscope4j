package io.github.oldmanpushcart.dashscope4j.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.Model;
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
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class ImageGenVideoModel implements Model {

    String name;
    URI remote;

    public static final ImageGenVideoModel WANX_V2_1_I2V_TURBO = new ImageGenVideoModel(
            "wanx2.1-i2v-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

    public static final ImageGenVideoModel WANX_V2_1_I2V_PLUS = new ImageGenVideoModel(
            "wanx2.1-i2v-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

}
