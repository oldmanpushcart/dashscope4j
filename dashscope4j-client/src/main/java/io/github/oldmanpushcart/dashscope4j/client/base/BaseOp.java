package io.github.oldmanpushcart.dashscope4j.client.base;

import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;

public interface BaseOp {

    StoreOp store();

    FilesOp files();

    ApiOp api();

}
