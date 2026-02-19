package io.github.oldmanpushcart.dashscope4j.client.base;

import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.TokenizerOp;

public interface BaseOp {

    StoreOp store();

    FilesOp files();

    TokenizerOp tokenizer();

}
