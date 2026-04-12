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

/**
 * 基于文件系统的技能提供者
 * <p>
 * 从指定目录扫描和加载技能（Skill），支持自动同步更新。
 * 核心功能包括：
 * <ul>
 *     <li><b>目录扫描</b>：递归扫描指定目录，加载符合规范的技能</li>
 *     <li><b>增量同步</b>：定期检测技能变更，自动更新已删除或修改的技能</li>
 *     <li><b>后台线程</b>：使用守护线程周期性执行同步任务</li>
 * </ul>
 * </p>
 *
 * @see SkillProvider
 */
public class FileSkillProvider implements SkillProvider {

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 技能扫描目录
     */
    private final Path scanDir;
    
    /**
     * 同步间隔时间
     */
    private final Duration syncInterval;

    /**
     * 已加载的技能映射表：技能名 -> 技能实例
     */
    private final Map<String, FileSkill> skills = new ConcurrentHashMap<>();
    
    /**
     * 初始化完成的 Future
     */
    private final CompletableFuture<Void> initF = new CompletableFuture<>();
    
    /**
     * 关闭完成的 Future
     */
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    
    /**
     * 后台同步线程
     */
    private final Thread syncer;
    
    /**
     * toString 缓存
     */
    private final String _toString;
    
    /**
     * 更新器，用于通知技能变更
     */
    private volatile Updater updater;

    /**
     * 构造文件系统技能提供者
     *
     * @param builder 构建器
     */
    public FileSkillProvider(Builder builder) {

        Objects.requireNonNull(builder.scanDir, "scanDir must not be null");
        Objects.requireNonNull(builder.syncInterval, "syncInterval must not be null");
        this.scanDir = builder.scanDir.normalize();
        this.syncInterval = builder.syncInterval;

        this._toString = "dashscope4j-agent:/skill-provider/file=%s".formatted(scanDir);
        // 创建守护线程用于后台同步
        this.syncer = new Thread(this::sync, _toString);
        this.syncer.setDaemon(true);

    }

    /**
     * 获取字符串表示
     *
     * @return 组件标识字符串
     */
    public String toString() {
        return _toString;
    }

    /**
     * 初始化技能提供者
     * <p>
     * 启动后台同步线程，开始周期性扫描和同步技能。
     * </p>
     *
     * @param updater 更新器，用于通知技能变更
     * @return 初始化完成的 CompletionStage
     * @throws IllegalStateException 如果已经关闭或已经初始化
     */
    @Override
    public CompletionStage<Void> init(Updater updater) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!initF.complete(null)) {
            throw new IllegalStateException("Already initialized!");
        }

        this.updater = updater;
        // 首次同步技能，然后启动后台同步线程
        return syncSkills()
                .thenAccept(unused -> syncer.start());
    }

    /**
     * 扫描技能目录
     * <p>
     * 遍历扫描目录下的所有子目录，尝试加载符合规范的技能。
     * 加载失败的技能会被忽略并记录警告日志。
     * </p>
     *
     * @return 扫描到的技能映射表：技能名 -> 技能实例
     * @throws IOException 如果扫描过程中发生 IO 错误
     */
    // 扫描 skills 目录，找出所有符合规范的技能。
    private Map<String, FileSkill> scanSkills() throws IOException {
        try (var paths = Files.list(scanDir)) {
            final var scanSkills = new HashMap<String, FileSkill>();
            paths.filter(Files::isDirectory)
                    .forEach(skillDir -> {

                        // 加载 SKILL
                        try {
                            final var skill = FileSkill.valueOf(skillDir);
                            scanSkills.put(skill.name(), skill);
                        } catch (Throwable t) {
                            logger.warn("{} scan error, ignore: {}", this, skillDir, t);
                        }

                    });
            return scanSkills;
        }
    }

    /**
     * 同步技能
     * <p>
     * 对比当前技能集和文件系统最新状态，执行增量更新：
     * <ul>
     *     <li>删除已从文件系统移除的技能</li>
     *     <li>新增或更新已变更的技能</li>
     * </ul>
     * </p>
     *
     * @return 同步完成的 CompletionStage
     */
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

    /**
     * 后台同步任务
     * <p>
     * 周期性执行技能同步，直到线程被中断。
     * 如果同步过程中发生异常，会等待下一个周期重试。
     * </p>
     */
    private void sync() {
        logger.trace("{}/syncer started.", this);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 执行技能同步
                    syncSkills().toCompletableFuture().join();
                    //noinspection BusyWait
                    Thread.sleep(syncInterval.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    logger.warn("{} sync error, will be retry after: {}ms", this, syncInterval.toMillis(), t);
                }
            }
        } finally {
            logger.trace("{}/syncer stopped.", this);
        }
    }

    /**
     * 关闭技能提供者
     * <p>
     * 中断后台同步线程，停止周期性同步任务。
     * 该方法可以安全地多次调用，只有第一次调用会真正执行关闭操作。
     * </p>
     */
    @Override
    public void close() {
        if (!closeF.complete(null)) {
            return;
        }
        // 中断后台同步线程
        syncer.interrupt();
    }

    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * FileSkillProvider 构建器
     * <p>
     * 使用 Builder 模式配置文件系统技能提供者。
     * </p>
     */
    public static class Builder implements Buildable<FileSkillProvider, Builder> {

        /**
         * 技能扫描目录
         */
        private Path scanDir;
        
        /**
         * 同步间隔时间，默认为 30 秒
         */
        private Duration syncInterval = Duration.ofSeconds(30);

        /**
         * 设置技能扫描目录
         *
         * @param scanDir 扫描目录路径
         * @return 当前构建器
         */
        public Builder scanDir(Path scanDir) {
            this.scanDir = scanDir;
            return this;
        }

        /**
         * 设置同步间隔时间
         *
         * @param syncInterval 同步间隔
         * @return 当前构建器
         */
        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        /**
         * 构建文件系统技能提供者
         *
         * @return 新创建的提供者实例
         */
        @Override
        public FileSkillProvider build() {
            return new FileSkillProvider(this);
        }

    }

}
