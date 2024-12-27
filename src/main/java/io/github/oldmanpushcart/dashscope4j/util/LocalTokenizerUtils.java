package io.github.oldmanpushcart.dashscope4j.util;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.local.LocalTokenizer;

import java.util.List;
import java.util.Map;

/**
 * 本地分词器工具类
 */
public class LocalTokenizerUtils {

    private static final Tokenizer tokenizer = new LocalTokenizer();

    /**
     * 编码
     *
     * @param text 文本
     * @return 编码结果
     */
    public static List<Map.Entry<Integer, String>> encode(String text) {
        return tokenizer.encode(text)
                .toCompletableFuture()
                .join();
    }

    /**
     * 编码
     *
     * @param messages 消息列表
     * @return 编码结果
     */
    public static List<Map.Entry<Integer, String>> encode(List<Message> messages) {
        return tokenizer.encode(messages)
                .toCompletableFuture()
                .join();
    }

    /**
     * 解码
     *
     * @param tokens 编码结果
     * @return 文本
     */
    public static String decode(List<Integer> tokens) {
        return tokenizer.decode(tokens)
                .toCompletableFuture()
                .join();
    }

}
