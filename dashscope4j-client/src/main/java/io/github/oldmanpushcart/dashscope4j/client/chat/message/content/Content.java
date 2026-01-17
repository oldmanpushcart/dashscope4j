package io.github.oldmanpushcart.dashscope4j.client.chat.message.content;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

/**
 * 消息内容的多态接口，支持文本、图像、音频、视频。
 */
public sealed interface Content permits AudioContent, ImageContent, TextContent, VideoContent {

    /**
     * 返回上下文缓存控制策略。
     */
    @JsonProperty("cache_control")
    CacheControl cacheControl();

    /**
     * 上下文缓存控制策略
     * <p>
     * <a hreef="https://help.aliyun.com/zh/model-studio/context-cache">上下文缓存</a>
     * </p>
     */
    record CacheControl(

            @JsonProperty("type")
            Type type

    ) {

        /**
         * 显示缓存策略
         */
        public static final CacheControl EPHEMERAL = new CacheControl(Type.EPHEMERAL);

        /**
         * 缓存类型
         */
        public enum Type {

            @JsonProperty("ephemeral")
            EPHEMERAL

        }

    }

    // --- 创建方法 ---

    /**
     * 创建文本消息内容。
     *
     * @param text 文本内容
     * @return 文本消息内容
     */
    static TextContent text(String text) {
        return TextContent.newBuilder()
                .text(text)
                .build();
    }

    /**
     * 创建图像消息内容。
     *
     * @param image 图像{@code URI}
     * @return 图像消息内容
     */
    static ImageContent image(URI image) {
        return ImageContent.newBuilder()
                .image(image)
                .build();
    }

    /**
     * 创建音频消息内容。
     *
     * @param audio 音频{@code URI}
     * @return 音频消息内容
     */
    static AudioContent audio(URI audio) {
        return AudioContent.newBuilder()
                .audio(audio)
                .build();
    }

    /**
     * 创建视频消息内容。
     *
     * @param video 视频{@code URI}
     * @return 视频消息内容
     */
    static VideoContent video(URI video) {
        return VideoContent.newBuilder()
                .addResource(video)
                .build();
    }

    /**
     * 创建视频消息内容。
     *
     * @param images 视频截图{@code URI}列表
     * @return 视频消息内容
     */
    static VideoContent video(List<URI> images) {
        return VideoContent.newBuilder()
                .addResources(images)
                .build();
    }

}
