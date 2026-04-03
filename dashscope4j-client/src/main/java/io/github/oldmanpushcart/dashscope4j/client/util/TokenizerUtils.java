package io.github.oldmanpushcart.dashscope4j.client.util;

import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.local.LocalTokenizer;

import java.util.Arrays;

/**
 * 词元工具
 */
public class TokenizerUtils {

    private static final LocalTokenizer tokenizer = new LocalTokenizer();

    /**
     * 估算 tokens 数量
     *
     * @param text 文本
     * @return tokens 数量
     */
    public static int estimateTokens(String text) {
        if (null == text) {
            return 0;
        }
        return tokenizer.encode(text)
                .toCompletableFuture()
                .join()
                .size();
    }

    /**
     * 估算 tokens 数量
     *
     * @param texts 文本
     * @return tokens 数量
     */
    public static int estimateTokens(String... texts) {
        if (null == texts) {
            return 0;
        }
        int tokens = 0;
        for (String text : texts) {
            tokens += estimateTokens(text);
        }
        return tokens;
    }

}
