package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.SkillProvider;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;

/**
 * Skill Tool Loader - 从 SkillProvider 加载 Skill 并注册为 Tool
 *
 * <p>特性:</p>
 * <ul>
 *   <li>支持 lazy/eager 两种加载模式</li>
 *   <li>提供三个全局工具：get_reference, get_asset, execute_script</li>
 *   <li>为每个 Skill 创建对应的 tool: skill$&lt;skill_name&gt;</li>
 *   <li>自动管理临时文件和资源清理</li>
 *   <li>init 失败时自动 close 防止资源泄漏</li>
 *   <li>支持定时扫描指纹变化，自动重新加载</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class SkillToolLoader implements ToolLoader {

    private static final Logger logger = LoggerFactory.getLogger(SkillToolLoader.class);

    private final Duration scanInterval;

    // 存储已加载的 Skill: skillName -> Skill
    private final Map<String, Skill> skillMap = new ConcurrentHashMap<>();

    // Provider 持有者列表：保持与 providers 顺序一致
    private final List<ProviderHolder> providerHolders;

    // 生命周期管理
    private final CompletableFuture<Void> installF = new CompletableFuture<>();
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private volatile Toolbox toolbox;
    private volatile Path tempDir;

    // 定时扫描线程
    private final Thread scanner;

    /**
     * Provider 持有者 - 封装 Provider 及其签名状态
     */
    private static class ProviderHolder {

        private final SkillProvider provider;
        private volatile String lastSignature;

        ProviderHolder(SkillProvider provider) {
            this.provider = provider;
        }

        SkillProvider provider() {
            return provider;
        }

        String getLastSignature() {
            return lastSignature;
        }

        void setLastSignature(String signature) {
            this.lastSignature = signature;
        }

    }

    private SkillToolLoader(Builder builder) {
        this.scanInterval = builder.scanInterval;

        // 初始化 Provider 持有者列表
        this.providerHolders = CommonUtils.unmodifiableCopy(builder.providers).stream()
                .map(ProviderHolder::new)
                .collect(Collectors.toList());

        // 初始化扫描线程（但不启动）
        this.scanner = new Thread(this::scanLoop, "skill-loader-scanner");
        this.scanner.setDaemon(true);

    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox/loader/skill";
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
    public CompletionStage<Void> install(Toolbox toolbox) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!installF.complete(null)) {
            throw new IllegalStateException("Already installed!");
        }

        // 初始化临时目录
        try {
            this.tempDir = initTempDir();
            logger.debug("{} create temp dir: {}", this, tempDir);
        } catch (IOException ioEx) {
            return CompletableFuture.failedFuture(ioEx);
        }

        this.toolbox = toolbox;

        final var stages = new ArrayList<CompletionStage<Void>>();

        // 初始化全局工具（直接使用 skillMap）
        List.of(
                new GetReferenceTool(skillMap).toTool(),
                new GetAssetTool(skillMap, tempDir).toTool(),
                new ExecuteScriptTool(skillMap, tempDir, Duration.ofSeconds(30)).toTool()
        ).forEach(tool -> {
            final var stage = toolbox.register(tool.meta().name(), tool);
            stages.add(stage);
        });

        // 加载所有 Provider 提供的 Skills
        providerHolders.stream()
                .map(this::loadProvider)
                .forEach(stages::add);

        // 等待所有初始加载完成，然后启动扫描线程
        return CompletableFutureUtils.allOf(stages)
                .thenRun(scanner::start);
    }

    @Override
    public void close() {

        if (!closeF.complete(null)) {
            return;
        }

        // 停止定时扫描
        if (scanner != null) {
            scanner.interrupt();
        }

        // 卸载所有 Provider 的 Skills（异步并等待完成）
        try {
            final var unloadStages = providerHolders.stream()
                    .map(this::unloadProvider)
                    .toList();
            CompletableFutureUtils.allOf(unloadStages).toCompletableFuture().join();
        } catch (Exception ex) {
            logger.warn("{} failed to unload providers", this, ex);
        }
        skillMap.clear();

        // 移除全局工具
        if (null != toolbox) {
            toolbox.remove(GetAssetTool.TOOL_NAME);
            toolbox.remove(GetReferenceTool.TOOL_NAME);
            toolbox.remove(ExecuteScriptTool.TOOL_NAME);
            toolbox = null;
        }

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
     * 加载单个 Provider 的所有 Skills
     *
     * @param providerHolder 已签名的 Provider
     * @return 加载完成的异步回调
     */
    private CompletionStage<Void> loadProvider(ProviderHolder providerHolder) {

        final var provider = providerHolder.provider();

        // 先获取签名，再加载 Skills（避免并发不一致）
        return provider.signature()

                // 记录签名
                .thenAccept(providerHolder::setLastSignature)

                // 加载SKILLS
                .thenCompose(signature -> provider.provide())
                .thenCompose(skills -> {

                    // 为每个 Skill 注册工具
                    final var regStages = skills.stream()
                            .map(skill -> {
                                final var tool = new LoadSkillTool(skill).toTool();
                                return toolbox.register(tool.meta().name(), tool)
                                        .thenAccept(unused -> skillMap.put(skill.header().name(), skill));
                            })
                            .toList();

                    // 等待所有注册完成
                    return CompletableFutureUtils.allOf(regStages);
                });

    }

    /**
     * 卸载单个 Provider 的所有 Skills
     *
     * @param providerHolder Provider 持有者
     * @return 卸载完成的异步回调
     */
    private CompletionStage<Void> unloadProvider(ProviderHolder providerHolder) {

        final var provider = providerHolder.provider();

        // 查找并移除该 Provider 提供的所有 Skills
        final var toRemove = skillMap.entrySet().stream()
                .filter(entry -> entry.getValue().from() == provider)
                .map(Map.Entry::getKey)
                .toList();

        if (toRemove.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 从 Toolbox 移除工具（异步），完成后清理 skillMap
        final var removeStages = toRemove.stream()
                .map(skillName -> {
                    final var toolName = SkillHelper.toToolName(skillName);
                    return toolbox.remove(toolName)
                            .thenAccept(unused -> skillMap.remove(skillName));
                })
                .toList();

        // 等待所有移除完成
        return CompletableFutureUtils.allOf(removeStages);
    }

    /**
     * 扫描循环 - 在独立线程中运行
     */
    private void scanLoop() {
        logger.debug("{}/scanner started.", this);
        try {
            while (!Thread.currentThread().isInterrupted()) {

                // 等待扫描间隔
                //noinspection BusyWait
                Thread.sleep(scanInterval.toMillis());

                // 执行扫描
                final var stages = providerHolders.stream()
                        .map(providerHolder -> {
                            final var provider = providerHolder.provider();
                            final var signature = providerHolder.getLastSignature();

                            // 获取新签名
                            return provider.signature()
                                    .thenCompose(newSignature -> {

                                        // 签名变化
                                        if (!Objects.equals(newSignature, signature)) {
                                            return CompletableFuture.<Void>completedStage(null)
                                                    .thenCompose(unused -> unloadProvider(providerHolder))
                                                    .thenCompose(unused -> loadProvider(providerHolder));
                                        } else {
                                            return CompletableFuture.completedStage(null);
                                        }

                                    })
                                    .exceptionally(ex -> {
                                        logger.warn("{}/scanner check signature failed!, skipping provider: {}", this, provider, ex);
                                        return null;
                                    });
                        })
                        .toList();

                // 等待扫描完成
                CompletableFutureUtils.allOf(stages)
                        .toCompletableFuture()
                        .join();

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            logger.debug("{}/scanner stopped.", this);
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
     *     .providers(List.of(
     *         FileSkillProvider.ofPath(Paths.get("skills/weekly-report")),
     *         FileSkillProvider.ofPath(Paths.get("skills/school-score"))
     *     ))
     *     .scanInterval(Duration.ofSeconds(30))
     *     .build();
     * }</pre>
     */
    public static class Builder implements Buildable<SkillToolLoader, Builder> {
        private List<SkillProvider> providers;
        private Duration scanInterval = Duration.ofSeconds(60);  // 默认 60 秒

        public Builder providers(List<SkillProvider> providers) {
            this.providers = providers;
            return this;
        }

        public Builder providers(UnaryOperator<List<SkillProvider>> operator) {
            this.providers = operator.apply(mutableCopy(this.providers));
            return this;
        }

        /**
         * 设置签名扫描间隔
         *
         * @param scanInterval 扫描间隔，建议生产环境 >= 60s
         * @return 当前构建器
         */
        public Builder scanInterval(Duration scanInterval) {
            this.scanInterval = scanInterval;
            return this;
        }

        @Override
        public SkillToolLoader build() {
            return new SkillToolLoader(this);
        }

    }

}
