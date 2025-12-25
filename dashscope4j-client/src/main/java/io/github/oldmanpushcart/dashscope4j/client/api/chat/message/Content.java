package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 内容
 *
 * @param <T> 数据类型
 */
@JsonSerialize(using = Content.ContentJsonSerializer.class)
@JsonDeserialize(using = Content.ContentJsonDeserializer.class)
public sealed abstract class Content<T> permits Content.Text, Content.Media {

    private final Type type;
    private final T data;
    private final Parameters parameters;

    private Content(Type type, T data, Parameters parameters) {
        this.type = type;
        this.data = data;
        this.parameters = new Parameters()
                .merge(parameters)
                .unmodifiable();
    }

    private Content(Type type, T data) {
        this(type, data, new Parameters());
    }

    /**
     * @return 类型
     */
    public Type type() {
        return type;
    }

    /**
     * @return 数据
     */
    public T data() {
        return data;
    }

    /**
     * @return 参数项
     */
    public Parameters parameters() {
        return parameters;
    }

    static class ContentJsonSerializer extends JsonSerializer<Content<?>> {

        @Override
        public void serialize(Content<?> content, JsonGenerator gen, SerializerProvider provider) throws IOException {
            final var map = new HashMap<>();
            map.put(content.type, content.data);
            gen.writeObject(map);
        }

    }

    static class ContentJsonDeserializer extends JsonDeserializer<Content<?>> {

        @Override
        public Content<?> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
            final var entry = parser.getCodec().readValue(parser, new TypeReference<Map.Entry<Type, String>>() {
            });
            return switch (entry.getKey()) {
                case TEXT -> new Text(entry.getValue());
                case IMAGE, AUDIO, VIDEO, FILE -> new Media(entry.getKey(), URI.create(entry.getValue()));
            };
        }

    }


    /**
     * 创建文本内容
     *
     * @param text 文本
     * @return 内容
     */
    public static Text ofText(String text) {
        return new Text(text);
    }

    /**
     * 创建媒体内容：图片
     *
     * @param image 图片
     * @return 内容
     */
    public static Media ofImage(URI image) {
        return new Media(Type.IMAGE, image);
    }


    /**
     * 创建媒体内容：音频
     *
     * @param audio 音频
     * @return 内容
     */
    public static Media ofAudio(URI audio) {
        return new Media(Type.AUDIO, audio);
    }

    /**
     * 创建媒体内容：视频
     *
     * @param video 视频
     * @return 内容
     */
    public static Media ofVideo(URI video) {
        return new Media(Type.VIDEO, video);
    }

    /**
     * 创建媒体内容：文件
     *
     * @param file 文件
     * @return 内容
     */
    public static Media ofFile(URI file) {
        return new Media(Type.FILE, file);
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


    /**
     * 文本内容
     */
    public static final class Text extends Content<String> {


        private Text(String data, Parameters parameters) {
            super(Type.TEXT, data, parameters);
        }

        private Text(String data) {
            super(Type.TEXT, data);
        }

    }


    /**
     * 媒体内容
     */
    public static final class Media extends Content<URI> {

        private Media(Type type, URI data) {
            super(type, data);
        }

        private Media(Type type, URI data, Parameters parameters) {
            super(type, data, parameters);
        }

    }

}
