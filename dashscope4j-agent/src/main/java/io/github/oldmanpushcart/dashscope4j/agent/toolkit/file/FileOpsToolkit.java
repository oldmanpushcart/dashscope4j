package io.github.oldmanpushcart.dashscope4j.agent.toolkit.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * 文件操作工具包
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
public class FileOpsToolkit implements Toolkit {

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

    /**
     * 工具集
     */
    private final List<Tool> tools;

    private FileOpsToolkit(Builder builder) {
        Objects.requireNonNull(builder.workspace, "workspace must not be null!");
        CheckUtils.require(builder.workspace, Files::isDirectory, "workspace must be an existing directory: %s".formatted(builder.workspace));
        CheckUtils.require(builder.workspace, Files::isReadable, "workspace must be readable: %s".formatted(builder.workspace));
        CheckUtils.require(builder.workspace, v -> !builder.readOnly && Files.isWritable(v), "workspace must be writable: %s".formatted(builder.workspace));
        CheckUtils.require(builder.maxResults, t -> t > 0, "maxResults must be greater than 0, current is: %s".formatted(builder.maxResults));
        this.workspace = builder.workspace.toAbsolutePath().normalize();
        this.maxResults = builder.maxResults;
        this.tools = builder.readOnly
                ? List.of(info(), list(), search())
                : List.of(info(), delete(), move(), copy(), touch(), mkdir(), list(), search());
    }

    @Override
    public @NonNull Iterator<Tool> iterator() {
        return tools.iterator();
    }

    // ==================== Builder ====================

