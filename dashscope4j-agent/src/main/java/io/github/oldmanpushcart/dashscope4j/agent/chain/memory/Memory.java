package io.github.oldmanpushcart.dashscope4j.agent.chain.memory;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * 记忆体
 */
public interface Memory {

    /**
     * 获取指定会话的最新的记忆片段 ID
     *
     * @param sessionId 会话 ID
     * @return 最新的记忆片段 ID
     */
    Long newestFragmentId(String sessionId);

    /**
     * 回忆
     * <p>
     * 回忆起{@code (olderThenFragmentId, newerThenFragmentId)}范围之内的记忆片段
     * </p>
     *
     * @param sessionId           会话 ID
     * @param olderThenFragmentId 旧于指定记忆片段 ID
     * @param newerThenFragmentId 新于指定记忆片段 ID
     * @return 回忆起来的记忆片段集合
     */
    List<Fragment> recall(String sessionId, long olderThenFragmentId, long newerThenFragmentId);

    /**
     * 回忆
     * <p>
     * 回忆起{@code (olderThenFragmentId, Long.MAX_VALUE)}范围之内的记忆片段
     * </p>
     *
     * @param sessionId           会话 ID
     * @param olderThenFragmentId 旧于指定记忆片段 ID
     * @return 片段列表
     */
    default List<Fragment> recall(String sessionId, long olderThenFragmentId) {
        return recall(sessionId, olderThenFragmentId, Long.MAX_VALUE);
    }

    /**
     * 存储记忆片段
     *
     * @param fragment 记忆片段
     * @return 记忆片段 ID
     */
    long persist(Fragment fragment);

    /**
     * 记忆片段
     */
    @Data
    @Accessors(fluent = true, chain = true)
    class Fragment implements Comparable<Fragment> {

        /**
         * 记忆片段 ID
         */
        private Long fragmentId;

        /**
         * 会话 ID
         */
        private String sessionId;

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
     * 记忆体上下文
     */
    @Data
    @Accessors(fluent = true, chain = true)
    class Context {

        /**
         * 会话 ID
         */
        String sessionId;

        /**
         * 新于指定记忆片段 ID
         */
        Long newerThenFragmentId;

        /**
         * 旧于指定记忆片段 ID
         */
        Long olderThenFragmentId;

        /**
         * 判断上下文是否无效
         *
         * @param context 上下文
         * @return TRUE | FALSE
         */
        public static boolean isInvalid(Context context) {
            return context == null || context.sessionId() == null;
        }

    }

}
