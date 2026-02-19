package io.github.oldmanpushcart.dashscope4j.client.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;

public interface TokenizerOp {

    Tokenizer remote(ChatModel model);

    Tokenizer local();

}
