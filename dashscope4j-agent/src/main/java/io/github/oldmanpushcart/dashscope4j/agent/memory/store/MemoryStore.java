package io.github.oldmanpushcart.dashscope4j.agent.memory.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.InstantAsStringDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.InstantAsStringSerializer;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.List;
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
     * <p>
     * 按 fragmentId 倒序返回（最新的在前）。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param after     起始 fragmentID（返回小于此 ID 的片段，Long.MAX_VALUE 表示从头开始）
     * @return 内存片段流
     */
    Publisher<Fragment> flow(String sessionId, long after);

    /**
     * 插入内存片段
     *
     * @param sessionId SESSION ID
     * @param messages  消息列表（包含用户输入和助手输出）
     * @return 内存片段ID
     */
    CompletionStage<Fragment> upsert(String sessionId, List<Message> messages);

    /**
     * 删除内存片段
     *
     * @param fragmentId 内存片段 ID
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
     * @param messages   消息列表（包含用户输入和助手输出）
     * @param tokens     TOKENS
     * @param createdAt  创建时间
     */
    record Fragment(

            @JsonProperty("fragment_id")
            long fragmentId,

            @JsonProperty("session_id")
            String sessionId,

            @JsonProperty("messages")
            List<Message> messages,

            @JsonProperty("tokens")
            int tokens,

            @JsonSerialize(using = InstantAsStringSerializer.class)
            @JsonDeserialize(using = InstantAsStringDeserializer.class)
            @JsonProperty("created_at")
            Instant createdAt

    ) {

    }

}
