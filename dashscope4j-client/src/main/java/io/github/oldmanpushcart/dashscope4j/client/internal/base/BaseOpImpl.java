package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.files.FilesOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.store.StoreOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.TokenizerOpImpl;

public class BaseOpImpl implements BaseOp {

    private final StoreOp storeOp;
    private final FilesOp filesOp;
    private final TokenizerOp tokenizerOp;

    public BaseOpImpl(DashscopeClient client) {
        this.storeOp = new StoreOpImpl(client);
        this.filesOp = new FilesOpImpl(client);
        this.tokenizerOp = new TokenizerOpImpl(client);
    }

    @Override
    public StoreOp store() {
        return storeOp;
    }

    @Override
    public FilesOp files() {
        return filesOp;
    }

    @Override
    public TokenizerOp tokenizer() {
        return tokenizerOp;
    }

}
