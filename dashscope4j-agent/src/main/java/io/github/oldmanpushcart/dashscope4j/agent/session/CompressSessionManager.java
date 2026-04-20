package io.github.oldmanpushcart.dashscope4j.agent.session;

import io.github.oldmanpushcart.dashscope4j.agent.session.store.SessionStore;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;

import java.util.*;

/**
 * 压缩会话管理器实现
 * <p>
 * 提供基于会话的记忆管理功能，核心特性包括：
 * <ul>
 *     <li><b>懒加载</b>：首次访问时从存储中加载会话历史</li>
 *     <li><b>Token 限制</b>：自动管理记忆大小，不超过最大 Token 数</li>
 *     <li><b>智能压缩</b>：当记忆超出限制时，使用 LLM 生成摘要并压缩历史</li>
 *     <li><b>LRU 缓存</b>：使用 LinkedHashMap 实现最近最少使用策略，自动淘汰旧会话</li>
 * </ul>
 * </p>
 *
 * @see SessionManager
 */
public class CompressSessionManager implements SessionManager {

    /**
     * 会话映射表：sessionId -> CompressSession
     * <p>
     * 使用 LinkedHashMap 实现 LRU（最近最少使用）策略，
     * 当会话数量超过 maxSessions 时，自动淘汰最久未访问的会话。
     * </p>
     */
    private final Map<String, CompressSession> sessionMap;

    /**
     * 会话存储器
     */
    private final SessionStore store;

    /**
     * DashScope 客户端
     */
    private final DashscopeClient client;

    /**
     * 用于生成摘要的聊天模型
     */
    private final ChatModel model;

    /**
     * 最大 Token 数
     */
    private final int maxTokens;

    /**
     * 保留的 Token 数（用于 GC 后保留的上下文）
     */
    private final int retainTokens;

    /**
     * 构造压缩会话管理器
     *
     * @param builder 构建器
     */
    public CompressSessionManager(Builder builder) {

        Objects.requireNonNull(builder.client, "client must not be null");
        Objects.requireNonNull(builder.model, "model must not be null");
        Objects.requireNonNull(builder.store, "store must not be null");
        CheckUtils.require(builder.maxTokens, t -> t > 0, "maxTokens must be greater than 0");
        CheckUtils.require(builder.gcRatio, t -> t > 0 && t < 1, "gcRatio must in (0,1)");
        CheckUtils.require(builder.maxSessions, t -> t > 0, "maxSessions must be greater than 0");

        // 创建支持 LRU 策略的会话映射表
        this.sessionMap = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, CompressSession> eldest) {
                        return size() > builder.maxSessions;
                    }
                }
        );
        this.store = builder.store;
        this.client = builder.client;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        // 计算保留 Token 数：maxTokens * gcRatio
        this.retainTokens = (int) (maxTokens * builder.gcRatio);

    }

    /**
     * 打开会话
     * <p>
     * 如果指定 ID 的会话已存在且未关闭，则直接返回；
     * 否则创建新的会话并缓存。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 会话实例
     */
    @Override
    public Session open(String sessionId) {
        return sessionMap
                .computeIfAbsent(sessionId, k ->
                        new CompressSession(
                                sessionId,
                                store,
                                client,
                                model,
                                maxTokens,
                                retainTokens
                        ));
    }

    /**
     * 关闭会话管理器
     * <p>
     * 关闭所有活跃的会话，释放底层存储资源。
     * </p>
     */
    @Override
    public void close() {
        // 关闭所有会话
        synchronized (sessionMap) {
            sessionMap.values().forEach(CompressSession::close);
            sessionMap.clear();
        }
        // 释放存储资源
        IOUtils.closeQuietly(store);
    }



    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * CompressSessionManager 构建器
     * <p>
     * 使用 Builder 模式配置压缩会话管理器。
     * </p>
     */
    public static class Builder implements Buildable<CompressSessionManager, Builder> {

        /**
         * 会话存储器
         */
        private SessionStore store;

        /**
         * DashScope 客户端
         */
        private DashscopeClient client;

        /**
         * 聊天模型（用于生成摘要）
         */
        private ChatModel model;

        /**
         * 最大 Token 数
         */
        private int maxTokens;

        /**
         * GC 比例（0-1 之间），决定压缩后保留的 Token 比例
         */
        private double gcRatio;

        /**
         * 最大会话数，默认为 100
         */
        private int maxSessions = 100;

        /**
         * 设置会话存储器
         *
         * @param store 存储器实例
         * @return 当前构建器
         */
        public Builder store(SessionStore store) {
            this.store = store;
            return this;
        }

        /**
         * 设置 DashScope 客户端
         *
         * @param client DashScope 客户端
         * @return 当前构建器
         */
        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        /**
         * 设置聊天模型
         *
         * @param model 聊天模型
         * @return 当前构建器
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * 设置最大 Token 数
         *
         * @param maxTokens 最大 Token 数
         * @return 当前构建器
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 设置 GC 比例
         *
         * @param gcRatio GC 比例（0-1 之间）
         * @return 当前构建器
         */
        public Builder gcRatio(double gcRatio) {
            this.gcRatio = gcRatio;
            return this;
        }

        /**
         * 设置最大会话数
         *
         * @param maxSessions 最大会话数
         * @return 当前构建器
         */
        public Builder maxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
            return this;
        }

        /**
         * 构建压缩会话管理器
         *
         * @return 新创建的压缩会话管理器实例
         */
        @Override
        public CompressSessionManager build() {
            return new CompressSessionManager(this);
        }

    }

}
