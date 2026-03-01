package io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.local.LocalTokenizer;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.remote.RemoteTokenizer;

public class TokenizerOpImpl implements TokenizerOp {

    private final DashscopeClient client;

    public TokenizerOpImpl(DashscopeClient client) {
        this.client = client;
    }

    @Override
    public Tokenizer remote(ChatModel model) {
        return new RemoteTokenizer(client, model);
    }

    @Override
    public Tokenizer local() {
        return new LocalTokenizer();
    }

}
