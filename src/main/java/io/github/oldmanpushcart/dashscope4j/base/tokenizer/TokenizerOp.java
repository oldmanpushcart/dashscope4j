package io.github.oldmanpushcart.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;

/**
 * 标记操作
 */
public interface TokenizerOp {

    /**
     * @param model 对话模型
     * @return 远程标记
     */
    Tokenizer remote(ChatModel model);

    /**
     * @return 本地标记
     */
    Tokenizer local();

}
