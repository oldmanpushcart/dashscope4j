package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.SkillProvider;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileSkillProvider implements SkillProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path skillsDir;
    private final Duration syncInterval;

    private final Map<String, FileSkill> skills = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> initF = new CompletableFuture<>();
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final Thread syncer;
    private final String _toString;
    private volatile Updater updater;

    public FileSkillProvider(Builder builder) {

        Objects.requireNonNull(builder.skillsDir, "skillsDir must not be null");
        Objects.requireNonNull(builder.syncInterval, "syncInterval must not be null");
        this.skillsDir = builder.skillsDir.normalize();
        this.syncInterval = builder.syncInterval;

        this._toString = "dashscope4j-agent:/skill-provider/file=%s".formatted(skillsDir);
        this.syncer = new Thread(this::sync, _toString);
        this.syncer.setDaemon(true);

    }

    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Updater updater) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!initF.complete(null)) {
            throw new IllegalStateException("Already initialized!");
        }

        this.updater = updater;
        return syncSkills()
                .thenAccept(unused -> syncer.start());
    }

    // 扫描 skills 目录，找出所有符合规范的技能。
    private Map<String, FileSkill> scanSkills() throws IOException {
        try (var paths = Files.list(skillsDir)) {
            final var scanSkills = new HashMap<String, FileSkill>();
            paths.filter(Files::isDirectory)
                    .forEach(skillDir -> {

                        // 加载 SKILL
                        try {
                            final var skill = FileSkill.valueOf(skillDir);
                            scanSkills.put(skill.name(), skill);
                        } catch (Throwable t) {
                            logger.warn("{}/scan error, ignore: {}", this, skillDir, t);
                        }

                    });
            return scanSkills;
        }
    }

    private CompletionStage<Void> syncSkills() {
        try {
            final var scanSkills = scanSkills();

            // 找出已被删除的技能
            final var removeNames = skills.keySet()
                    .stream()
                    .filter(name -> !scanSkills.containsKey(name))
                    .collect(Collectors.toSet());

            // 找出变更的技能
            final var updateSkills = scanSkills.entrySet()
                    .stream()
                    .filter(entry -> {
                        final var name = entry.getKey();
                        final var sSkill = entry.getValue();
                        final var aSkill = skills.get(name);
                        return !sSkill.equals(aSkill);
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            // 变更同步
            final var stages = new ArrayList<CompletionStage<Void>>();
            removeNames.forEach(name -> {
                final var stage = updater.remove(name)
                        .thenAccept(unused -> {
                            skills.remove(name);
                            logger.debug("{} remove skill: {}", this, name);
                        });
                stages.add(stage);
            });
            updateSkills.forEach((name, skill) -> {
                final var stage = updater.upsert(skill)
                        .thenAccept(unused -> {
                            skills.put(name, skill);
                            logger.debug("{} upsert skill: {}", this, name);
                        });
                stages.add(stage);
            });

            return CompletableFutureUtils.allOf(stages);
        } catch (IOException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private void sync() {
        logger.trace("{}/syncer started.", this);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    syncSkills().toCompletableFuture().join();
                    //noinspection BusyWait
                    Thread.sleep(syncInterval.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    logger.warn("{}/sync error, will be retry after: {}ms", this, syncInterval.toMillis(), t);
                }
            }
        } finally {
            logger.trace("{}/syncer stopped.", this);
        }
    }

    @Override
    public void close() {
        if (!closeF.complete(null)) {
            return;
        }
        syncer.interrupt();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<FileSkillProvider, Builder> {

        private Path skillsDir;
        private Duration syncInterval = Duration.ofSeconds(30);

        public Builder skillsDir(Path skillsDir) {
            this.skillsDir = skillsDir;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public FileSkillProvider build() {
            return new FileSkillProvider(this);
        }

    }

}
