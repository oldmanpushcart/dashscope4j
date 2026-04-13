package io.github.oldmanpushcart.dashscope4j.agent.session;

/**
 * 会话管理器
 * <p>
 * 负责管理多个会话的生命周期，提供会话的打开、关闭等功能。
 * 内部使用 LRU 策略管理会话缓存，自动淘汰长时间未使用的会话。
 * </p>
 */
public interface SessionManager extends AutoCloseable {

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
    Session open(String sessionId);

    /**
     * 关闭会话管理器
     * <p>
     * 关闭所有活跃的会话，释放底层存储资源。
     * </p>
     */
    @Override
    void close();

}
