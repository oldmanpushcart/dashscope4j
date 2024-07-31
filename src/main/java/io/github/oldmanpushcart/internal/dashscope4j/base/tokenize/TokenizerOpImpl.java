package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize;

import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.local.LocalTokenizer;
import io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.remote.RemoteTokenizer;

public class TokenizerOpImpl implements TokenizerOp {

    private final ApiExecutor executor;

    public TokenizerOpImpl(ApiExecutor executor) {
        this.executor = executor;
    }


    @Override
    public Tokenizer remote(ChatModel model) {
        return new RemoteTokenizer(executor, model);
    }

    @Override
    public Tokenizer local() {
        return new LocalTokenizer();
    }

}
