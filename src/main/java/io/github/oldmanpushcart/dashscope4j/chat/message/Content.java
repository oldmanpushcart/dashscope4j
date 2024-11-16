package io.github.oldmanpushcart.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ContentImpl;

import java.net.URI;
import java.util.Collection;

/**
 * 内容
 *
 * @param <T> 内容类型
 */
public sealed interface Content<T> permits ContentImpl {

    /**
     * @return 权重
     */
    float factor();

    /**
     * @return 类型
     */
    Type type();

    /**
     * @return 数据
     */
    T data();

    /**
     * 创建新数据
     *
     * @param data 数据
     * @param <U>  数据类型
     * @return 新内容
     */
    <U> Content<U> newData(U data);

    /**
     * 创建新数据
     *
     * @param factor 权重
     * @param data   数据
     * @param <U>    数据类型
     * @return 新内容
     */
    <U> Content<U> newFactorData(float factor, U data);

    /**
     * 创建内容
     *
     * @param type 类型
     * @param data 数据
     * @param <T>  数据类型
     * @return 内容
     */
    static <T> Content<T> of(Type type, T data) {
        return new ContentImpl<>(type, data);
    }

    /**
     * 创建内容
     *
     * @param factor 权重
     * @param type   类型
     * @param data   数据
     * @param <T>    数据类型
     * @return 内容
     */
    static <T> Content<T> of(float factor, Type type, T data) {
        return new ContentImpl<>(factor, type, data);
    }

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
     * @param resource 图像资源标识
     * @return 图像内容
     */
    static Content<URI> ofImage(URI resource) {
        return new ContentImpl<>(Type.IMAGE, resource);
    }

    /**
     * 音频
     *
     * @param resource 音频资源标识
     * @return 音频内容
     */
    static Content<URI> ofAudio(URI resource) {
        return new ContentImpl<>(Type.AUDIO, resource);
    }

    /**
     * 视频
     *
     * @param resource 视频资源标识
     * @return 视频内容
     */
    static Content<URI> ofVideo(URI resource) {
        return new ContentImpl<>(Type.VIDEO, resource);
    }

    /**
     * 视频
     * <p>
     * 通义千问可以将多个图片资源标识合并为一个视频内容,
     * 最少传入4张图片，最多可传入768张图片。
     * </p>
     *
     * @param resources 图片资源标识集合
     * @return 视频内容
     * @since 2.2.2
     */
    static Content<Collection<URI>> ofVideo(Collection<URI> resources) {
        return new ContentImpl<>(Type.VIDEO, resources);
    }

    /**
     * 文件
     *
     * @param resource 文件资源标识
     * @return 文件内容
     */
    static Content<URI> ofFile(URI resource) {
        return new ContentImpl<>(Type.FILE, resource);
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
