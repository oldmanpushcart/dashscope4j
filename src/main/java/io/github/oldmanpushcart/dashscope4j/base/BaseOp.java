package io.github.oldmanpushcart.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;

/**
 * 基础操作
 */
public interface BaseOp {

    /**
     * @return http客户端
     * @since 3.1.0
     */
    okhttp3.OkHttpClient http();

    /**
     * @return 标记操作
     */
    TokenizerOp tokenize();

    /**
     * @return 存储操作
     */
    StoreOp store();

    /**
     * @return 文件操作
     */
    FilesOp files();

}
