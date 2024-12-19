package io.github.oldmanpushcart.dashscope4j.base;

import io.github.oldmanpushcart.dashscope4j.base.tokenizer.TokenizerOp;

/**
 * 基础操作
 */
public interface BaseOp {

    /**
     * @return 标记操作
     */
    TokenizerOp tokenize();

}
