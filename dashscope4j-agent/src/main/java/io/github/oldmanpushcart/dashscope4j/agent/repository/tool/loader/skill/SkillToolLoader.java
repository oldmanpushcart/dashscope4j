package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anthropic Skills 加载器
 * <p>
 * 根据 Anthropic Skills 规范，从本地技能目录加载工具：
 * - 扫描技能根目录下的所有子目录（每个子目录是一个 Skill）
 * - 解析每个 Skill 的 SKILL.md 文件（包含 YAML frontmatter 和 Markdown 主体）
 * - 将每个 Skill 注册为 FunctionTool，名称前缀为 skill$<SKILL 名称>$
 * - 监听文件变化，自动重新加载
 * <p>
 * 使用示例：
 * <pre>{@code
 * SkillToolLoader loader = SkillToolLoader.newBuilder()
 *     .skillsRootDir(Paths.get("/path/to/skills"))
 *     .debounceMillis(500)
 *     .build();
 * }</pre>
 */
public class SkillToolLoader implements Repository.Loader<String, Tool>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SkillToolLoader.class);
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private final Path skillsRootDir;
    private final long debounceMillis;
    private final boolean lazy;
    private final Map<String, SkillInfo> loadedSkills;
    private final ExecutorService executor; // 用于异步任务处理

    // WatchService 和防抖相关（volatile 保证可见性）
    private final WatchService watchService;
    private volatile boolean isWatching;
    private final Map<Path, Long> pendingChanges; // skillDir -> nextProcessTime
    private final Object taskLock = new Object(); // 任务锁
    private volatile CompletableFuture<Void> currentProcessingTask;

    /**
     * 私有构造函数，通过 Builder 构建实例
     */
    private SkillToolLoader(Builder builder) {
        this.skillsRootDir = builder.skillsRootDir.toAbsolutePath().normalize();
        this.debounceMillis = builder.debounceMillis;
        this.lazy = builder.lazy;
        this.loadedSkills = new HashMap<>();

        // 创建执行器（用于异步任务处理）
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkillToolLoader-Processor");
            t.setDaemon(true);
            return t;
        });

        // 创建 WatchService（用于文件监听）
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create WatchService", e);
        }

        this.isWatching = false;
        this.pendingChanges = new HashMap<>();
        this.currentProcessingTask = null;
        logger.info("SkillToolLoader initialized with skills root directory: {}, debounce: {}ms",
                this.skillsRootDir, this.debounceMillis);
    }

    /**
     * 创建 Builder 实例
     *
     * @return Builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {
        return CompletableFuture.completedStage(null)
                .thenAccept(unused -> {
                    try {
                        if (lazy) {
                            // Lazy 模式：仅创建目录，不立即加载，等待文件变化时触发
                            logger.info("SkillToolLoader initialized in LAZY mode. Skills will be loaded on first change.");
                            ensureSkillsDirExists();
                        } else {
                            // Eager 模式：立即加载所有 Skills
                            loadAllSkillsSync(updater);
                            logger.info("SkillToolLoader initialized successfully, loaded {} skills", loadedSkills.size());
                        }

                        // 启动文件监听
                        startWatching(updater);

                    } catch (IOException e) {
                        logger.error("Failed to initialize SkillToolLoader", e);
                        throw new RuntimeException("Failed to initialize SkillToolLoader", e);
                    }
                });
    }

    /**
     * 确保技能目录存在
     */
    private void ensureSkillsDirExists() throws IOException {
        if (!Files.exists(skillsRootDir)) {
            logger.warn("Skills root directory does not exist: {}, creating it", skillsRootDir);
            Files.createDirectories(skillsRootDir);
        }

        if (!Files.isDirectory(skillsRootDir)) {
            throw new IOException("Skills root path is not a directory: " + skillsRootDir);
        }
    }

    /**
     * 加载所有 Skills（同步版本，等待所有 upsert 完成）
     */
    private void loadAllSkillsSync(Repository.Updater<String, Tool> updater) throws IOException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 遍历所有子目录（每个子目录是一个 Skill）
        try (var stream = Files.list(skillsRootDir)) {
            stream
                    .filter(Files::isDirectory)
                    .forEach(skillDir -> {
                        try {
                            CompletableFuture<Void> future = loadSingleSkillAsync(skillDir, updater);
                            futures.add(future);
                        } catch (Exception e) {
                            logger.error("Failed to load skill from directory: {}", skillDir, e);
                        }
                    });
        }

        // 等待所有 upsert 操作完成
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * 异步加载单个 Skill
     *
     * @param skillDir Skill 目录
     * @param updater  仓库更新器
     * @return 异步完成标识
     */
    private CompletableFuture<Void> loadSingleSkillAsync(Path skillDir, Repository.Updater<String, Tool> updater) throws IOException {
        Path skillMdPath = skillDir.resolve("SKILL.md");

        if (!Files.exists(skillMdPath)) {
            logger.warn("SKILL.md not found in directory: {}, skipping", skillDir);
            return CompletableFuture.completedFuture(null);
        }

        // 读取并解析 SKILL.md
        String content = Files.readString(skillMdPath);
        SkillDefinition skillDef = parseSkillMd(content);

        if (skillDef == null) {
            logger.error("Failed to parse SKILL.md in directory: {}", skillDir);
            return CompletableFuture.completedFuture(null);
        }

        // 验证 name 字段与目录名一致
        SkillDefinition correctedSkillDef = validateAndCorrectSkillName(skillDef, skillDir);

        // 最终的工具名称：skill$<SKILL 名称>
        final String toolName = "skill$" + correctedSkillDef.name();

        // 创建 FunctionTool 并等待 upsert 完成
        return updater.upsert(toolName, createSkillTool(correctedSkillDef))
                .thenRun(() -> {
                    // 记录已加载的 Skill
                    try {
                        loadedSkills.put(toolName, new SkillInfo(skillDir, correctedSkillDef, Files.getLastModifiedTime(skillMdPath).toMillis()));
                        logger.info("Loaded skill: {} from directory: {}", toolName, skillDir);
                    } catch (IOException e) {
                        logger.error("Failed to get last modified time for skill: {}", toolName, e);
                    }
                }).toCompletableFuture();
    }


    /**
     * 验证并校正 Skill 名称（确保与目录名一致）
     *
     * @param skillDef Skill 定义
     * @param skillDir Skill 目录
     * @return 校正后的 Skill 定义
     */
    private SkillDefinition validateAndCorrectSkillName(SkillDefinition skillDef, Path skillDir) {
        String dirName = skillDir.getFileName().toString();
        if (!skillDef.name().equals(dirName)) {
            logger.warn("Skill name '{}' does not match directory name '{}', using directory name",
                    skillDef.name(), dirName);
            return new SkillDefinition(
                    dirName,
                    skillDef.description(),
                    skillDef.license(),
                    skillDef.compatibility(),
                    skillDef.metadata(),
                    skillDef.allowedTools(),
                    skillDef.bodyContent()
            );
        }
        return skillDef;
    }

    /**
     * 解析 SKILL.md 文件
     *
     * @param content SKILL.md 内容
     * @return SkillDefinition 对象
     */
    private SkillDefinition parseSkillMd(String content) {
        // 提取 YAML frontmatter（--- 之间的部分）
        Pattern frontmatterPattern = Pattern.compile("^---\\s*\n(.*?)\n---\\s*\n(.*)", Pattern.DOTALL);
        Matcher matcher = frontmatterPattern.matcher(content);

        if (!matcher.matches()) {
            logger.error("Invalid SKILL.md format: missing YAML frontmatter");
            return null;
        }

        String yamlContent = matcher.group(1);
        String bodyContent = matcher.group(2);

        try {
            // 解析 YAML 为 JsonNode
            JsonNode yamlNode = YAML_MAPPER.readTree(yamlContent);

            String name = yamlNode.has("name") ? yamlNode.get("name").asText() : null;
            String description = yamlNode.has("description") ? yamlNode.get("description").asText() : "";
            String license = yamlNode.has("license") ? yamlNode.get("license").asText() : null;
            String compatibility = yamlNode.has("compatibility") ? yamlNode.get("compatibility").asText() : null;

            // 解析 metadata（可选）
            Map<String, String> metadata = new HashMap<>();
            if (yamlNode.has("metadata") && yamlNode.get("metadata").isObject()) {
                JsonNode metadataNode = yamlNode.get("metadata");
                // 使用 properties() 替代已废弃的 fields()
                metadataNode.properties().forEach(entry ->
                        metadata.put(entry.getKey(), entry.getValue().asText())
                );
            }

            // 解析 allowed-tools（可选）
            List<String> allowedTools = new ArrayList<>();
            if (yamlNode.has("allowed-tools") && yamlNode.get("allowed-tools").isArray()) {
                JsonNode allowedToolsNode = yamlNode.get("allowed-tools");
                for (JsonNode node : allowedToolsNode) {
                    allowedTools.add(node.asText());
                }
            }

            return new SkillDefinition(name, description, license, compatibility, metadata, allowedTools, bodyContent);

        } catch (IOException e) {
            logger.error("Failed to parse YAML frontmatter", e);
            return null;
        }
    }

    /**
     * 创建 Skill 对应的 FunctionTool
     * <p>
     * 工具名称格式：skill$<SKILL 名称>
     *
     * @param skillDef Skill 定义
     * @return FunctionTool 实例
     */
    private static FunctionTool createSkillTool(SkillDefinition skillDef) {
        final var toolName = "skill$" + skillDef.name();
        
        return FunctionTool.newBuilder()
                .name(toolName)
                .description(buildToolDescription(skillDef))
                .parameterType(SkillInvocationSpec.class)
                .<SkillInvocationSpec>function((caller, spec) -> {
                    // Skill 调用逻辑
                    logger.debug("Invoking skill: {} with input: {}", toolName, spec);

                    // 返回 Skill 的指令集，作为 ReAct 的提示
                    String response = buildSkillResponse(skillDef, spec);
                    return CompletableFuture.completedStage(response);

                })
                .build();
    }

    /**
     * 构建工具描述（仅包含 YAML 元数据）
     *
     * @param def Skill 定义
     * @return 工具描述文本
     */
    private static String buildToolDescription(SkillDefinition def) {
        return PromptTemplate.newBuilder()
                .template("""
                        这是一个`SKILL`工具，里面描述了如何解决问题并最终得到答案。
                        
                        ## 名称: ${skill_name}
                        
                        ## 描述
                        ${skill_description}
                        
                        ## 能力
                        ${skill_compatibility}
                        
                        ## 使用工具
                        ${skill_allowed_tools}
                        """)
                .variable("skill_name", def.name())
                .variable("skill_description", def.description())
                .variable("skill_compatibility", def.compatibility())
                .variable("skill_allowed_tools", def.allowedTools())
                .build()
                .render();
    }

    /**
     * 构建 Skill 响应（作为 ReAct 的提示）
     *
     * @param def  Skill 定义
     * @param spec 调用参数
     * @return 响应文本，包含 Role、Goal、Constraints 等引导信息
     */
    private static String buildSkillResponse(SkillDefinition def, SkillInvocationSpec spec) {
        return PromptTemplate.newBuilder()
                .template("""
                        ## 写在前边
                        这里并非为最终答案，我将通过`描述正文`引导你完成任务。
                        你需要根据引导进行思考和使用推荐的工具完成工作。现在请进行下一步的思考。
                        
                        ## 描述正文
                        ${skill_body}
                        """)
                .variable("skill_body", def.bodyContent())
                .build()
                .render();
    }

    /**
     * 启动文件监听，当 Skill 文件变更时重新加载
     */
    private void startWatching(Repository.Updater<String, Tool> updater) {
        // 注册根目录和现有子目录的监听
        registerDirectory(skillsRootDir, watchService);
        try (var stream = Files.list(skillsRootDir)) {
            stream
                    .filter(Files::isDirectory)
                    .forEach(dir -> registerDirectory(dir, watchService));
        } catch (IOException e) {
            logger.error("Failed to list skill directories", e);
        }

        isWatching = true;

        // 在后台线程中监听文件变化
        Thread watchThread = new Thread(() -> runWatchLoop(watchService, updater), "SkillToolLoader-WatchLoop");
        watchThread.setDaemon(true);
        watchThread.start();

        logger.info("Started watching skill directory changes");
    }

    /**
     * 运行文件监听循环
     */
    private void runWatchLoop(WatchService watchService, Repository.Updater<String, Tool> updater) {
        logger.debug("Watch loop started");
        while (isWatching) {
            WatchKey key = null;
            try {
                logger.trace("Waiting for watch key...");
                key = watchService.take();
                logger.debug("Watch key received: {}", key);

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path changedPath = (Path) event.context();
                    Path contextPath = ((Path) key.watchable()).resolve(changedPath);

                    logger.debug("File event detected: kind={}, path={}, context={}", kind, changedPath, contextPath);

                    // 检查是否是 SKILL.md 文件或新目录
                    if (changedPath.toString().equals("SKILL.md")) {
                        // SKILL.md 文件变化，加入待处理队列
                        logger.debug("SKILL.md changed, scheduling reload for: {}", contextPath.getParent());
                        scheduleReload(contextPath.getParent(), updater);
                    } else if (Files.isDirectory(contextPath)) {
                        // 新目录创建，注册监听并重新加载
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            logger.debug("New directory created: {}, registering watch", contextPath);
                            registerDirectory(contextPath, watchService);
                        }
                        logger.debug("Directory change detected: {}", contextPath);
                        scheduleReload(contextPath, updater);
                    }
                }

                key.reset();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error watching file changes", e);
                if (key != null) {
                    key.reset();
                }
            }
        }
        logger.debug("Watch loop stopped");
    }

    /**
     * 注册目录监听
     */
    private void registerDirectory(Path dir, WatchService watchService) {
        try {
            if (Files.isDirectory(dir)) {
                dir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                logger.debug("Registered watch for directory: {}", dir);
            }
        } catch (IOException e) {
            logger.warn("Failed to register watch for directory: {}", dir, e);
        }
    }

    /**
     * 调度重新加载任务（带防抖）
     */
    private void scheduleReload(Path skillDir, Repository.Updater<String, Tool> updater) {
        long currentTime = System.currentTimeMillis();
        long processTime = currentTime + debounceMillis;

        synchronized (pendingChanges) {
            pendingChanges.put(skillDir, processTime);
            logger.debug("Scheduled reload for skill: {} at {}", skillDir, processTime);
        }

        // 启动处理任务（使用简单的同步检查）
        synchronized (taskLock) {
            if (currentProcessingTask == null || currentProcessingTask.isDone()) {
                currentProcessingTask = processPendingChanges(updater);
            }
        }
    }


    /**
     * 处理待处理的变更（带防抖逻辑）
     */
    private CompletableFuture<Void> processPendingChanges(Repository.Updater<String, Tool> updater) {
        return CompletableFuture.runAsync(() -> {
            try {
                while (isWatching) {
                    Thread.sleep(50); // 短暂休眠，避免空转

                    long currentTime = System.currentTimeMillis();
                    List<Path> readyToProcess;

                    synchronized (pendingChanges) {
                        // 找出所有已到期的任务
                        readyToProcess = pendingChanges.entrySet().stream()
                                .filter(entry -> entry.getValue() <= currentTime)
                                .map(Map.Entry::getKey)
                                .toList();

                        if (readyToProcess.isEmpty()) {
                            // 没有待处理的任务，继续等待下一批
                            continue;
                        }

                        // 移除已处理的任务
                        readyToProcess.forEach(pendingChanges::remove);
                    }

                    // 处理每个变化的 skill
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    for (Path skillDir : readyToProcess) {
                        try {
                            CompletableFuture<Void> future = reloadSingleSkillAsync(skillDir, updater);
                            futures.add(future);
                        } catch (Exception e) {
                            logger.error("Failed to reload skill: {}", skillDir, e);
                        }
                    }
                    
                    // 等待所有 reload 操作完成
                    if (!futures.isEmpty()) {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error processing pending changes", e);
            }
        }, executor);
    }

    /**
     * 重新加载单个 Skill（异步版本，等待 upsert 完成）
     */
    private CompletableFuture<Void> reloadSingleSkillAsync(Path skillDir, Repository.Updater<String, Tool> updater) throws IOException {
        // 初始工具名称（用于查找已加载的技能）：skill$<目录名>
        final String initialToolName = "skill$" + skillDir.getFileName();

        logger.info("Reloading skill: {} from directory: {}", initialToolName, skillDir);

        // 检查目录是否存在
        if (!Files.exists(skillDir) || !Files.isDirectory(skillDir)) {
            // 目录被删除，移除工具
            if (loadedSkills.containsKey(initialToolName)) {
                return updater.remove(initialToolName)
                        .thenRun(() -> {
                            loadedSkills.remove(initialToolName);
                            logger.info("Removed deleted skill: {}", initialToolName);
                        }).toCompletableFuture();
            }
            return CompletableFuture.completedFuture(null);
        }

        // 检查 SKILL.md 是否存在
        Path skillMdPath = skillDir.resolve("SKILL.md");
        if (!Files.exists(skillMdPath)) {
            // SKILL.md 被删除，移除工具
            if (loadedSkills.containsKey(initialToolName)) {
                return updater.remove(initialToolName)
                        .thenRun(() -> {
                            loadedSkills.remove(initialToolName);
                            logger.info("Removed skill due to missing SKILL.md: {}", initialToolName);
                        }).toCompletableFuture();
            }
            return CompletableFuture.completedFuture(null);
        }

        // 读取并解析 SKILL.md
        String content = Files.readString(skillMdPath);
        SkillDefinition skillDef = parseSkillMd(content);

        if (skillDef == null) {
            logger.error("Failed to parse SKILL.md in directory: {}, keeping old version if exists", skillDir);
            return CompletableFuture.completedFuture(null);
        }

        // 验证 name 字段与目录名一致
        SkillDefinition correctedSkillDef = validateAndCorrectSkillName(skillDef, skillDir);

        // 最终的工具名称：skill$<SKILL 名称>
        final String toolName = "skill$" + correctedSkillDef.name();

        // 更新到 updater 并等待完成
        return updater.upsert(toolName, createSkillTool(correctedSkillDef))
                .thenRun(() -> {
                    // 更新已加载的 Skill
                    try {
                        loadedSkills.put(toolName, new SkillInfo(skillDir, correctedSkillDef, Files.getLastModifiedTime(skillMdPath).toMillis()));
                        logger.info("Reloaded skill: {} successfully", toolName);
                    } catch (IOException e) {
                        logger.error("Failed to get last modified time for skill: {}", toolName, e);
                    }
                }).toCompletableFuture();
    }

    /**
     * 重新加载所有 Skills（保留防抖机制）
     */
    private void reloadAllSkills(Repository.Updater<String, Tool> updater) {
        logger.info("Reloading all skills...");

        try {
            // 清空已加载的 Skills
            loadedSkills.keySet().forEach(key -> {
                try {
                    updater.remove(key);
                } catch (Exception e) {
                    logger.error("Failed to remove skill: {}", key, e);
                }
            });
            loadedSkills.clear();

            // 重新加载（使用异步版本并等待完成）
            try {
                loadAllSkillsSync(updater);
            } catch (Exception e) {
                logger.error("Failed to reload skills", e);
                throw e;
            }

            logger.info("Reloaded {} skills", loadedSkills.size());

        } catch (IOException e) {
            logger.error("Failed to reload skills", e);
        }
    }

    @Override
    public void close() {
        // 停止监听
        isWatching = false;

        // 关闭 WatchService
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Failed to close watch service", e);
            }
        }

        // 关闭执行器（优雅关闭）
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清空缓存
        loadedSkills.clear();
        pendingChanges.clear();

        logger.info("SkillToolLoader closed");
    }

    // ==================== Builder 模式 ====================

    /**
     * Builder for creating {@link SkillToolLoader} instances.
     */
    public static class Builder implements Buildable<SkillToolLoader, Builder> {

        private Path skillsRootDir;
        private long debounceMillis = 500; // 默认 500ms 防抖
        private boolean lazy = false; // 默认 eager 模式，立即加载

        /**
         * 设置技能根目录（必需）
         *
         * @param skillsRootDir 技能根目录路径
         * @return Builder
         */
        public Builder skillsRootDir(Path skillsRootDir) {
            this.skillsRootDir = skillsRootDir;
            return this;
        }

        /**
         * 设置防抖时间（可选，默认 500ms）
         * <p>
         * 当同一个 skill 文件在短时间内多次变化时，只会触发一次加载。
         *
         * @param debounceMillis 防抖时间（毫秒）
         * @return Builder
         */
        public Builder debounceMillis(long debounceMillis) {
            if (debounceMillis < 0) {
                throw new IllegalArgumentException("debounceMillis must be non-negative");
            }
            this.debounceMillis = debounceMillis;
            return this;
        }

        /**
         * 设置是否使用 lazy 加载模式（可选，默认 false）
         * <p>
         * - lazy=false（eager 模式）：初始化时立即加载所有现有 skills
         * - lazy=true（lazy 模式）：初始化时不加载，仅在检测到文件变化时加载
         *
         * @param lazy 是否使用 lazy 模式
         * @return Builder
         */
        public Builder lazy(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        @Override
        public SkillToolLoader build() {
            if (skillsRootDir == null) {
                throw new IllegalArgumentException("skillsRootDir is required");
            }
            return new SkillToolLoader(this);
        }
    }

    // ==================== 数据结构 ====================

    /**
     * Skill 定义信息
     */
    private record SkillDefinition(
            String name,
            String description,
            String license,
            String compatibility,
            Map<String, String> metadata,
            List<String> allowedTools,
            String bodyContent
    ) {
    }

    /**
     * 已加载 Skill 的信息
     */
    private record SkillInfo(
            Path directory,
            SkillDefinition definition,
            long lastModifiedTime
    ) {
    }

    /**
     * Skill 调用参数规格
     */
    public record SkillInvocationSpec(

            @JsonPropertyDescription("输入参数（JSON 格式）")
            @JsonProperty("input")
            String input

    ) {
    }

}
