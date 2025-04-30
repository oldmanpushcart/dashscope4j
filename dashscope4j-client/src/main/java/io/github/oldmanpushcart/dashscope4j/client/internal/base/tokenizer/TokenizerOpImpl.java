package io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.local.LocalTokenizer;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.remote.RemoteTokenizer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenizerOpImpl implements TokenizerOp {

    private final ApiOp apiOp;

    @Override
    public Tokenizer remote(ChatModel model) {
        return new RemoteTokenizer(apiOp, model);
    }

    @Override
    public Tokenizer local() {
        return new LocalTokenizer();
    }

}
