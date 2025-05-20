package io.github.oldmanpushcart.dashscope4j.client.api.video.generation;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

/**
 * 文生视频模型
 *
 * @since 3.1.0
 */
public interface TextGenVideoModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseTextGenVideoModel extends BaseModel implements TextGenVideoModel {

        public BaseTextGenVideoModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseTextGenVideoModel(String name, URI remote) {
            super(name, remote);
        }

    }

    TextGenVideoModel WANX_V2_1_T2V_TURBO = new BaseTextGenVideoModel(
            "wanx2.1-t2v-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

    TextGenVideoModel WANX_V2_1_T2V_PLUS = new BaseTextGenVideoModel(
            "wanx2.1-t2v-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis")
    );

}
