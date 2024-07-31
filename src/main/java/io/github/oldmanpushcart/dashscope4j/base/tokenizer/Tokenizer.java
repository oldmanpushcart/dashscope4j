package io.github.oldmanpushcart.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 标记器
 *
 * @since 2.1.1
 */
public interface Tokenizer {

    /**
     * 编码文本为标记对列表
     *
     * @param text 文本
     * @return 标记对列表
     */
    CompletableFuture<List<Map.Entry<Integer, String>>> encode(String text);

    /**
     * 编码消息列表为标记对列表
     *
     * @param messages 消息列表
     * @return 标记对列表
     */
    CompletableFuture<List<Map.Entry<Integer, String>>> encode(List<Message> messages);

    /**
     * 解码标记列表为文本
     *
     * @param tokens 标记列表
     * @return 文本
     */
    CompletableFuture<String> decode(List<Integer> tokens);

    /**
     * @return 是否支持解码
     */
    boolean isDecodeSupported();

}
