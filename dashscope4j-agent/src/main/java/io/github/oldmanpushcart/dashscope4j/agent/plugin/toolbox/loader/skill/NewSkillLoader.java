package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.AbstractToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class NewSkillLoader extends AbstractToolLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolUse.Mode mode;
    private final Path home;
    private final Duration scanInterval;
    private final Thread scanner;
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private NewSkillLoader(Builder builder) {
        Objects.requireNonNull(builder.mode, "mode must not be null");
        Objects.requireNonNull(builder.home, "home must not be null");
        Objects.requireNonNull(builder.scanInterval, "scanInterval must not be null");
        this.mode = builder.mode;
        this.home = builder.home;
        this.scanInterval = builder.scanInterval;
        this.scanner = new Thread(this::scanning);
        this.scanner.start();
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox/loader/skill";
    }

    @Override
    public List<ToolUse> loaded() {
        try {
            final var skill = Skill.of(home);
            final var tool = new LoadSkillFunction(skill).asTool();
            final var use = new ToolUse(mode, tool, this);
            return List.of(use);
        } catch (Throwable t) {
            logger.warn("{} skipping skill, home={};", this, home, t);
            return List.of();
        }
    }

    @Override
    public void close() {
        super.close();
        if (closeF.complete(null)) {
            scanner.interrupt();
        }
    }

    private void scanning() {

        final var lock = new ReentrantLock();
        final var condition = lock.newCondition();
        Skill last = null;
        try {
            while (!Thread.currentThread().isInterrupted()
                    && !closeF.isDone()) {

                lock.lock();
                try {
                    if (!condition.await(scanInterval.toMillis(), TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } finally {
                    lock.unlock();
                }

                final Skill current;
                try {
                    current = Skill.of(home);
                } catch (IOException e) {
                    logger.warn("{} scan skipped by error! home={};", this, home, e);
                    continue;
                }

                if (!Objects.equals(last, current)) {
                    final var upserts = List.of(new ToolUse(mode, new LoadSkillFunction(current).asTool(), this));
                    final var removes = null == last
                            ? List.<String>of()
                            : List.of(last.header().name());
                    notifyChanged(upserts, removes);
                    last = current;
                }

            }
        } catch (InterruptedException iEx) {
            Thread.currentThread().interrupt();
        }

    }

    public static class Builder implements Buildable<NewSkillLoader, Builder> {

        private ToolUse.Mode mode = ToolUse.Mode.DYNAMIC;
        private Path home;
        private Duration scanInterval = Duration.ofSeconds(5);

        public Builder mode(ToolUse.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder home(Path home) {
            this.home = home;
            return this;
        }

        public Builder scanInterval(Duration scanInterval) {
            this.scanInterval = scanInterval;
            return this;
        }

        @Override
        public NewSkillLoader build() {
            return new NewSkillLoader(this);
        }

    }

}
