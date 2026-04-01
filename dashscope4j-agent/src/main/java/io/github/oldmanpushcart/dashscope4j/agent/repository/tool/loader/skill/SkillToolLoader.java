package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.SkillProvider;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;
import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.unmodifiableCopy;

/**
 * Skill Tool Loader - 从 SkillProvider 加载 Skill 并注册为 Tool
 *
 * <p>特性:</p>
 * <ul>
 *   <li>支持 lazy/eager 两种加载模式</li>
 *   <li>提供三个全局工具：get_reference, get_assert, execute_script</li>
 *   <li>为每个 Skill 创建对应的 tool: skill$&lt;skill_name&gt;</li>
 *   <li>自动管理临时文件和资源清理</li>
 *   <li>init 失败时自动 close 防止资源泄漏</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class SkillToolLoader implements Repository.Loader<String, Tool> {

    private static final Logger logger = LoggerFactory.getLogger(SkillToolLoader.class);

    private final List<SkillProvider> providers;

    // 存储已加载的 Skill
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    // 生命周期管理
    private final CompletableFuture<Void> initF = new CompletableFuture<>();
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private volatile Repository.Updater<String, Tool> toolUpdater;
    private volatile Path tempDir;

    private SkillToolLoader(Builder builder) {
        this.providers = unmodifiableCopy(builder.providers);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/tool/loader/skill";
    }

    /**
     * 初始化临时目录
     *
     * @return 临时目录
     */
    private static Path initTempDir() throws IOException {
        final var tempDir = Files.createTempDirectory("skill-loader-");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> toolUpdater) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!initF.complete(null)) {
            throw new IllegalStateException("Already initialized!");
        }

        // 初始化临时目录
        try {
            this.tempDir = initTempDir();
            logger.debug("{} create temp dir: {}", this, tempDir);
        } catch (IOException ioEx) {
            return CompletableFuture.failedFuture(ioEx);
        }

        this.toolUpdater = toolUpdater;

        final var stages = new ArrayList<CompletionStage<Void>>();

        // 初始化全局工具
        List.of(
                new GetReferenceTool(skills).toTool(),
                new GetAssertTool(skills, tempDir).toTool(),
                new ExecuteScriptTool(skills, tempDir, Duration.ofSeconds(30)).toTool()
        ).forEach(tool -> {
            final var stage = toolUpdater.upsert(tool.meta().name(), tool);
            stages.add(stage);
        });


        // 初始化所有 Provider
        final var skillUpdater = new SkillUpdater(toolUpdater, skills);
        providers.forEach(provider -> {
            final var stage = provider.init(skillUpdater);
            stages.add(stage);
        });

        return CompletableFutureUtils.allOf(stages);
    }

    @Override
    public void close() {

        if (!closeF.complete(null)) {
            return;
        }

        // 移除全局工具
        if (null != toolUpdater) {
            toolUpdater.remove(GetAssertTool.TOOL_NAME);
            toolUpdater.remove(GetReferenceTool.TOOL_NAME);
            toolUpdater.remove(ExecuteScriptTool.TOOL_NAME);
            toolUpdater = null;
        }

        // 移除所有 SKILL 注册的工具
        skills.keySet().forEach(name -> toolUpdater.remove(SkillHelper.toToolName(name)));
        skills.clear();

        // 关闭所有 Provider
        providers.forEach(IOUtils::closeQuietly);

        // 清理临时文件
        if (null != tempDir) {
            try (final var walker = Files.walk(tempDir).sorted(Comparator.reverseOrder())) {
                walker.forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException deleteEx) {
                        logger.warn("{} failed to delete temp file: {}", this, path, deleteEx);
                    }
                });
            } catch (IOException cleanupEx) {
                logger.warn("{} failed to cleanup tempDir: {}", this, tempDir, cleanupEx);
            } finally {
                this.tempDir = null;
            }
        }

        logger.debug("{} closed.", this);

    }


    /**
     * SkillProvider.Updater 实现
     *
     * @param updater   Tool Updater
     * @param skillsMap Skill 缓存
     */
    private record SkillUpdater(Repository.Updater<String, Tool> updater, Map<String, Skill> skillsMap)
            implements SkillProvider.Updater {

        @Override
        public CompletionStage<Void> upsert(Skill skill) {
            final var tool = new LoadSkillTool(skill).toTool();
            skillsMap.put(skill.name(), skill);
            return updater.upsert(tool.meta().name(), tool)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            skillsMap.remove(skill.name());
                        }
                    });
        }

        @Override
        public CompletionStage<Void> remove(String name) {
            return updater.remove(SkillHelper.toToolName(name))
                    .thenAccept(unused -> skillsMap.remove(name));
        }

    }

    // === Builder ===

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link SkillToolLoader} instances.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * SkillToolLoader loader = SkillToolLoader.newBuilder()
     *     .addProvider(new FileSkillProvider(Paths.get("skills")))
     *     .addProvider(new DatabaseSkillProvider(dataSource))
     *     .blocking(true)
     *     .parallel(10)
     *     .build();
     * }</pre>
     */
    public static class Builder implements Buildable<SkillToolLoader, Builder> {
        private List<SkillProvider> providers;

        public Builder providers(List<SkillProvider> providers) {
            this.providers = providers;
            return this;
        }

        public Builder providers(UnaryOperator<List<SkillProvider>> operator) {
            this.providers = operator.apply(mutableCopy(this.providers));
            return this;
        }

        @Override
        public SkillToolLoader build() {
            return new SkillToolLoader(this);
        }

    }

}
