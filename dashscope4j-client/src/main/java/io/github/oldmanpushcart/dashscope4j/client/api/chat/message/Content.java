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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

            final var pojo = new HashMap<>();

            // 写入内容
            if (content instanceof Text) {
                pojo.put(content.type(), content.data());
            } else if (content instanceof Media media) {
                final var resources = media.data();
                if (resources.isEmpty()) {
                    pojo.put(content.type(), null);
                } else if (resources.size() == 1) {
                    pojo.put(content.type(), media.first());
                } else {
                    pojo.put(content.type(), resources);
                }
            } else {
                throw new IllegalArgumentException("Unsupported content class: " + content.getClass().getSimpleName());
            }

            // 写入参数
            if (null != content.parameters() && !content.parameters().isEmpty()) {
                content.parameters().forEach(pojo::put);
            }

            gen.writeObject(pojo);
        }

    }

    static class ContentJsonDeserializer extends JsonDeserializer<Content<?>> {

        @Override
        public Content<?> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JacksonException {
            final var entry = parser.getCodec().readValue(parser, new TypeReference<Map.Entry<Type, Object>>() {
            });
            final var key = entry.getKey();
            final var value = entry.getValue();
            return switch (key) {
                case TEXT -> new Text(String.valueOf(value));
                case IMAGE, AUDIO, VIDEO, FILE -> {
                    if (value instanceof Collection<?> collection) {
                        final var uris = collection.stream()
                                .map(Object::toString)
                                .map(URI::create)
                                .toList();
                        yield new Media(key, uris);
                    } else {
                        yield new Media(key, List.of(URI.create(String.valueOf(value))));
                    }
                }
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
        return new Media(Type.IMAGE, List.of(image));
    }


    /**
     * 创建媒体内容：音频
     *
     * @param audio 音频
     * @return 内容
     */
    public static Media ofAudio(URI audio) {
        return new Media(Type.AUDIO, List.of(audio));
    }

    public static Media ofAudio(URI audio, Parameters parameters) {
        return new Media(Type.AUDIO, List.of(audio), parameters);
    }

    /**
     * 创建媒体内容：视频
     *
     * @param video 视频
     * @return 内容
     */
    public static Media ofVideo(URI video) {
        return new Media(Type.VIDEO, List.of(video));
    }

    public static Media ofVideo(URI video, Parameters parameters) {
        return new Media(Type.VIDEO, List.of(video), parameters);
    }

    /**
     * 创建媒体内容：视频
     *
     * @param images 视频图片集
     * @return 内容
     */
    public static Media ofVideo(List<URI> images) {
        return new Media(Type.VIDEO, images);
    }

    public static Media ofVideo(List<URI> images, Parameters parameters) {
        return new Media(Type.VIDEO, images, parameters);
    }

    /**
     * 创建媒体内容：文件
     *
     * @param file 文件
     * @return 内容
     */
    public static Media ofFile(URI file) {
        return new Media(Type.FILE, List.of(file));
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
    public static final class Media extends Content<List<URI>> {

        private Media(Type type, List<URI> data) {
            super(type, data);
        }

        private Media(Type type, List<URI> data, Parameters parameters) {
            super(type, data, parameters);
        }

        public Media changeData(List<URI> data) {
            return new Media(type(), data, parameters());
        }

        public URI first() {
            return (null != data() && !data().isEmpty())
                    ? data().get(0)
                    : null;
        }

    }

}
