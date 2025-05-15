package io.github.oldmanpushcart.dashscope4j.agent.plugin.memory;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * 回忆
     *
     * @param conversationId 对话 ID
     * @param condition      条件
     * @return 回忆起来的记忆片段集合
     */
    List<Fragment> recall(String conversationId, Condition condition);

    /**
     * 存储记忆片段
     *
     * @param fragment 记忆片段
     * @return 记忆片段 ID
     */
    long persist(Fragment fragment);

    /**
     * 条件
     */
    @Data
    @Accessors(fluent = true, chain = true)
    class Condition {

        /**
         * 最大数量
         */
        private Integer maxCount;

        /**
         * 最大长度
         */
        private Integer maxTokens;

        /**
         * 最大时长
         */
        private Duration maxDuration;

        /**
         * 开始 ID
         */
        private Long beginId;

    }

    /**
     * 片段
     */
    @Data
    @Accessors(fluent = true, chain = true)
    class Fragment implements Comparable<Fragment> {

        /**
         * 片段 ID
         */
        private Long fragmentId;

        /**
         * 对话 ID
         */
        private String conversationId;

        /**
         * 请求消息
         */
        private Message requestMessage;

        /**
         * 响应消息
         */
        private Message responseMessage;

        /**
         * 创建时间
         */
        private Instant createdAt;

        /**
         * 更新时间
         */
        private Instant updatedAt;

        @Override
        public int compareTo(Fragment o) {
            return Long.compare(this.fragmentId, o.fragmentId);
        }

    }

    /**
     * 上下文
     */
    @Data
    @Accessors(fluent = true, chain = true)
    class Context {

        /**
         * 对话 ID
         */
        private String conversationId;

        /**
         * 当前片段 ID
         */
        private Long currentId;

    }

}
