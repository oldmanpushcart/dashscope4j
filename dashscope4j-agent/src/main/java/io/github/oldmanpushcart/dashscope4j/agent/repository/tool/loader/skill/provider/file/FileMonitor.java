package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 文件系统监控器 - 负责监听文件系统变化
 * <p>
 * 作为独立线程运行，通过 EventHandler 通知 Provider 文件变更事件
 * </p>
 *
 * @since 4.0.0
 */
class FileMonitor extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FileMonitor.class);

    // === Configuration ===
    private final Path baseDir;
    private final String _toString;
    private final EventHandler eventHandler;

    /**
     * 事件处理器接口
     */
    interface EventHandler {

        /**
         * 文件创建事件
         *
         * @param path 文件路径
         */
        void onFileCreate(Path path);

        /**
         * 文件删除事件
         *
         * @param path 文件路径
         */
        void onFileDelete(Path path);

        /**
         * 文件修改事件
         *
         * @param path 文件路径
         */
        void onFileModify(Path path);
        
    }

    /**
     * 创建文件监控器
     *
     * @param baseDir      基础监听目录
     * @param eventHandler 事件处理器
     */
    FileMonitor(Path baseDir, EventHandler eventHandler) {
        super("FileMonitor-%s".formatted(baseDir.getFileName()));
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.eventHandler = eventHandler;
        this._toString = "dashscope4j-agent:/skill/monitor/%s".formatted(this.baseDir.getFileName());
        this.setDaemon(true);
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 监听目录及其子目录
     */
    private void watching(WatchService watch, Path dir) throws IOException {

        dir.register(watch, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        // 递归注册所有子目录
        try (final var paths = Files.list(dir)) {
            paths.filter(Files::isDirectory)
                    .forEach(child -> {
                        try {
                            watching(watch, child);
                        } catch (IOException e) {
                            logger.warn("Failed to register child directory: {}", child, e);
                        }
                    });
        }
    }

    @Override
    public void run() {
        logger.debug("{} started", this);

        try (final var watch = baseDir.getFileSystem().newWatchService()) {

            // 递归监听所有现有目录
            watching(watch, baseDir);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watch.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 处理事件
                processEvents(key);

            }
        } catch (IOException e) {
            logger.error("{} watching occur error", this, e);
        } finally {
            logger.debug("{} thread stopped", this);
        }
    }

    /**
     * 处理 Watch 事件
     */
    private void processEvents(WatchKey key) {

        for (final var event : key.pollEvents()) {

            final var kind = event.kind();

            /*
             * 被监听的根目录
             * 确保 watchable 是 Path 类型
             *
             * 例如：/Users/dev/skills
             */
            if (!(key.watchable() instanceof Path basePath)) {
                continue;
            }

            /*
             * 事件发生的相对路径
             * 确保 context 是 Path 类型
             *
             * 例如：math/add
             */
            if (!(event.context() instanceof Path pathContext)) {
                continue;
            }

            /*
             * 完整路径
             * 由 <根目录>/<事件发生的相对路径> 拼接而成
             *
             * 例如：/Users/dev/skills/math/add
             */
            final var fullPath = basePath.resolve(pathContext);

            try {
                if (kind == ENTRY_CREATE) {
                    eventHandler.onFileCreate(fullPath);
                } else if (kind == ENTRY_DELETE) {
                    eventHandler.onFileDelete(fullPath);
                } else if (kind == ENTRY_MODIFY) {
                    eventHandler.onFileModify(fullPath);
                }
            } catch (Exception e) {
                logger.warn("{} process event:{}, path:{} occur error!", this, event.kind(), fullPath, e);
            }
        }

        key.reset();
    }

    @Override
    public void close() {
        this.interrupt();
    }
}