    public static FileOpsToolkit create() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<FileOpsToolkit, Builder> {
        private Path workspace = Path.of("./");
        private int maxResults = DEFAULT_MAX_RESULTS;
        private boolean readOnly = false;

        /**
         * 设置工作区根路径
         *
         * @param workspace 工作区目录路径
         * @return this
         */
        public Builder workspace(Path workspace) {
            this.workspace = workspace;
            return this;
        }

        /**
         * 设置最大返回条目数
         *
         * @param maxResults 最大返回数（1-1000）
         * @return this
         */
        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        /**
         * 设置是否为只读模式
         * <p>
         * 当设置为 true 时，只会安装读取相关的工具（file$info、file$list、file$search），
         * 不会安装写入、删除和移动工具（file$delete、file$move、file$touch、file$mkdir）。
         *
         * @param readOnly 是否只读
         * @return this
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        @Override
        public FileOpsToolkit build() {
            Objects.requireNonNull(workspace, "workspace must be set before building");
            return new FileOpsToolkit(this);
        }
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
                            throw ToolExecutionException.callFailed(
                                    "file$info",
                                    "File not found: " + spec.path(),
                                    "Verify the file path is correct and the file exists in the workspace."
                            );
                        }

                        final var attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                        return Map.of(
                                "name", filePath.getFileName().toString(),
                                "type", attrs.isDirectory() ? "directory" : "file",
                                "size", attrs.size(),
                                "last_modified", attrs.lastModifiedTime().toString(),
                                "readable", Files.isReadable(filePath),
                                "writable", Files.isWritable(filePath)
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$info",
                                "Access denied: " + spec.path(),
                                "Check file permissions and ensure the file is within the workspace.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$info",
                                "Failed to read file attributes: " + spec.path(),
                                "The file may be corrupted or inaccessible. Try again or check system logs.",
                                ex
                        );
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
                            throw ToolExecutionException.callFailed(
                                    "file$delete",
                                    "File not found: " + spec.path(),
                                    "Verify the file path is correct and the file exists in the workspace."
                            );
                        }

                        if (Files.isDirectory(targetPath)) {
                            // 检查是否为空目录
                            try (final var stream = Files.list(targetPath)) {
                                if (stream.findAny().isPresent()) {
                                    throw ToolExecutionException.callFailed(
                                            "file$delete",
                                            "Directory not empty: " + spec.path(),
                                            "Remove all files and subdirectories first, or use a recursive delete tool."
                                    );
                                }
                            }
                        }

                        Files.delete(targetPath);

                        return Map.of(
                                "success", true,
                                "message", "Deleted: " + spec.path()
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$delete",
                                "Access denied: " + spec.path(),
                                "Check file permissions and ensure you have delete access.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$delete",
                                "Failed to delete: " + spec.path(),
                                "The file may be in use or locked. Try again later.",
                                ex
                        );
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
                            throw ToolExecutionException.callFailed(
                                    "file$move",
                                    "Source file not found: " + spec.source(),
                                    "Verify the source path is correct and the file exists."
                            );
                        }

                        if (Files.exists(targetPath)) {
                            throw ToolExecutionException.callFailed(
                                    "file$move",
                                    "Target already exists: " + spec.target(),
                                    "Choose a different target path or delete the existing file first."
                            );
                        }

                        // 确保目标父目录存在
                        final Path parentDir = targetPath.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            throw ToolExecutionException.callFailed(
                                    "file$move",
                                    "Parent directory not found: " + workspace.relativize(parentDir),
                                    "Create the parent directory first or choose a different target path."
                            );
                        }

                        Files.move(sourcePath, targetPath);

                        return Map.of(
                                "success", true,
                                "message", "Moved: %s -> %s".formatted(spec.source(), spec.target())
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$move",
                                "Access denied: cannot move " + spec.source() + " to " + spec.target(),
                                "Check permissions for both source and target locations.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$move",
                                "Failed to move: " + spec.source() + " -> " + spec.target(),
                                "The file may be in use or the operation is not supported.",
                                ex
                        );
                    }
                })
                .build();
    }

    /**
     * file$copy 工具
     */
    private FunctionTool copy() {
        return FunctionTool.newBuilder()
                .name("file$copy")
                .description("""
                        复制文件或目录到另一个位置。
                        
                        【使用场景】
                        - 备份文件
                        - 创建文件副本
                        - 复制文件到其他目录
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        
                        【注意事项】
                        - 如果目标已存在且 overwrite=false，操作会失败
                        - 源和目标必须在 workspace 范围内
                        - 跨目录复制时，目标父目录必须存在
                        - 默认不覆盖已存在的文件，设置 overwrite=true 以覆盖
                        """)
                .parameterType(CopySpec.class)
                .<CopySpec>function((caller, spec) -> {
                    try {
                        final Path sourcePath = FileUtils.checkPathEscape(workspace, spec.source());
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.target());

                        if (!Files.exists(sourcePath)) {
                            throw ToolExecutionException.callFailed(
                                    "file$copy",
                                    "Source file not found: " + spec.source(),
                                    "Verify the source path is correct and the file exists."
                            );
                        }

                        if (Files.exists(targetPath) && !spec.overwrite()) {
                            throw ToolExecutionException.callFailed(
                                    "file$copy",
                                    "Target already exists: " + spec.target(),
                                    "Set overwrite=true to replace the existing file, or choose a different target path."
                            );
                        }

                        // 确保目标父目录存在
                        final Path parentDir = targetPath.getParent();
                        if (parentDir != null && !Files.exists(parentDir)) {
                            throw ToolExecutionException.callFailed(
                                    "file$copy",
                                    "Parent directory not found: " + workspace.relativize(parentDir),
                                    "Create the parent directory first or choose a different target path."
                            );
                        }

                        // 执行复制
                        if (Files.isDirectory(sourcePath)) {
                            // 复制目录（递归）
                            copyDirectory(sourcePath, targetPath, spec.overwrite());
                        } else {
                            // 复制文件
                            Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }

                        return Map.of(
                                "success", true,
                                "message", "Copied: %s -> %s".formatted(spec.source(), spec.target())
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$copy",
                                "Access denied: cannot copy " + spec.source() + " to " + spec.target(),
                                "Check permissions for both source and target locations.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$copy",
                                "Failed to copy: " + spec.source() + " -> " + spec.target(),
                                "The file may be in use or the operation is not supported.",
                                ex
                        );
                    }
                })
                .build();
    }

