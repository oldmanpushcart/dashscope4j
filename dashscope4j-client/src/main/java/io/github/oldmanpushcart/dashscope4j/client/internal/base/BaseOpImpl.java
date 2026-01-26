package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.files.FilesOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.store.StoreOpImpl;

public class BaseOpImpl implements BaseOp {

    private final StoreOp storeOp;
    private final FilesOp filesOp;

    public BaseOpImpl(DashscopeClient client) {
        this.storeOp = new StoreOpImpl(client);
        this.filesOp = new FilesOpImpl(client);
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
