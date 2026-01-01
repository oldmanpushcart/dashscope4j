package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.store.StoreOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;

public class BaseOpImpl implements BaseOp {

    private final StoreOp storeOp;

    public BaseOpImpl(AsyncApi asyncApi) {
        this.storeOp = new StoreOpImpl(asyncApi);
    }

    @Override
    public StoreOp store() {
        return storeOp;
    }

}
