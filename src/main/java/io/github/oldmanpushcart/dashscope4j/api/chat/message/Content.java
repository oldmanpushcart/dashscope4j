package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.*;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息内容
 * <p>
 * 这个类的数据和类型必须严格控制，所以不想让任何人可以自定义构造，减少后续处理成本。
 * </p>
 *
 * @param <T> 数据类型
 */
@Value
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class Content<T> {

    /**
     * 类型
     */
    Type type;

    /**
     * 数据
     */
    T data;

    /**
     * 创建新数据
     *
     * @param data 数据
     * @param <U>  数据类型
     * @return 新内容
     */
    public <U> Content<U> newData(U data) {
        return new Content<>(type, data);
    }

    /**
     * 序列化为 {@code {"<TYPE>":"<DATA>"}} 格式
     *
     * @return Json Object Map
     */
    @JsonValue
    ObjectMap extract() {
        return new ObjectMap() {{
            put(type, data);
        }};
    }

    /**
     * 反序列化
     *
     * @param map Json Object Map
     * @return 内容
     */
    @JsonCreator
    static Content<?> of(Map<Type, Object> map) {
        return map.entrySet().stream()
                .map(entry -> {

                    final Type type = entry.getKey();
                    final Object data = entry.getValue();

                    // 文本内容为字符串
                    if (type == Type.TEXT) {
                        return new Content<>(type, data.toString());
                    }

                    // 多模态的数据内容为URI
                    if (type == Type.IMAGE || type == Type.AUDIO || type == Type.FILE || type == Type.VIDEO) {
                        if (data instanceof Collection) {
                            final Collection<?> collection = (Collection<?>) data;
                            final Collection<URI> URIs = collection
                                    .stream()
                                    .map(item -> URI.create(item.toString()))
                                    .collect(Collectors.toList());
                            return new Content<>(type, URIs);
                        } else {
                            return new Content<>(type, URI.create(data.toString()));
                        }
                    }

                    // 其他类型不做反序列化支持
                    return null;

                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }


    /**
     * 文本
     *
     * @param text 文本
     * @return 文本内容
     */
    public static Content<String> ofText(String text) {
        return new Content<>(Type.TEXT, text);
    }

    /**
     * 图像
     *
     * @param resource 图像资源标识
     * @return 图像内容
     */
    public static Content<URI> ofImage(URI resource) {
        return new Content<>(Type.IMAGE, resource);
    }

    /**
     * 音频
     *
     * @param resource 音频资源标识
     * @return 音频内容
     */
    public static Content<URI> ofAudio(URI resource) {
        return new Content<>(Type.AUDIO, resource);
    }

    /**
     * 视频
     *
     * @param resource 视频资源标识
     * @return 视频内容
     */
    public static Content<URI> ofVideo(URI resource) {
        return new Content<>(Type.VIDEO, resource);
    }

    /**
     * 文件
     *
     * @param resource 文件资源标识
     * @return 文件内容
     */
    public static Content<URI> ofFile(URI resource) {
        return new Content<>(Type.FILE, resource);
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
     */
    public static Content<Collection<URI>> ofVideo(Collection<URI> resources) {
        return new Content<>(Type.VIDEO, resources);
    }

    /**
     * 类型
     */
    public enum Type {

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
