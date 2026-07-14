package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class SkillSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path home;
    private final Duration scanInterval;
    private final String _toString;

    private volatile Skill current;
    private ScheduledExecutorService scheduler;
    private boolean isOwnScheduler;

    private final AtomicReference<CompletableFuture<SkillSource>> initRef = new AtomicReference<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private SkillSource(Builder builder) {
        super(builder.name);
        Objects.requireNonNull(builder.home, "home must not be null");
        Objects.requireNonNull(builder.scanInterval, "scanInterval must not be null");
        this.home = builder.home;
        this.scheduler = builder.scheduler;
        this.scanInterval = builder.scanInterval;
        this._toString = "dashscope4j-agent:/toolbox/source/skill/%s".formatted(name());
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<List<Tool>> tools() {

        final var initF = initRef.get();
        if (null == initF) {
            return CompletableFuture.failedStage(new IllegalStateException("Not initialized!"));
        }

        return initF.thenApply(u -> Optional.ofNullable(current)
                .map(skill -> new SkillFunction(skill).asTool())
                .map(List::of)
                .orElseGet(List::of));
    }

    @Override
    public CompletionStage<SkillSource> initialize() {
        final var initF = new CompletableFuture<SkillSource>();
        if (!initRef.compareAndSet(null, initF)) {
            return initRef.get();
        }

        CompletableFuture
                .runAsync(() -> {

                    synchronized (this) {
                        // 初始化扫描线程
                        if (null == scheduler) {
                            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                                final var t = new Thread(r, "%s/scanner".formatted(SkillSource.this));
                                t.setDaemon(true);
                                return t;
                            });
                            isOwnScheduler = true;
                        }

                        // 启动扫描任务
                        scheduler.scheduleAtFixedRate(
                                () -> {

                                    // 扫描发现变更，则发送变更事件
                                    try {
                                        if (scanning()) {
                                            fireChanged();
                                        }
                                    } catch (Throwable t) {
                                        logger.warn("{} scanning failed by error! home={}", this, home, t);
                                    }

                                },
                                scanInterval.toMillis(),
                                scanInterval.toMillis(),
                                TimeUnit.MILLISECONDS
                        );
                    }

                    // 开始初始化扫描
                    scanning();

                })
                .whenComplete((v, t) -> {
                    if (t != null) {
                        initF.completeExceptionally(t);
                    } else {
                        initF.complete(this);
                    }
                });

        return initF;
    }

    private boolean scanning() {
        try {
            final var skillMdPath = home.resolve("SKILL.md");

            // 如果文件不存在，则根据是否之前已经加载过做为变更判断
            if (!Files.exists(skillMdPath)) {
                synchronized (this) {
                    final var isChanged = null != current;
                    if (isChanged) {
                        logger.debug("{}/scanning SKILL.md not found, unloading current skill. skill={};home={}", this, current.header().name(), home);
                    }
                    current = null;
                    return isChanged;
                }
            }

            // 如果文件已存在，则和已加载的技能做比对
            final var lastModifiedAt = Files.getLastModifiedTime(skillMdPath).toInstant();
            synchronized (this) {
                if (null == current
                        || !lastModifiedAt.equals(current.lastModifiedAt())) {
                    current = Skill.of(home);
                    return true;
                }
            }
        } catch (Throwable t) {
            logger.warn("{}/scanning failed by error! home={}", this, home, t);
        }

        // 异常或文件时间戳没有改变，都认为技能没有变更
        return false;
    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public void close() {
        initRef.compareAndSet(null, CompletableFuture.failedFuture(new IllegalStateException("Already closed!")));
        initRef.get()
                .whenComplete((u, t) -> {
                    if (closeF.complete(null)) {
                        if (null != scheduler && isOwnScheduler) {
                            scheduler.shutdownNow();
                        }
                        super.close();
                        logger.debug("{} closed.", this);
                    }
                });
    }

    public static class Builder implements Buildable<SkillSource, Builder> {

        private String name;
        private Path home;
        private ScheduledExecutorService scheduler;
        private Duration scanInterval = Duration.ofSeconds(5);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder home(Path home) {
            this.home = home;
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
        public SkillSource build() {
            return new SkillSource(this);
        }

    }

}
