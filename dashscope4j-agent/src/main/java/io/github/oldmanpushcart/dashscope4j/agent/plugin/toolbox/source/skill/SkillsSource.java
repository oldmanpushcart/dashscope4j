package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.AbstractToolSource;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkillsSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path directory;
    private final Duration scanInterval;
    private final boolean isOwnScheduler;
    private final String _toString;

    private final Map<Path, Skill> snapshots = new ConcurrentHashMap<>();

    private volatile State state = State.IDLE;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduleF;

    private SkillsSource(Builder builder) {
        super(builder.name);
        Objects.requireNonNull(builder.directory, "directory must not be null");
        this.directory = builder.directory;
        this.scanInterval = builder.scanInterval;
        this.scheduler = builder.scheduler;
        this.isOwnScheduler = Objects.isNull(this.scheduler);
        this._toString = "dashscope4j-agent:/toolbox/source/skills/%s".formatted(name());
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

        synchronized (this) {
            return snapshots.values()
                    .stream()
                    .map(SkillFunction::new)
                    .map(SkillFunction::asTool)
                    .toList();
        }
    }

    @Override
    public synchronized SkillsSource initialize() {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (isInitialized()) {
            throw new IllegalStateException("Already initialized!");
        }

        // 强制扫描
        scanning();

        // 初始化扫描线程
        if (isOwnScheduler) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                final var t = new Thread(r, "%s/scanner".formatted(SkillsSource.this));
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
                        logger.warn("{} scanning failed by error!", this, t);
                    }

                },
                scanInterval.toMillis(),
                scanInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // 状态流转为运行中
        state = State.INITIALIZED;
        logger.debug("{} initialized.", this);

        return this;
    }

    private synchronized boolean scanning() {

        final var currentVersions = new HashMap<Path, Instant>();
        try (final var stream = Files.list(directory)) {
            stream
                    // 列出当前目录下的所有SKILL目录，并采集他们的SKILL.md文件最后修改时间
                    .map(path -> {
                        final var home = path.resolve("SKILL.md");
                        if (!Files.exists(home)) {
                            logger.debug("{}/scanning ignored skill directory by SKILL.md not found! path={}", this, path);
                            return null;
                        }
                        try {
                            final var lastModifiedAt = Files.getLastModifiedTime(home).toInstant();
                            return Map.entry(path, lastModifiedAt);
                        } catch (IOException e) {
                            logger.debug("{}/scanning ignored skill directory by error! path={}", this, path, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)

                    // 转换为当前版本快照
                    .forEach(entry -> {
                        final var path = entry.getKey().toAbsolutePath().normalize();
                        final var lastModifiedAt = entry.getValue();
                        currentVersions.put(path, lastModifiedAt);
                    });
        } catch (IOException ioEx) {
            logger.debug("{}/scanning ignored skills directory by error! directory: {}", this, directory, ioEx);
        }


        final var toBeRemovePaths = new ArrayList<Path>();
        final var toBeUpdatePaths = new ArrayList<Path>();
        final var toBeInsertPaths = new ArrayList<Path>();

        snapshots.forEach((path, snapshot) -> {

            // 快照有当前版本没有，则认为需要删除
            if (!currentVersions.containsKey(path)) {
                toBeRemovePaths.add(path);
                return;
            }

            // 快照和当前版本都有，则比对版本是否一致，不一致的认为需要更新
            final var version = currentVersions.get(path);
            if (!Objects.equals(snapshot.lastModifiedAt(), version)) {
                toBeUpdatePaths.add(path);
            }

        });

        currentVersions.forEach((path, version) -> {

            // 当前版本有，但快照没有，则认为需要新增
            if (!snapshots.containsKey(path)) {
                toBeInsertPaths.add(path);
            }

        });


        // 先删除所有有变动的
        Stream.of(toBeRemovePaths, toBeUpdatePaths)
                .flatMap(Collection::stream)
                .forEach(path -> {
                    final var skill = snapshots.remove(path);
                    if (null != skill) {
                        logger.debug("{}/scanning remove skill. name={};home={};", this, skill.header().name(), skill.home());
                    }
                });

        // 再添加变更的
        Stream.of(toBeUpdatePaths, toBeInsertPaths)
                .flatMap(Collection::stream)
                .map(home -> {
                    try {
                        final var skill = Skill.of(home);
                        logger.debug("{}/scanning upsert skill. name={};home={};", this, skill.header().name(), skill.home());
                        return skill;
                    } catch (IOException e) {
                        logger.warn("{}/scanning ignored skill by error! home={}", this, home, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(skill -> snapshots.put(skill.home(), skill));

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

    public static class Builder implements Buildable<SkillsSource, Builder> {

        private String name;
        private Path directory;
        private ScheduledExecutorService scheduler;
        private Duration scanInterval = Duration.ofSeconds(5);

        public Builder name(String name) {
            this.name = name;
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
        public SkillsSource build() {
            return new SkillsSource(this);
        }

    }

}
