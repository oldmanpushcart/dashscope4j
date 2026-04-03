package io.github.oldmanpushcart.dashscope4j.agent.memory.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.concurrent.CompletionStage;

/**
 * 内存存储
 * <p>
 * 用于存储和检索多个内存片段。
 * </p>
 */
public interface MemoryStore extends AutoCloseable {

    /**
     * 流式获取内存片段
     *
     * @param sessionId 会话 ID
     * @param offset    偏移量
     * @return 内存片段流
     */
    Publisher<Fragment> flow(String sessionId, long offset);

    /**
     * 插入内存片段
     *
     * @param sessionId SESSION ID
     * @param inbound   用户输入
     * @param outbound  助手输出
     * @return 内存片段ID
     */
    CompletionStage<Long> upsert(String sessionId, Message inbound, Message outbound);

    /**
     * 删除内存片段
     *
     * @param fragmentId 内存片段ID
     * @return 删除结果
     */
    CompletionStage<Void> remove(long fragmentId);

    /**
     * 记忆片段
     * <p>
     * 用于存储和检索单个内存片段。
     * </p>
     *
     * @param fragmentId ID
     * @param sessionId  SESSION ID
     * @param inbound    用户输入
     * @param outbound   助手输出
     * @param tokens     TOKENS
     * @param createdAt  创建时间
     */
    record Fragment(

            @JsonProperty("fragment_id")
            long fragmentId,

            @JsonProperty("session_id")
            String sessionId,

            @JsonProperty("inbound")
            Message inbound,

            @JsonProperty("outbound")
            Message outbound,

            @JsonProperty("tokens")
            int tokens,

            @JsonProperty("created_at")
            Instant createdAt

    ) {
    }

}
