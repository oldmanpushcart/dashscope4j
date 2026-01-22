package io.github.oldmanpushcart.dashscope4j.client.internal.base;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.ApiOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.files.FilesOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.store.StoreOpImpl;

import java.net.http.HttpClient;

public class BaseOpImpl implements BaseOp {

    private final StoreOp storeOp;
    private final FilesOp filesOp;
    private final ApiOp apiOp;

    public BaseOpImpl(DashscopeClient client, String host, String ak, HttpClient http) {
        this.storeOp = new StoreOpImpl(client);
        this.filesOp = new FilesOpImpl(client);
        this.apiOp = new ApiOpImpl(client, host, ak, http);
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
