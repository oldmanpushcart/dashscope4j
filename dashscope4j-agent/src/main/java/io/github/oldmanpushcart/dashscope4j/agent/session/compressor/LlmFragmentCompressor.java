package io.github.oldmanpushcart.dashscope4j.agent.session.compressor;

import io.github.oldmanpushcart.dashscope4j.agent.session.store.FragmentStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 基于 LLM 摘要的片段压缩器
 * <p>
 * 使用聊天模型生成历史对话摘要，保留最近的上下文片段。
 * </p>
 */
public class LlmFragmentCompressor implements FragmentCompressor {

    private final DashscopeClient client;
    private final ChatModel model;

    /**
     * 构造 LLM 摘要压缩器
     *
     * @param client DashScope 客户端
     * @param model  聊天模型（用于生成摘要）
     */
    public LlmFragmentCompressor(DashscopeClient client, ChatModel model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public CompletionStage<Result> compress(List<Fragment> fragments, int retainTokens) {

        /*
         * 完整的片段快照
         * 之所以要进行快照，是因为接下来要对这个集合进行修改操作
         */
        final var fragmentCopy = new ArrayList<>(fragments);

        // 执行紧凑化，分离出需要压缩的旧片段
        final var evictions = compact(retainTokens, fragmentCopy);

        // 按时间正序排列被驱逐的片段（从旧到新），拼接为历史对话
        final var history = evictions.stream()
                .sorted((o1, o2) -> Long.compare(o2.fragmentId(), o1.fragmentId()))
                .flatMap(f -> f.messages().stream())
                .toList();

        // 构建摘要生成请求
        final var request = AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .addMessages(history)
                        .addMessage(Message.user("""
                                你是一个专业的对话摘要助手。请总结对话历史，生成一个简洁但全面的摘要。摘要应该：
                                1. 保留关键信息和重要细节
                                2. 忽略寒暄和无关内容
                                3. 用简洁的语言总结主要话题和结论
                                4. 保持在 200-500 字以内
                                5. 只输出摘要内容，不要添加任何解释或额外说明
                                """))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message())
                .thenApply(message -> new Result(fragmentCopy, message));
    }

    /**
     * 紧凑化片段列表
     * <p>
     * 从片段列表中移除超出保留 Token 数的旧片段，保留最近的片段。
     * 片段列表按时间倒序排列（最新的在前），因此从尾部开始移除旧片段。
     * </p>
     *
     * @param retainTokens 保留的 Token 数
     * @param fragments    片段列表（会被修改，移除旧片段）
     * @return 被移除的片段列表（按时间正序排列，从旧到新）
     */
    private static List<Fragment> compact(int retainTokens, List<Fragment> fragments) {
        final var evictions = new ArrayList<Fragment>();
        int tokens = 0;
        boolean evictFlag = false;
        final var removeIt = fragments.iterator();
        while (removeIt.hasNext()) {
            final var fragment = removeIt.next();
            // 如果累计 Token 数未超过保留限制，则保留该片段
            if (!evictFlag && !(evictFlag = !(tokens + fragment.tokens() <= retainTokens))) {
                tokens += fragment.tokens();
            } else {
                // 超出限制，移除该片段并记录到驱逐列表
                removeIt.remove();
                evictions.add(0, fragment);  // 插入到头部，保持从旧到新的顺序
            }
        }
        return evictions;
    }

}
