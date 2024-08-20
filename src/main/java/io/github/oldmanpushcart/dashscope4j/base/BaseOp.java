package io.github.oldmanpushcart.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;

/**
 * 辅助功能操作
 */
public interface BaseOp {

    /**
     * @return 存储操作
     */
    StoreOp store();

    /**
     * @return 文件操作
     */
    FilesOp files();

    /**
     * @return 标记操作
     * @since 2.1.1
     */
    TokenizerOp tokenize();

}
