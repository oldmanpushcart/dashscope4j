package io.github.oldmanpushcart.internal.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.tokenizer.local.LocalTokenizer;

public class TokenizerOpImpl implements TokenizerOp {

    @Override
    public Tokenizer remote(ChatModel model) {
        return null;
    }

    @Override
    public Tokenizer local() {
        return new LocalTokenizer();
    }

}
