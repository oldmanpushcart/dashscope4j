package io.github.oldmanpushcart.dashscope4j.client.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 文生视频模型
 *
 * @since 3.1.0
 */
public interface TextGenVideoModel extends Model {

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class DefaultTextGenVideoModel implements TextGenVideoModel {
        String name;
        URI remote;
    }

    TextGenVideoModel WANX_V2_1_T2V_TURBO = new DefaultTextGenVideoModel(
            "wanx2.1-t2v-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

    TextGenVideoModel WANX_V2_1_T2V_PLUS = new DefaultTextGenVideoModel(
            "wanx2.1-t2v-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

}
