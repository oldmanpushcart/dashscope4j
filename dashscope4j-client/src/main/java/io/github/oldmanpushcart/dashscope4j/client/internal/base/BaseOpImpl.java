package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.ApiOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.files.FilesOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.store.StoreOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;

public class BaseOpImpl implements BaseOp {

    private final StoreOp storeOp;
    private final FilesOp filesOp;
    private final ApiOp apiOp;

    public BaseOpImpl(AsyncApi asyncApi, FlowApi flowApi, TaskApi taskApi, ExchangeApi exchangeApi) {
        this.storeOp = new StoreOpImpl(asyncApi);
        this.filesOp = new FilesOpImpl(asyncApi);
        this.apiOp = new ApiOpImpl(asyncApi, flowApi, taskApi, exchangeApi);
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
    public ApiOp api() {
        return apiOp;
    }

}
