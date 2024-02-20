package io.github.ompc.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.ompc.dashscope4j.internal.util.LazyGet;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 内容
 *
 * @param type 类型
 * @param lazy 内容懒加载
 * @param <T>  内容类型
 */
public record Content<T>(Type type, LazyGet<CompletableFuture<T>> lazy) {

    /**
     * 加载内容
     *
     * @return 内容
     */
    public CompletableFuture<T> fetch() {
        return lazy.get();
    }

    /**
     * 获取内容
     *
     * @return 内容
     */
    public T data() {
        return fetch().join();
    }

    /**
     * 是否为文本
     *
     * @return TRUE | FALSE
     */
    public boolean isText() {
        return type == Type.TEXT;
    }

    /**
     * 是否为图像
     *
     * @return TRUE | FALSE
     */
    public boolean isImage() {
        return type == Type.IMAGE;
    }

    /**
     * 是否为音频
     *
     * @return TRUE | FALSE
     */
    public boolean isAudio() {
        return type == Type.AUDIO;
    }

    /**
     * 序列化为 {@code {"<TYPE>":"<DATA>"}} 格式
     *
     * @return Json Object Map
     */
    @JsonValue
    Map<Type, T> extract() {
        return Map.of(type, data());
    }

    /**
     * 反序列化
     *
     * @param map Json Object Map
     * @return 内容
     */
    @JsonCreator
    static Content<?> of(Map<String, String> map) {
        return map.entrySet().stream()
                .map(entry -> switch (entry.getKey()) {
                    case "text" -> ofText(entry.getValue());
                    case "image" -> ofImage(URI.create(entry.getValue()));
                    case "audio" -> ofAudio(URI.create(entry.getValue()));
                    default -> throw new IllegalArgumentException("Unknown content-type: %s".formatted(entry.getKey()));
                })
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
        return new Content<>(Type.TEXT, LazyGet.of(CompletableFuture.completedFuture(text)));
    }

    /**
     * 图像
     *
     * @param uri 图像地址
     * @return 图像内容
     */
    public static Content<URI> ofImage(URI uri) {
        return new Content<>(Type.IMAGE, LazyGet.of(CompletableFuture.completedFuture(uri)));
    }

    /**
     * 图像
     *
     * @param supplier 图像地址获取器
     * @return 图像内容
     */
    public static Content<URI> ofImage(Supplier<CompletableFuture<URI>> supplier) {
        return new Content<>(Type.IMAGE, LazyGet.of(supplier));
    }

    /**
     * 音频
     *
     * @param uri 音频地址
     * @return 音频内容
     */
    public static Content<URI> ofAudio(URI uri) {
        return new Content<>(Type.AUDIO, LazyGet.of(CompletableFuture.completedFuture(uri)));
    }

    /**
     * 音频
     *
     * @param supplier 音频地址获取器
     * @return 音频内容
     */
    public static Content<URI> ofAudio(Supplier<CompletableFuture<URI>> supplier) {
        return new Content<>(Type.AUDIO, LazyGet.of(supplier));
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
        AUDIO

    }

}
