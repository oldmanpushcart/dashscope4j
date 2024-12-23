package io.github.oldmanpushcart.dashscope4j.internal.base;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.internal.base.store.StoreOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.TokenizerOpImpl;

public class BaseOpImpl implements BaseOp {

    private final TokenizerOp tokenizerOp;
    private final StoreOp storeOp;

    public BaseOpImpl(Cache cache, ApiOp apiOp) {
        this.tokenizerOp = new TokenizerOpImpl(apiOp);
        this.storeOp = new StoreOpImpl(cache, apiOp);
    }

    @Override
    public TokenizerOp tokenize() {
        return tokenizerOp;
    }

    @Override
    public StoreOp store() {
        return storeOp;
    }

}
