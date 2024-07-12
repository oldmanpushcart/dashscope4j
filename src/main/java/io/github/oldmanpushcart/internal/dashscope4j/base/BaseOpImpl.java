package io.github.oldmanpushcart.internal.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.files.FilesOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.store.StoreOpImpl;

public class BaseOpImpl implements BaseOp {

    private final FilesOp filesOp;
    private final StoreOp storeOp;

    public BaseOpImpl(ApiExecutor apiExecutor, CacheFactory cacheFactory) {
        this.filesOp = new FilesOpImpl(apiExecutor, cacheFactory);
        this.storeOp = new StoreOpImpl(apiExecutor, cacheFactory);
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
