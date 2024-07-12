package io.github.oldmanpushcart.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;

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

}