    /**
     * file$touch 工具 - 创建文件
     */
    private FunctionTool touch() {
        return FunctionTool.newBuilder()
                .name("file$touch")
                .description("""
                        创建新文件或更新现有文件的时间戳。
                        
                        【使用场景】
                        - 创建新的文本文件
                        - 写入初始文件内容
                        - 创建空文件作为占位符
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        - path: 创建的路径（相对于 workspace）
                        
                        【注意事项】
                        - 文件已存在时会覆盖内容，请谨慎操作
                        - 适合创建文本文件，不适合二进制文件
                        - 如果父目录不存在且 parents=true，会自动创建
                        """)
                .parameterType(TouchSpec.class)
                .<TouchSpec>function((caller, spec) -> {
                    try {
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.path());

                        // 确保父目录存在
                        if (spec.parents()) {
                            final Path parentDir = targetPath.getParent();
                            if (parentDir != null && !Files.exists(parentDir)) {
                                Files.createDirectories(parentDir);
                            }
                        }

                        // 创建或更新文件
                        if (spec.content() != null) {
                            Files.writeString(targetPath, spec.content(), CREATE, TRUNCATE_EXISTING);
                        } else {
                            // 创建空文件
                            if (!Files.exists(targetPath)) {
                                Files.createFile(targetPath);
                            }
                        }

                        return Map.of(
                                "success", true,
                                "message", "File created: " + workspace.relativize(targetPath),
                                "path", workspace.relativize(targetPath).toString()
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$touch",
                                "Access denied: " + spec.path(),
                                "Check file permissions and ensure you have write access.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$touch",
                                "Failed to create file: " + spec.path(),
                                "The disk may be full or the path is invalid.",
                                ex
                        );
                    }
                })
                .build();
    }

    /**
     * file$mkdir 工具 - 创建目录
     */
    private FunctionTool mkdir() {
        return FunctionTool.newBuilder()
                .name("file$mkdir")
                .description("""
                        创建新目录。
                        
                        【使用场景】
                        - 创建目录结构
                        - 组织项目文件
                        - 创建工作区子目录
                        
                        【返回结果】
                        - success: 是否成功
                        - message: 操作结果提示
                        - path: 创建的目录路径（相对于 workspace）
                        
                        【注意事项】
                        - 目录已存在时返回成功（幂等操作）
                        - 如果 parents=true，会创建所有必需的父目录
                        - 类似 Unix 的 mkdir -p 命令
                        """)
                .parameterType(MkdirSpec.class)
                .<MkdirSpec>function((caller, spec) -> {
                    try {
                        final Path targetPath = FileUtils.checkPathEscape(workspace, spec.path());

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

                        return Map.of(
                                "success", true,
                                "message", "Directory created: " + workspace.relativize(targetPath),
                                "path", workspace.relativize(targetPath).toString()
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$mkdir",
                                "Access denied: " + spec.path(),
                                "Check file permissions and ensure you have write access.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$mkdir",
                                "Failed to create directory: " + spec.path(),
                                "The disk may be full or the path is invalid.",
                                ex
                        );
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
                            throw ToolExecutionException.callFailed(
                                    "file$list",
                                    "Not a directory: " + spec.path(),
                                    "Provide a valid directory path. Use file$info to check the path type."
                            );
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

                        return new ListResult(
                                items.stream().map(Object.class::cast).toList(),
                                items.size(),
                                hasMore,
                                maxReturn,
                                hint
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$list",
                                "Access denied: " + spec.path(),
                                "Check directory permissions and ensure it is within the workspace.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$list",
                                "Failed to list directory: " + spec.path(),
                                "The directory may be inaccessible or corrupted.",
                                ex
                        );
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
                            throw ToolExecutionException.callFailed(
                                    "file$search",
                                    "Not a directory: " + spec.path(),
                                    "Provide a valid directory path. Use file$info to check the path type."
                            );
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

                        return new ListResult(
                                items,
                                items.size(),
                                hasMore.get(),
                                maxReturn,
                                hint
                        );

                    } catch (SecurityException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$search",
                                "Access denied: " + spec.path(),
                                "Check directory permissions and ensure it is within the workspace.",
                                ex
                        );
                    } catch (IOException ex) {
                        throw ToolExecutionException.callFailed(
                                "file$search",
                                "Failed to search files in: " + spec.path(),
                                "The directory may be inaccessible or corrupted.",
                                ex
                        );
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 递归复制目录
     */
    private void copyDirectory(Path source, Path target, boolean overwrite) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        try (var stream = Files.list(source)) {
            stream.forEach(entry -> {
                try {
                    final Path targetEntry = target.resolve(entry.getFileName().toString());
                    if (Files.isDirectory(entry)) {
                        // 递归复制子目录
                        copyDirectory(entry, targetEntry, overwrite);
                    } else {
                        // 复制文件
                        if (Files.exists(targetEntry) && !overwrite) {
                            throw new IOException("Target already exists: " + workspace.relativize(targetEntry));
                        }
                        Files.copy(entry, targetEntry, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + entry, e);
                }
            });
        }
    }

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
     * file$copy 参数
     */
    record CopySpec(
            @JsonPropertyDescription("源文件或目录的相对路径")
            @JsonProperty(value = "source", required = true)
            String source,

            @JsonPropertyDescription("目标文件或目录的相对路径")
            @JsonProperty(value = "target", required = true)
            String target,

            @JsonPropertyDescription("是否覆盖已存在的目标（默认 false）")
            @JsonProperty("overwrite")
            boolean overwrite
    ) {
    }

    /**
     * file$touch 参数
     */
    record TouchSpec(
            @JsonPropertyDescription("要创建的文件相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("文件内容（不提供则创建空文件）")
            @JsonProperty("content")
            String content,

            @JsonPropertyDescription("是否自动创建所有必需的父目录")
            @JsonProperty("parents")
            boolean parents
    ) {
    }

    /**
     * file$mkdir 参数
     */
    record MkdirSpec(
            @JsonPropertyDescription("要创建的目录相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("是否自动创建所有必需的父目录（类似 mkdir -p）")
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

}





