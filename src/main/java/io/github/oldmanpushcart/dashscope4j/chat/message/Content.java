package io.github.oldmanpushcart.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ContentImpl;

import java.net.URI;

/**
 * 内容
 *
 * @param <T> 内容类型
 */
public interface Content<T> {

    /**
     * 类型
     *
     * @return 类型
     */
    Type type();

    /**
     * 数据
     *
     * @return 数据
     */
    T data();

    /**
     * 文本
     *
     * @param text 文本
     * @return 文本内容
     */
    static Content<String> ofText(String text) {
        return new ContentImpl<>(Type.TEXT, text);
    }

    /**
     * 图像
     *
     * @param uri 图像地址
     * @return 图像内容
     */
    static Content<URI> ofImage(URI uri) {
        return new ContentImpl<>(Type.IMAGE, uri);
    }

    /**
     * 音频
     *
     * @param uri 音频地址
     * @return 音频内容
     */
    static Content<URI> ofAudio(URI uri) {
        return new ContentImpl<>(Type.AUDIO, uri);
    }

    /**
     * 视频
     *
     * @param uri 视频地址
     * @return 视频内容
     */
    static Content<URI> ofVideo(URI uri) {
        return new ContentImpl<>(Type.VIDEO, uri);
    }

    /**
     * 文件
     *
     * @param uri 文件地址
     * @return 文件内容
     */
    static Content<URI> ofFile(URI uri) {
        return new ContentImpl<>(Type.FILE, uri);
    }

    /**
     * 类型
     */
    enum Type {

        /**
         * 文本
         */
        @JsonProperty("text")
        TEXT,

        /**
         * 图像
         */
        @JsonProperty("image")
        IMAGE,

        /**
         * 音频
         */
        @JsonProperty("audio")
        AUDIO,

        /**
         * 视频
         */
        @JsonProperty("video")
        VIDEO,

        /**
         * 文件
         */
        @JsonProperty("file")
        FILE

    }

}
