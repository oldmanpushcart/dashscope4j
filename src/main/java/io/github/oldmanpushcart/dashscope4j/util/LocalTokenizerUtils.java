package io.github.oldmanpushcart.dashscope4j.util;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.local.LocalTokenizer;

import java.util.List;
import java.util.Map;

public class LocalTokenizerUtils {

    private static final Tokenizer tokenizer = new LocalTokenizer();

    public static List<Map.Entry<Integer, String>> encode(String text) {
        return tokenizer.encode(text)
                .toCompletableFuture()
                .join();
    }

    public static List<Map.Entry<Integer, String>> encode(List<Message> messages) {
        return tokenizer.encode(messages)
                .toCompletableFuture()
                .join();
    }

    public static String decode(List<Integer> tokens) {
        return tokenizer.decode(tokens)
                .toCompletableFuture()
                .join();
    }

}
