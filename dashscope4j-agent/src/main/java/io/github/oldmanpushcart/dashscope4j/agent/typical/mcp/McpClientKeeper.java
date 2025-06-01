package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * McpClient 生命周期管理
 * <p>
 * 负责Mcp客户端的初始化、心跳检测、中断重连、资源回收等内置操作，
 * 确保客户端在整个生命周期内保持可用状态。
 * </p>
 */
@Slf4j
public class McpClientKeeper {

    private final Config config;
    private final AtomicBoolean shutdownRef = new AtomicBoolean(false);
    private final Map<String, ClientRegistration> registrationMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(r ->
            new Thread(r) {{
                setName("McpClientKeeper-Executor");
                setDaemon(true);
            }});

    /**
     * 默认构造函数
     * <p>
     * 采用默认配置
     * </p>
     */
    public McpClientKeeper() {
        this(new Config());
    }

    /**
     * 构造函数
     *
     * @param config 配置
     */
    public McpClientKeeper(Config config) {
        this.config = requireNonNull(config);
    }

    @Override
    public String toString() {
        return "dashscope-agent://mcp-client-keeper";
    }


    // 初始化客户端
    private void initialize(ClientRegistration registration) {
        initialize(registration, Duration.ZERO);
    }

    // 初始化客户端
    private void initialize(ClientRegistration registration, Duration interval) {
        executor.schedule(() -> {
            final var client = requireNonNull(registration.factory.create());

            CompletableFuture.completedStage(null)

                    // 初始化Mcp客户端
                    .thenCompose(unused -> client.initialize().toFuture())

                    /*
                     * 初始化成功
                     *
                     * 将初始化好的客户端注入到注册信息中，这样可以通知到所有等待获取客户端的请求拿到最新的客户端
                     * 并创建心跳检测任务检测客户端健康状态
                     */
                    .thenAccept(r -> {
                        log.debug("{}/{} initialized.", this, registration.name);
                        registration.holderRef.get().complete(client);
                        heartbeat(registration);
                    })

                    /*
                     * 初始化失败
                     * 重新创建初始化任务，继续初始化，直到成功或关闭
                     */
                    .exceptionally(ex -> {
                        log.debug("{}/{} initialize failed!", this, registration.name, ex);
                        initialize(registration, config.reinitializeInterval);
                        return null;
                    });

        }, interval.toMillis(), TimeUnit.MILLISECONDS);

    }

    // 客户端心跳检测
    private void heartbeat(ClientRegistration registration) {
        executor.schedule(() -> {
            final var name = registration.name;
            final var holder = registration.holderRef.get();
            holder

                    // 发送Ping包做心跳检测
                    .thenCompose(client -> client.ping().toFuture())

                    /*
                     * 心跳检测成功
                     * 说明客户端健康，需要重新创建下一次心跳任务
                     */
                    .thenAccept(r -> {
                        log.debug("{}/{} heartbeat.", this, name);
                        heartbeat(registration);
                    })

                    /*
                     * 心跳检测失败
                     * 说明客户端网络已中断，需要创建重连任务
                     */
                    .exceptionally(ex -> {

                        /*
                         * CAS创建新的客户端持有者
                         * 1. 创建成功后注册重新连任务，重连时会将初始化好的客户端重新注入到新的持有者中。
                         * 2. 对于现有持有者的客户端进行销毁
                         */
                        if (registration.holderRef.compareAndSet(holder, new CompletableFuture<>())) {
                            log.debug("{}/{} heartbeat failed!", this, name, ex);
                            holder.thenAccept(McpAsyncClient::close);
                            initialize(registration);
                        }
                        return null;

                    });

        }, config.pingInterval.toMillis(), TimeUnit.MILLISECONDS);
    }


    // 检查是否已关闭
    private void checkShutdown() {
        if (shutdownRef.get()) {
            throw new IllegalStateException("Already shutdown!");
        }
    }

    /**
     * 查找已注册信息
     *
     * @param name 连接名称
     * @return 连接
     */
    public ClientRegistration lookup(String name) {
        checkShutdown();
        return Optional
                .ofNullable(registrationMap.get(name))
                .orElseThrow(() -> new IllegalArgumentException("Not registered! name=%s".formatted(name)));
    }

    /**
     * 注册
     *
     * @param name    注册名
     * @param factory 客户端工厂
     * @return 注册信息
     */
    public ClientRegistration register(String name, McpClientFactory factory) {
        checkShutdown();

        // 构建注册信息
        final var registration = new ClientRegistration(name, factory);

        // 尝试进行注册，如果已经存在则抛出异常
        if (null != registrationMap.putIfAbsent(name, registration)) {
            throw new IllegalArgumentException("Already registered! name=%s".formatted(name));
        }

        // 注册成功后开始初始化
        initialize(registration);
        log.debug("{}/{} registered.", this, name);

        return registration;
    }

    /**
     * 注销
     *
     * @param name 注册名
     */
    public void unregister(String name) {
        checkShutdown();

        // 找到客户端注册信息，并加以销毁
        final var registration = registrationMap.get(name);
        if (null != registration) {
            registration.destroy();
        }

        log.debug("{}/{} unregistered.", this, name);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        if (!shutdownRef.compareAndSet(false, true)) {
            return;
        }
        executor.shutdownNow();
        registrationMap.forEach((name, registration) -> registration.destroy());
        registrationMap.clear();
    }

    /**
     * 是否已经关闭
     *
     * @return TRUE | FALSE
     */
    public boolean isShutdown() {
        return shutdownRef.get();
    }


    /**
     * 配置
     */
    @Data
    @Accessors(fluent = true, chain = true)
    public static class Config {

        /**
         * 重新初始化间隔
         */
        private Duration reinitializeInterval = Duration.ofSeconds(5);

        /**
         * ping间隔
         */
        private Duration pingInterval = Duration.ofSeconds(30);

    }


    /**
     * Mcp客户端注册信息
     */
    @AllArgsConstructor
    public static class ClientRegistration {

        private final String name;
        private final McpClientFactory factory;
        private final AtomicReference<CompletableFuture<McpAsyncClient>> holderRef
                = new AtomicReference<>(new CompletableFuture<>());

        /**
         * 销毁客户端注册信息
         * <ul>
         *     <li>若客户端正在初始化，则取消初始化过程</li>
         *     <li>若客户端已经完成初始化，则销毁客户端</li>
         * </ul>
         */
        void destroy() {
            final var future = holderRef.get();
            if (!future.cancel(true)
                && future.isDone()
                && !future.isCompletedExceptionally()) {
                future.thenAccept(McpAsyncClient::close);
            }
        }

        /**
         * 获取McpClient
         * <p>
         * 如果客户端正在初始化，则等待初始化完成
         * </p>
         *
         * @return McpClient
         */
        public CompletionStage<McpAsyncClient> fetch() {
            return holderRef.get();
        }

        /**
         * 获取并验证是否有效的McpClient
         *
         * @return McpClient
         */
        public CompletionStage<McpAsyncClient> validate() {
            return fetch().thenCompose(client ->
                    client.ping()
                            .toFuture()
                            .thenApply(v -> client));
        }

    }


    /**
     * Mcp客户端工厂
     */
    @FunctionalInterface
    public interface McpClientFactory {

        /**
         * 创建新的Mcp客户端
         *
         * @return McpClient
         */
        McpAsyncClient create();

    }

}
