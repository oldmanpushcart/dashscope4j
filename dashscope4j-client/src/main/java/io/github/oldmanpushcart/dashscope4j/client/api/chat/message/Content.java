package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

/**
 * 多模态内容
 * <p>
 * 只有两个实现类
 * <li>{@link TextContent} : 负责处理文本内容</li>
 * <li>{@link MediaContent} : 负责处理多媒体内容</li>
 * </p>
 *
 * <p>
 * 多模态 = 纯文本 + 多媒体
 * </p>
 *
 * @param <T> 类型
 */
@Data
@Accessors(fluent = true)
public abstract class Content<T> {

    private final Type type;
    private final T data;

    private Content(Type type, T data) {
        this.type = type;
        this.data = data;
    }

    /**
     * 更改数据
     *
     * @param data 新数据
     * @return 修改后的内容
     */
    abstract public Content<T> changeData(T data);

    /**
     * 构造文本内容
     *
     * @param text 文本
     * @return 文本内容
     */
    public static TextContent ofText(String text) {
        return new TextContent(text);
    }

    /**
     * 构造图像内容
     *
     * @param image 图片URI
     * @return 图像内容
     */
    public static MediaContent ofImage(URI image) {
        return new MediaContent(Type.IMAGE, image);
    }

    /**
     * 构造音频内容
     *
     * @param audio 音频URI
     * @return 音频内容
     */
    public static MediaContent ofAudio(URI audio) {
        return new MediaContent(Type.AUDIO, audio);
    }

    /**
     * 构造视频内容
     *
     * @param video 视频URI
     * @return 视频内容
     */
    public static MediaContent ofVideo(URI video) {
        return new MediaContent(Type.VIDEO, video);
    }

    /**
     * 构造文件内容
     *
     * @param file 文件URI
     * @return 文件内容
     */
    public static MediaContent ofFile(URI file) {
        return new MediaContent(Type.FILE, file);
    }

    /**
     * 类型
     */
    public enum Type {

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


    // 序列化
    @JsonValue
    Map.Entry<Type, ?> extract() {
        return new SimpleEntry<>(type, data);
    }

    // 反序列化
    @JsonCreator
    static Content<?> of(Map.Entry<Type, String> entry) {
        switch (entry.getKey()) {
            case TEXT:
                return new TextContent(entry.getValue());
            case IMAGE:
            case AUDIO:
            case VIDEO:
            case FILE:
                return new MediaContent(entry.getKey(), URI.create(entry.getValue()));
            default:
                throw new IllegalArgumentException("Unsupported type: " + entry.getKey());
        }
    }

    /**
     * 文本内容
     */
    @Getter
    @Accessors(fluent = true)
    public static class TextContent extends Content<String> {

        private TextContent(String data) {
            super(Type.TEXT, data);
        }

        @Override
        public TextContent changeData(String data) {
            return new TextContent(data);
        }

    }

    /**
     * 多媒体内容
     */
    @Getter
    @Accessors(fluent = true)
    public static class MediaContent extends Content<URI> {

        private MediaContent(Type type, URI data) {
            super(type, data);
        }

        @Override
        public MediaContent changeData(URI data) {
            return new MediaContent(type(), data);
        }

    }

}
