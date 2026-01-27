package io.github.oldmanpushcart.dashscope4j.client;

import java.util.concurrent.CompletionStage;

/**
 * 数据交换连接
 */
public interface ExchangeConnection extends AutoCloseable {

    /**
     * @return 数据交换连接的 ID
     */
    String id();

    /**
     * @return 数据交换连接是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭数据交换连接
     */
    @Override
    void close();

    /**
     * @return 数据交换连接关闭的异步通知
     */
    CompletionStage<Void> closeFuture();

}
