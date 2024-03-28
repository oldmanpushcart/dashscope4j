package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm.FactorContentImpl;

import java.net.URI;

/**
 * 权重内容
 *
 * @param <T> 数据类型
 * @since 1.3.0
 */
public interface FactorContent<T> extends Content<T> {

    /**
     * @return 权重
     */
    float factor();

    /**
     * 图像
     *
     * @param factor 权重
     * @param uri    图像地址
     * @return 图像内容
     */
    static FactorContent<URI> ofImage(float factor, URI uri) {
        return new FactorContentImpl<>(factor, Type.IMAGE, uri);
    }

    /**
     * 图像
     *
     * @param uri 图像地址
     * @return 图像内容
     */
    static FactorContent<URI> ofImage(URI uri) {
        return new FactorContentImpl<>(Type.IMAGE, uri);
    }

    /**
     * 音频
     *
     * @param factor 权重
     * @param uri    音频地址
     * @return 音频内容
     */
    static FactorContent<URI> ofAudio(float factor, URI uri) {
        return new FactorContentImpl<>(factor, Type.AUDIO, uri);
    }

    /**
     * 音频
     *
     * @param uri 音频地址
     * @return 音频内容
     */
    static FactorContent<URI> ofAudio(URI uri) {
        return new FactorContentImpl<>(Type.AUDIO, uri);
    }

    /**
     * 文本
     *
     * @param factor 权重
     * @param text   文本
     * @return 文本内容
     */
    static FactorContent<String> ofText(float factor, String text) {
        return new FactorContentImpl<>(factor, Type.TEXT, text);
    }

    /**
     * 文本
     *
     * @param text 文本
     * @return 文本内容
     */
    static FactorContent<String> ofText(String text) {
        return new FactorContentImpl<>(Type.TEXT, text);
    }

}
