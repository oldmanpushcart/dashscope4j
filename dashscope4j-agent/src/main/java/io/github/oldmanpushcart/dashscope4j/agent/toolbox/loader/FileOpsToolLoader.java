package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * 文件操作工具加载器
 * <p>
 * 提供基础文件操作工具给 LLM 使用：
 * - info: 获取文件/目录信息
 * - delete: 删除文件或空目录
 * - move: 移动/重命名文件
 * - create: 创建文件或目录
 * - list: 列出目录内容
 * - search: 搜索文件（支持详细模式）
 * </p>
 * <p>
 * 所有路径操作都限制在 workspace 范围内，防止目录穿越攻击。
 * </p>
 */
public class FileOpsToolLoader implements ToolLoader {

    /**
     * 默认最大返回条目数
     */
    private static final int DEFAULT_MAX_RESULTS = 100;

    /**
     * 工作区根路径
     */
    private final Path workspace;

    /**
     * 最大返回条目数
     */
    private final int maxResults;

    private FileOpsToolLoader(Builder builder) {
        this.workspace = builder.workspace;
        this.maxResults = builder.maxResults;
    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {
        final List<FunctionTool> tools = List.of(
                info(),
                delete(),
                move(),
                create(),
                list(),
                search()
        );

        final var stages = tools.stream()
                .map(tool -> toolbox.register(tool.meta().name(), tool))
                .toList();

        return CompletableFutureUtils.allOf(stages);
    }

    @Override
    public void close() {
        // 无资源需要释放
    }

    // ==================== 工具方法 ====================

    /**
     * file$info 工具
     */
    private FunctionTool info() {
        return FunctionTool.newBuilder()
                .name("file$info")
                .description("""
                        获取文件或目录的详细信息。
                        
                        【使用场景】
                        - 查看文件属性（大小、类型、权限等）
                        - 检查文件是否存在
                        - 获取最后修改时间
                        
                        【返回结果】
                        - name: 文件/目录名
                        - type: 类型（file/directory）
                        - size: 文件大小（字节）
                        - last_modified: 最后修改时间
                        - readable/writable: 读写权限
                        """)
                .parameterType(InfoSpec.class)
                .<InfoSpec>function((caller, spec) -> {
                    try {
                        final Path filePath = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(filePath)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        final var attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                        final var info = Map.of(
                                "name", filePath.getFileName().toString(),
                                "type", attrs.isDirectory() ? "directory" : "file",
                                "size", attrs.size(),
                                "last_modified", attrs.lastModifiedTime().toString(),
                                "readable", Files.isReadable(filePath),
                                "writable", Files.isWritable(filePath)
                        );

                        return Result.success(info);

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "读取文件信息失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * file$delete 工具
     */
    private FunctionTool delete() {
        return FunctionTool.newBuilder()
                .name("file$delete")
                .description("""
                        删除文件或空目录。
                        
                        【使用场景】
                        - 删除临时文件
                        - 清理不需要的文件
                        - 删除空目录
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        
                        【注意事项】
                        - 只能删除文件或空目录
                        - 非空目录无法删除，需要先清空内容
                        - 删除操作不可恢复，请谨慎操作
                        """)
                .parameterType(DeleteSpec.class)
                .<DeleteSpec>function((caller, spec) -> {
                    try {
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(targetPath)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        if (Files.isDirectory(targetPath)) {
                            // 检查是否为空目录
                            try (final var stream = Files.list(targetPath)) {
                                if (stream.findAny().isPresent()) {
                                    return Result.error("DIRECTORY_NOT_EMPTY",
                                            "目录非空，无法删除：" + spec.path());
                                }
                            }
                        }

                        Files.delete(targetPath);

                        final var result = Map.of(
                                "success", true,
                                "message", "Deleted: " + spec.path()
                        );
                        return Result.success(result);

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "删除失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * file$move 工具
     */
    private FunctionTool move() {
        return FunctionTool.newBuilder()
                .name("file$move")
                .description("""
                        移动文件或目录到另一个位置，或重命名文件/目录。
                        
                        【使用场景】
                        - 重命名文件或目录
                        - 移动文件到其他目录
                        - 整理文件结构
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        
                        【注意事项】
                        - 如果目标已存在，操作会失败
                        - 源和目标必须在 workspace 范围内
                        - 跨目录移动时，目标父目录必须存在
                        """)
                .parameterType(MoveSpec.class)
                .<MoveSpec>function((caller, spec) -> {
                    try {
                        final Path sourcePath = FileUtils.checkPathEscape(workspace, spec.source());
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.target());

                        if (!Files.exists(sourcePath)) {
                            return Result.error("FILE_NOT_FOUND", "源文件不存在：" + spec.source());
                        }

                        if (Files.exists(targetPath)) {
                            return Result.error("TARGET_EXISTS", "目标已存在：" + spec.target());
                        }

                        // 确保目标父目录存在
                        final Path parentDir = targetPath.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            return Result.error("PARENT_NOT_FOUND", 
                                    "目标父目录不存在：" + workspace.relativize(parentDir));
                        }

                        Files.move(sourcePath, targetPath);

                        final var result = Map.of(
                                "success", true,
                                "message", "Moved: %s -> %s".formatted(spec.source(), spec.target())
                        );
                        return Result.success(result);

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "移动失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * file$create 工具
     */
    private FunctionTool create() {
        return FunctionTool.newBuilder()
                .name("file$create")
                .description("""
                        创建文件或目录（统一入口）。
                        
                        【使用场景】
                        - 创建新的文本文件
                        - 创建目录结构
                        - 写入初始文件内容
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        - path: 创建的路径（相对于 workspace）
                        - type: 创建的类型（file/directory）
                        
                        【注意事项】
                        - 文件已存在时会覆盖内容，请谨慎操作
                        - 目录已存在时返回成功（幂等操作）
                        - 适合创建文本文件，不适合二进制文件
                        """)
                .parameterType(CreateSpec.class)
                .<CreateSpec>function((caller, spec) -> {
                    try {
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.path());

                        // 确保父目录存在
                        if (spec.parents()) {
                            final Path parentDir = targetPath.getParent();
                            if (parentDir != null && !Files.exists(parentDir)) {
                                Files.createDirectories(parentDir);
                            }
                        }

                        switch (spec.type()) {
                            case FILE -> {
                                // 创建文件
                                if (spec.content() != null) {
                                    Files.writeString(targetPath, spec.content(), CREATE, TRUNCATE_EXISTING);
                                } else {
                                    // 创建空文件
                                    if (!Files.exists(targetPath)) {
                                        Files.createFile(targetPath);
                                    }
                                }

                                final var result = Map.of(
                                        "success", true,
                                        "message", "File created: " + workspace.relativize(targetPath),
                                        "path", workspace.relativize(targetPath).toString(),
                                        "type", "file"
                                );
                                return Result.success(result);
                            }

                            case DIRECTORY -> {
                                // 创建目录
                                if (spec.parents()) {
                                    Files.createDirectories(targetPath);
                                } else {
                                    try {
                                        Files.createDirectory(targetPath);
                                    } catch (FileAlreadyExistsException e) {
                                        // 目录已存在，视为成功（幂等）
                                    }
                                }

                                final var result = Map.of(
                                        "success", true,
                                        "message", "Directory created: " + workspace.relativize(targetPath),
                                        "path", workspace.relativize(targetPath).toString(),
                                        "type", "directory"
                                );
                                return Result.success(result);
                            }

                            default -> {
                                return Result.error("INVALID_TYPE", "无效的创建类型：" + spec.type());
                            }
                        }

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "创建失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * file$list 工具
     */
    private FunctionTool list() {
        return FunctionTool.newBuilder()
                .name("file$list")
                .description("""
                        列出指定目录下的文件和子目录。
                        
                        【使用场景】
                        - 查看目录结构
                        - 确认文件是否存在
                        - 浏览项目组织
                        
                        【返回结果】
                        - items: 文件和目录名称列表
                        - count: 实际返回的数量
                        - truncated: 是否被截断
                        - hint: 如果被截断，提供补救建议
                        """)
                .parameterType(ListSpec.class)
                .<ListSpec>function((caller, spec) -> {
                    try {
                        final Path dirPath = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.isDirectory(dirPath)) {
                            return Result.error("NOT_DIRECTORY", "不是目录：" + spec.path());
                        }

                        final int maxReturn = spec.limit() != null
                                ? Math.min(spec.limit(), maxResults)
                                : DEFAULT_MAX_RESULTS;

                        final var items = new ArrayList<String>();
                        int totalCount = 0;
                        boolean hasMore = false;

                        try (final var stream = Files.list(dirPath)) {
                            final var iterator = stream.iterator();

                            while (iterator.hasNext()) {
                                totalCount++;
                                final Path entry = iterator.next();

                                if (items.size() < maxReturn) {
                                    items.add(workspace.relativize(entry).toString());
                                } else {
                                    hasMore = true;
                                    break;
                                }
                            }
                        }

                        final String hint = hasMore 
                                ? buildListTruncateHint(spec.path(), items.size(), maxReturn, totalCount) 
                                : null;

                        final var result = new ListResult(
                                items.stream().map(Object.class::cast).toList(),
                                items.size(),
                                hasMore,
                                maxReturn,
                                hint
                        );

                        return Result.success(result);

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "列出目录失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * file$search 工具
     */
    private FunctionTool search() {
        return FunctionTool.newBuilder()
                .name("file$search")
                .description("""
                        在指定目录下搜索匹配的文件，支持简单列表和详细信息两种模式。
                        
                        【使用场景】
                        - 查找特定类型的文件（如 *.java, *.md）
                        - 按文件名模式搜索
                        - 获取目录详细列表（设置 pattern="*" 且 detail=true）
                        
                        【返回结果】
                        - items: 匹配的文件列表
                          * 简单模式：字符串数组 ["src/main.java", ...]
                          * 详细模式：对象数组 [{"name":"main.java","type":"file",...}, ...]
                        - count: 实际返回的数量
                        - truncated: 是否被截断
                        - hint: 如果被截断，提供详细的补救建议
                        """)
                .parameterType(SearchSpec.class)
                .<SearchSpec>function((caller, spec) -> {
                    try {
                        final Path searchPath = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.isDirectory(searchPath)) {
                            return Result.error("NOT_DIRECTORY", "不是目录：" + spec.path());
                        }

                        final int maxReturn = spec.limit() != null
                                ? Math.min(spec.limit(), maxResults)
                                : DEFAULT_MAX_RESULTS;

                        final var matcher = FileSystems.getDefault()
                                .getPathMatcher("glob:" + spec.pattern());

                        final var items = new ArrayList<>();
                        final AtomicBoolean hasMore = new AtomicBoolean(false);

                        Consumer<Path> collector = (p) -> {
                            if (items.size() < maxReturn) {
                                if (spec.detail()) {
                                    try {
                                        final var attrs = Files.readAttributes(p, BasicFileAttributes.class);
                                        final var info = Map.of(
                                                "name", p.getFileName().toString(),
                                                "path", workspace.relativize(p).toString(),
                                                "type", attrs.isDirectory() ? "directory" : "file",
                                                "size", attrs.size(),
                                                "last_modified", attrs.lastModifiedTime().toString()
                                        );
                                        items.add(info);
                                    } catch (IOException e) {
                                        // 跳过无法读取的文件
                                    }
                                } else {
                                    items.add(workspace.relativize(p).toString());
                                }
                            } else {
                                hasMore.set(true);
                            }
                        };

                        if (spec.recursive()) {
                            try (final var stream = Files.walk(searchPath)) {
                                stream.filter(p -> matcher.matches(p.getFileName()))
                                        .forEach(collector);
                            }
                        } else {
                            try (final var stream = Files.list(searchPath)) {
                                stream.filter(p -> matcher.matches(p.getFileName()))
                                        .forEach(collector);
                            }
                        }

                        final String hint = hasMore.get()
                                ? buildSearchTruncateHint(spec, items.size(), maxReturn)
                                : null;

                        final var result = new ListResult(
                                items,
                                items.size(),
                                hasMore.get(),
                                maxReturn,
                                hint
                        );

                        return Result.success(result);

                    } catch (SecurityException ex) {
                        return Result.error("ACCESS_DENIED", ex.getMessage());
                    } catch (IOException ex) {
                        return Result.error("IO_ERROR", "搜索失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建 list 截断提示
     */
    private String buildListTruncateHint(String path, int returned, int maxReturn, int total) {
        return """
                ⚠️ 结果已截断：目录中包含 %d 个条目，仅返回前 %d 个。
                
                【补救建议】
                1. 使用更具体的搜索：file$search(path="%s", pattern="*.java")
                2. 进入子目录查看：file$list(path="%s/<子目录名>")
                3. 增加 limit 参数（最大1000）：file$list(path="%s", limit=%d)
                
                【当前状态】
                - 总条目数：%d
                - 已返回：%d
                - 限制上限：%d
                """.formatted(total, returned, path, path, path, maxReturn * 2, total, returned, maxReturn);
    }

    /**
     * 构建 search 截断提示
     */
    private String buildSearchTruncateHint(SearchSpec spec, int returned, int maxReturn) {
        return """
                ⚠️ 搜索结果已截断：找到超过 %d 个匹配文件，仅返回前 %d 个。
                
                【补救建议】
                1. 缩小搜索范围：指定更具体的目录路径
                   - 当前：path="%s"
                   - 建议：path="%s/src" 或 path="%s/src/main"
                
                2. 使用更精确的 pattern：
                   - 当前：pattern="%s"
                   - 建议：pattern="*Controller.java" 或 pattern="Test*.java"
                
                3. 关闭递归搜索（如果不需要子目录）：
                   - 设置 recursive=false
                
                4. 增加 limit 参数（最大1000）：
                   - file$search(path="%s", pattern="%s", limit=%d)
                
                【当前状态】
                - 已返回：%d 个文件
                - 限制上限：%d
                - 递归搜索：%s
                - 匹配模式：%s
                """.formatted(
                returned + 1, returned,
                spec.path(), spec.path(), spec.path(),
                spec.pattern(),
                spec.path(), spec.pattern(), maxReturn * 2,
                returned, maxReturn,
                spec.recursive() ? "是" : "否",
                spec.pattern()
        );
    }

    // ==================== Spec 数据结构 ====================

    /**
     * file$info 参数
     */
    record InfoSpec(
            @JsonPropertyDescription("文件或目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path
    ) {
    }

    /**
     * file$delete 参数
     */
    record DeleteSpec(
            @JsonPropertyDescription("要删除的文件或空目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path
    ) {
    }

    /**
     * file$move 参数
     */
    record MoveSpec(
            @JsonPropertyDescription("源文件的相对路径")
            @JsonProperty(value = "source", required = true)
            String source,

            @JsonPropertyDescription("目标文件的相对路径")
            @JsonProperty(value = "target", required = true)
            String target
    ) {
    }

    /**
     * 创建类型枚举
     */
    enum CreateType {
        /** 创建文件 */
        FILE,
        /** 创建目录 */
        DIRECTORY
    }

    /**
     * file$create 参数
     */
    record CreateSpec(
            @JsonPropertyDescription("要创建的文件或目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("创建类型：FILE-文件，DIRECTORY-目录")
            @JsonProperty(value = "type", required = true)
            CreateType type,

            @JsonPropertyDescription("文件内容（仅当 type=FILE 时有效，不提供则创建空文件）")
            @JsonProperty("content")
            String content,

            @JsonPropertyDescription("是否自动创建所有必需的父目录")
            @JsonProperty("parents")
            boolean parents
    ) {
    }

    /**
     * file$list 参数
     */
    record ListSpec(
            @JsonPropertyDescription("要列出的目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("最大返回条目数（可选，默认100，最大1000）")
            @JsonProperty("limit")
            Integer limit
    ) {
    }

    /**
     * file$search 参数
     */
    record SearchSpec(
            @JsonPropertyDescription("搜索起始目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("文件名匹配模式（支持通配符 * 和 ?）")
            @JsonProperty(value = "pattern", required = true)
            String pattern,

            @JsonPropertyDescription("是否递归搜索子目录")
            @JsonProperty("recursive")
            boolean recursive,

            @JsonPropertyDescription("是否返回详细信息（包含名称、类型、大小、修改时间）")
            @JsonProperty("detail")
            boolean detail,

            @JsonPropertyDescription("最大返回条目数（可选，默认100，最大1000）")
            @JsonProperty("limit")
            Integer limit
    ) {
    }

    // ==================== 结果数据结构 ====================

    /**
     * 统一的列表/搜索结果返回结构
     */
    record ListResult(
            @JsonProperty("items")
            List<Object> items,

            @JsonProperty("count")
            int count,

            @JsonProperty("truncated")
            boolean truncated,

            @JsonProperty("max_results")
            int maxResults,

            @JsonProperty("hint")
            String hint
    ) {
    }

    /**
     * 统一的结果封装
     */
    record Result(
            @JsonProperty("error")
            String error,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Object data
    ) {
        /**
         * 创建成功结果
         */
        static Result success(Object data) {
            return new Result(null, null, data);
        }

        /**
         * 创建错误结果
         */
        static Result error(String error, String message) {
            return new Result(error, message, null);
        }
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<FileOpsToolLoader, Builder> {
        private Path workspace;
        private int maxResults = DEFAULT_MAX_RESULTS;

        /**
         * 设置工作区根路径
         *
         * @param workspace 工作区目录路径
         * @return this
         */
        public Builder workspace(Path workspace) {
            Objects.requireNonNull(workspace, "workspace must not be null");
            if (!Files.isDirectory(workspace)) {
                throw new IllegalArgumentException("workspace must be an existing directory: " + workspace);
            }
            this.workspace = workspace.toAbsolutePath().normalize();
            return this;
        }

        /**
         * 设置工作区根路径（字符串形式）
         *
         * @param workspace 工作区目录路径字符串
         * @return this
         */
        public Builder workspace(String workspace) {
            return workspace(Paths.get(workspace));
        }

        /**
         * 设置最大返回条目数
         *
         * @param maxResults 最大返回数（1-1000）
         * @return this
         */
        public Builder maxResults(int maxResults) {
            if (maxResults < 1 || maxResults > 1000) {
                throw new IllegalArgumentException(
                        "maxResults must be between 1 and 1000, got: " + maxResults
                );
            }
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public FileOpsToolLoader build() {
            Objects.requireNonNull(workspace, "workspace must be set before building");
            return new FileOpsToolLoader(this);
        }
    }

}



