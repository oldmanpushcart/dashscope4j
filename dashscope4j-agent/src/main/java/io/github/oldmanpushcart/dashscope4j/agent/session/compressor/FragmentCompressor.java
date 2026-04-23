package io.github.oldmanpushcart.dashscope4j.agent.session.compressor;

import io.github.oldmanpushcart.dashscope4j.agent.session.store.FragmentStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 片段压缩器接口
 * <p>
 * 定义会话历史压缩的标准行为，负责将超出限制的片段列表压缩为摘要和保留片段。
 * </p>
 */
public interface FragmentCompressor {

    /**
     * 执行压缩操作
     *
     * @param fragments    当前所有片段列表（按时间倒序，最新的在前）
     * @param retainTokens 需要保留的 Token 数（较新的片段）
     * @return 压缩结果的 CompletionStage
     */
    CompletionStage<Result> compress(List<Fragment> fragments, int retainTokens);

    /**
     * 压缩结果记录
     * <p>
     * 封装压缩操作的结果，包含保留的新片段和生成的历史摘要。
     * </p>
     *
     * @param retained 保留的片段列表（较新的片段，未被压缩）
     * @param summary  生成的历史摘要消息
     */
    record Result(List<Fragment> retained, Message summary) {

    }

}
