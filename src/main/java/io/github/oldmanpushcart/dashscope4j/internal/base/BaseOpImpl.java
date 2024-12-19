package io.github.oldmanpushcart.dashscope4j.internal.base;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.TokenizerOpImpl;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BaseOpImpl implements BaseOp {

    private final ApiOp apiOp;

    @Override
    public TokenizerOp tokenize() {
        return new TokenizerOpImpl(apiOp);
    }

}
