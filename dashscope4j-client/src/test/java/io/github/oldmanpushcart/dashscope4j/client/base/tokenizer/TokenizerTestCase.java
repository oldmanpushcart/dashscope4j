package io.github.oldmanpushcart.dashscope4j.client.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TokenizerTestCase implements LoadingEnv {

    /**
     * 远程已经失效而且也不推荐使用
     */
    @Disabled
    @Test
    public void test$tokenizer$remote() {

        final var tokens = client.base().tokenizer().remote(ChatModel.QWEN_FLASH)
                .encode("我走在长街中，听戏子唱京城。")
                .toCompletableFuture()
                .join();

        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());

    }

    @Test
    public void test$tokenizer$local() {

        final var tokens = client.base().tokenizer().local()
                .encode("我走在长街中，听戏子唱京城。")
                .toCompletableFuture()
                .join();

        System.out.println(tokens);
        Assertions.assertNotNull(tokens);
        Assertions.assertFalse(tokens.isEmpty());

    }

}
