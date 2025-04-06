package io.github.oldmanpushcart.dashscope4j.internal.base;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;
import io.github.oldmanpushcart.dashscope4j.internal.base.files.FilesOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.base.store.StoreOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.TokenizerOpImpl;
import okhttp3.OkHttpClient;

public class BaseOpImpl implements BaseOp {

    private final okhttp3.OkHttpClient http;
    private final TokenizerOp tokenizerOp;
    private final StoreOp storeOp;
    private final FilesOp filesOp;

    public BaseOpImpl(okhttp3.OkHttpClient http, ApiOp apiOp) {
        this.http = http;
        this.tokenizerOp = new TokenizerOpImpl(apiOp);
        this.storeOp = new StoreOpImpl(apiOp);
        this.filesOp = new FilesOpImpl(apiOp);
    }

    @Override
    public OkHttpClient http() {
        return http;
    }

    @Override
    public TokenizerOp tokenize() {
        return tokenizerOp;
    }

    @Override
    public StoreOp store() {
        return storeOp;
    }

    @Override
    public FilesOp files() {
        return filesOp;
    }

}
