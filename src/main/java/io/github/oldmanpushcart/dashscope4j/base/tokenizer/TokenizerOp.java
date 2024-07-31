package io.github.oldmanpushcart.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;

/**
 * 标记操作
 *
 * @since 2.1.1
 */
public interface TokenizerOp {

    /**
     * 远程标记
     *
     * @param model 对话模型
     * @return 标记器
     */
    Tokenizer remote(ChatModel model);

    /**
     * 本地标记
     *
     * @return 标记器
     */
    Tokenizer local();

}
