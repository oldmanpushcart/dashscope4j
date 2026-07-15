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

public class SkillSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path home;
    private final Duration scanInterval;
    private final boolean isOwnScheduler;
    private final boolean blockingInitialize;
    private final String _toString;

    private volatile State state = State.IDLE;
    private Skill current;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduleF;

    private SkillSource(Builder builder) {
        super(builder.name);
        Objects.requireNonNull(builder.home, "home must not be null");
        Objects.requireNonNull(builder.scanInterval, "scanInterval must not be null");
        this.home = builder.home;
        this.scheduler = builder.scheduler;
        this.scanInterval = builder.scanInterval;
        this.isOwnScheduler = null == builder.scheduler;
        this.blockingInitialize = builder.blockingInitialize;
        this._toString = "dashscope4j-agent:/toolbox/source/skill/%s".formatted(name());
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public List<Tool> tools() {

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        synchronized (this) {
            return Optional.ofNullable(current)
                    .map(SkillFunction::new)
                    .map(SkillFunction::asTool)
                    .map(List::of)
                    .orElseGet(List::of);
        }
    }

    @Override
    public synchronized SkillSource initialize() {

        if (State.CLOSED == state) {
            throw new IllegalStateException("Already closed!");
        }

        if (State.RUNNING == state) {
            return this;
        }

        // 初始化扫描线程
        if (isOwnScheduler) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                final var t = new Thread(r, "%s/scanner".formatted(SkillSource.this));
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
                        logger.warn("{} scanning failed by error! home={}", this, home, t);
                    }

                },
                scanInterval.toMillis(),
                scanInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // 开始初始化扫描
        if (blockingInitialize) {
            scanning();
        }

        // 状态流转为运行中
        state = State.RUNNING;
        logger.debug("{} initialized.", this);

        return this;
    }

    private synchronized boolean scanning() {
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
        return State.CLOSED == state;
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
        RUNNING,
        CLOSED
    }

    public static class Builder implements Buildable<SkillSource, Builder> {

        private String name;
        private Path home;
        private ScheduledExecutorService scheduler;
        private Duration scanInterval = Duration.ofSeconds(5);
        private boolean blockingInitialize;

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

        public Builder blockingInitialize(boolean blockingInitialize) {
            this.blockingInitialize = blockingInitialize;
            return this;
        }

        @Override
        public SkillSource build() {
            return new SkillSource(this);
        }

    }

}
