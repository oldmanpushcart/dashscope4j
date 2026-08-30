package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalStateStage;

public class SkillsToolSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path directory;
    private final Duration scanInterval;
    private final boolean isOwnScheduler;
    private final String _toString;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<Path, Skill> cached = new ConcurrentHashMap<>();

    private volatile State state = State.IDLE;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduleF;

    private SkillsToolSource(Builder builder) {
        super(builder.namespace);
        Objects.requireNonNull(builder.directory, "directory must not be null");
        this.directory = builder.directory;
        this.scanInterval = builder.scanInterval;
        this.scheduler = builder.scheduler;
        this.isOwnScheduler = Objects.isNull(this.scheduler);
        this._toString = "dashscope4j-agent:/toolbox/source/skills/%s".formatted(namespace());
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public List<Tool> tools() {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        rwLock.readLock().lock();
        try {
            return cached.values()
                    .stream()
                    .map(skill -> new SkillFunction(namespace(), skill))
                    .map(SkillFunction::asTool)
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }

    }

    @Override
    public synchronized CompletionStage<SkillsToolSource> initialize() {

        if (isClosed()) {
            return illegalStateStage("Already closed!");
        }

        if (isInitialized()) {
            return illegalStateStage("Already initialized!");
        }

        // 强制扫描
        scanning();

        // 初始化扫描线程
        if (isOwnScheduler) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                final var t = new Thread(r, "%s/scanner".formatted(SkillsToolSource.this));
                t.setDaemon(true);
                return t;
            });
        }

        // 启动扫描任务
        scheduleF = scheduler.scheduleAtFixedRate(
                () -> {

                    // 扫描发现变更，则发送变更事件
                    try {
                        if (scanning()) {
                            fireChanged();
                        }
                    } catch (Throwable t) {
                        logger.warn("{} scan failed by error!", this, t);
                    }

                },
                scanInterval.toMillis(),
                scanInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // 状态流转为运行中
        state = State.INITIALIZED;
        logger.debug("{} initialized. directory={};size={};skills={};",
                this,
                directory,
                cached.size(),
                cached.keySet()
        );

        return CompletableFuture.completedStage(this);
    }

    private synchronized boolean scanning() {

        // 列出当前目录下的所有SKILL目录，并采集他们的SKILL.md文件最后修改时间
        final var snapshotVersions = new HashMap<Path, Instant>();
        try (final var stream = Files.list(directory)) {
            stream.forEach(path -> {
                final var homeMd = path.resolve("SKILL.md");
                final var home = path.toAbsolutePath().normalize();
                if (!Files.exists(homeMd)) {
                    logger.debug("{}/scanning ignored by SKILL.md not found! home={}", this, home);
                    return;
                }
                try {
                    final var version = Files.getLastModifiedTime(homeMd).toInstant();
                    snapshotVersions.put(home, version);
                } catch (IOException e) {
                    logger.debug("{}/scanning ignored by error! home={}", this, home, e);
                }
            });
        } catch (IOException ioEx) {
            logger.debug("{}/scanning ignored by error! directory={}", this, directory, ioEx);
        }


        final var toBeRemovePaths = new ArrayList<Path>();
        final var toBeUpdatePaths = new ArrayList<Path>();
        final var toBeInsertPaths = new ArrayList<Path>();

        cached.forEach((home, skill) -> {

            // 快照有当前版本没有，则认为需要删除
            if (!snapshotVersions.containsKey(home)) {
                toBeRemovePaths.add(home);
                return;
            }

            // 快照和当前版本都有，则比对版本是否一致，不一致的认为需要更新
            final var snapshotVersion = snapshotVersions.get(home);
            final var version = skill.lastModifiedAt();
            if (!Objects.equals(version, snapshotVersion)) {
                toBeUpdatePaths.add(home);
            }

        });

        snapshotVersions.forEach((home, version) -> {

            // 当前版本有，但快照没有，则认为需要新增
            if (!cached.containsKey(home)) {
                toBeInsertPaths.add(home);
            }

        });


        rwLock.writeLock().lock();
        try {
            // 先删除所有有变动的
            Stream.of(toBeRemovePaths, toBeUpdatePaths)
                    .flatMap(Collection::stream)
                    .forEach(home -> {
                        final var skill = cached.remove(home);
                        if (null != skill) {
                            logger.debug("{}/scanning remove skill. name={};home={};", this, skill.header().name(), skill.home());
                        }
                    });

            // 再添加变更的
            Stream.of(toBeUpdatePaths, toBeInsertPaths)
                    .flatMap(Collection::stream)
                    .forEach(home -> {
                        try {
                            final var skill = Skill.of(home);
                            logger.debug("{}/scanning upsert skill. name={};home={};", this, skill.header().name(), skill.home());
                            cached.put(home, skill);
                        } catch (IOException e) {
                            logger.warn("{}/scanning ignored skill by error! home={}", this, home, e);
                        }
                    });
        } finally {
            rwLock.writeLock().unlock();
        }

        // 通知外边，扫描发现了变动
        return !toBeRemovePaths.isEmpty()
                || !toBeUpdatePaths.isEmpty()
                || !toBeInsertPaths.isEmpty();
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    private boolean isInitialized() {
        return State.INITIALIZED == state;
    }

    @Override
    public synchronized void close() {

        if (State.CLOSED == state) {
            return;
        }

        if (null != scheduleF) {
            scheduleF.cancel(true);
            scheduleF = null;
        }

        if (null != scheduler && isOwnScheduler) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        super.close();
        logger.debug("{} closed.", this);
    }

    private enum State {
        IDLE,
        INITIALIZED,
        CLOSED
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<SkillsToolSource, Builder> {

        private String namespace;
        private Path directory;
        private ScheduledExecutorService scheduler;
        private Duration scanInterval = Duration.ofSeconds(5);

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder scanInterval(Duration scanInterval) {
            this.scanInterval = scanInterval;
            return this;
        }

        @Override
        public SkillsToolSource build() {
            return new SkillsToolSource(this);
        }

    }

}
