package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 文件操作工具加载器
 * 提供基础文件操作和内容检索功能
 */
public class FileOpsToolLoader implements Repository.Loader<String, Tool> {

    public static final Repository.Loader<String, Tool> INSTANCE = new FileOpsToolLoader();

    private static final int MAX_READ_LINES = 10000; // 最大读取行数
    private static final int DEFAULT_LIMIT = 200; // 默认返回行数
    private static final int MAX_SEARCH_RESULTS = 50; // 最大搜索结果数
    private static final int MAX_GREP_RESULTS = 20; // 最大 grep 结果数
    private static final int GREP_CONTEXT_LINES = 1; // grep 上下文行数
    private static final int MAX_DIFF_LINES = 1000; // diff 最大比较行数

    // 缓存工作目录根路径，避免重复创建
    private static final Path WORKSPACE_ROOT = Paths.get("").toAbsolutePath().normalize();

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {
        return CompletableFuture.completedStage(null)
                .thenAccept(unused -> List.of(
                        listDirectory(),
                        readFile(),
                        writeFile(),
                        appendFile(),
                        deleteFile(),
                        moveFile(),
                        createDirectory(),
                        searchFiles(),
                        grepContent(),
                        getFileInfo(),
                        diffFiles()
                ).forEach(tool -> updater.upsert(tool.meta().name(), tool)));
    }

    @Override
    public void close() {
        // 无资源需要关闭
    }

    /**
     * 列出目录下的文件和子目录
     */
    public static FunctionTool listDirectory() {
        return FunctionTool.newBuilder()
                .name("file$list_directory")
                .description("""
                        列出指定目录下的文件和子目录（类似 Linux 的 ls 命令）。
                        
                        【使用场景】
                        - 查看目录下有哪些文件和子目录
                        - 探索项目结构或目录层次
                        - 查找特定文件的位置
                        
                        【参数说明】
                        - path: 目录的相对路径（必需），例如："src/main/java"
                        - recursive: 是否递归列出子目录（可选，默认 false）
                          * false: 只列出一级目录
                          * true: 最多递归 3 层深度
                        
                        【返回结果】
                        - 包含文件名/目录名、类型（file/directory）、大小（字节）的列表
                        
                        【注意事项】
                        - 只能访问工作目录内的文件
                        - 不支持绝对路径
                        - 递归模式下最大深度限制为 3 层
                        """)
                .parameterType(ListDirectorySpec.class)
                .<ListDirectorySpec>function((caller, spec) -> {
                    try {
                        Path resolved = resolveAndValidate(spec.path());
                        if (!Files.exists(resolved)) {
                            return errorResult("DIRECTORY_NOT_FOUND", "目录不存在：" + spec.path());
                        }
                        if (!Files.isDirectory(resolved)) {
                            return errorResult("NOT_A_DIRECTORY", "路径不是目录：" + spec.path());
                        }

                        List<FileEntry> entries = new ArrayList<>();
                        int maxDepth = spec.recursive() ? 3 : 1;

                        Files.walkFileTree(resolved, new SimpleFileVisitor<Path>() {
                            @Override
                            public @NonNull FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) throws IOException {
                                // 计算当前深度（相对于根目录）
                                int depth = resolved.relativize(dir).getNameCount();

                                if (depth >= maxDepth) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                                // 在 visitFile 中的肯定是文件，不需要再检查 isDirectory
                                entries.add(new FileEntry(
                                        relativize(file).toString(),
                                        "file",
                                        attrs.size() // 直接使用 attrs，避免再次 IO
                                ));
                                return FileVisitResult.CONTINUE;
                            }
                        });

                        return successResult(entries);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "读取目录失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 读取文本文件内容
     */
    public static FunctionTool readFile() {
        return FunctionTool.newBuilder()
                .name("file$read_file")
                .description("""
                        读取文本文件的内容，支持分页读取（适用于大文件）。
                        
                        【使用场景】
                        - 查看源代码文件内容
                        - 读取配置文件
                        - 查看日志文件
                        - 阅读文本文档
                        
                        【参数说明】
                        - path: 文件的相对路径（必需），例如："README.md"
                        - limit_lines: 每次读取的最大行数（可选，默认 200，最大 10000）
                        - offset_lines: 跳过的行数（可选，默认 0），用于分页
                        
                        【返回结果】
                        - text: 实际读取的文本内容
                        - lines_returned: 本次返回的行数
                        - total_lines: 文件总行数
                        - has_more: 是否还有更多内容（true 表示可以继续读取）
                        
                        【注意事项】
                        - 仅支持文本文件，无法读取图片、视频等二进制文件
                        - 大文件建议分批读取（使用 offset_lines 和 limit_lines）
                        - 默认每次最多读取 200 行，避免返回内容过长
                        - 不能读取目录
                        """)
                .parameterType(ReadFileSpec.class)
                .<ReadFileSpec>function((caller, spec) -> {
                    try {
                        Path resolved = resolveAndValidate(spec.path());
                        if (!Files.exists(resolved)) {
                            return errorResult("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }
                        if (Files.isDirectory(resolved)) {
                            return errorResult("IS_A_DIRECTORY", "路径是目录而非文件：" + spec.path());
                        }

                        // 检测是否为二进制文件
                        if (isBinaryFile(resolved)) {
                            return errorResult("BINARY_FILE", "无法读取二进制文件：" + spec.path());
                        }

                        int offset = Math.max(0, spec.offsetLines());
                        int limit = spec.limitLines() > 0 ? Math.min(spec.limitLines(), MAX_READ_LINES) : DEFAULT_LIMIT;

                        // 使用流式读取，避免大文件 OOM，同时统计总行数
                        List<String> content = new ArrayList<>();
                        int totalLines = 0;

                        try (BufferedReader reader = Files.newBufferedReader(resolved, Charset.defaultCharset())) {
                            // 跳过 offset 行（高效实现）
                            if (offset > 0) {
                                long skipped = reader.skip(offset);
                            }

                            // 读取 limit 行
                            String line;
                            int count = 0;
                            while (count < limit && (line = reader.readLine()) != null) {
                                content.add(line);
                                count++;
                            }

                            // 计算总行数（如果需要考虑 hasMore）
                            if (count == limit) {
                                // 可能还有更多行，继续统计
                                while ((line = reader.readLine()) != null) {
                                    totalLines++;
                                }
                                totalLines += offset + count;
                            } else {
                                // 已经读完，totalLines = offset + 实际读取的行数
                                totalLines = offset + count;
                            }
                        }

                        boolean hasMore = totalLines > offset + content.size();
                        String text = String.join("\n", content);

                        return successResult(new ReadResult(text, content.size(), totalLines, hasMore));

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "读取文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 创建新文件或覆盖现有文件
     */
    public static FunctionTool writeFile() {
        return FunctionTool.newBuilder()
                .name("file$write_file")
                .description("""
                        创建新文件或覆盖现有文件（需谨慎使用覆盖功能）。
                        
                        【使用场景】
                        - 创建新的源代码文件
                        - 生成配置文件
                        - 写入输出结果到文件
                        - 修改现有文件内容（需显式声明覆盖）
                        
                        【参数说明】
                        - path: 文件的相对路径（必需），例如："output/result.txt"
                        - content: 要写入的文件内容（必需，不能为空）
                        - overwrite: 是否覆盖现有文件（可选，默认 false）
                          * false（默认）: 如果文件已存在则报错
                          * true: 允许覆盖现有文件
                        
                        【返回结果】
                        - 成功消息："文件写入成功"
                        
                        【注意事项】
                        - 默认禁止覆盖现有文件，防止误操作
                        - 如需覆盖必须显式设置 overwrite=true
                        - 如果父目录不存在会自动创建
                        - 内容不能为空字符串
                        - 只能在工作目录内写入
                        """)
                .parameterType(WriteFileSpec.class)
                .<WriteFileSpec>function((caller, spec) -> {
                    try {
                        // 验证输入参数
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "文件内容不能为空");
                        }

                        Path resolved = resolveAndValidate(spec.path());

                        // 检查文件是否已存在
                        if (Files.exists(resolved) && !spec.overwrite()) {
                            return errorResult("FILE_EXISTS", "文件已存在，设置 overwrite=true 以覆盖：" + spec.path());
                        }

                        // 确保父目录存在
                        Path parent = resolved.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        Files.writeString(resolved, spec.content(), Charset.defaultCharset());
                        return successResult("文件写入成功");

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "写入文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 向文件末尾追加内容
     */
    public static FunctionTool appendFile() {
        return FunctionTool.newBuilder()
                .name("file$append_file")
                .description("""
                        向文件末尾追加内容（适合日志记录、累积数据等场景）。
                        
                        【使用场景】
                        - 向日志文件追加新的日志条目
                        - 在数据文件末尾添加新记录
                        - 累积输出结果
                        - 修改配置文件（添加配置项）
                        
                        【参数说明】
                        - path: 文件的相对路径（必需），例如："logs/app.log"
                        - content: 要追加的内容（必需，不能为空）
                        
                        【返回结果】
                        - 成功消息："内容追加成功"
                        
                        【注意事项】
                        - 内容总是添加到文件末尾
                        - 如果文件不存在会自动创建
                        - 如果父目录不存在会自动创建
                        - 内容不能为空字符串
                        - 不会覆盖现有内容
                        """)
                .parameterType(AppendFileSpec.class)
                .<AppendFileSpec>function((caller, spec) -> {
                    try {
                        // 验证输入参数
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "追加内容不能为空");
                        }

                        Path resolved = resolveAndValidate(spec.path());

                        // 确保父目录存在
                        Path parent = resolved.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        Files.writeString(resolved, spec.content(), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        return successResult("内容追加成功");

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "追加内容失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 删除指定文件或空目录
     */
    public static FunctionTool deleteFile() {
        return FunctionTool.newBuilder()
                .name("file$delete_file")
                .description("""
                        删除指定文件或空目录（安全删除，防止误删重要文件）。
                        
                        【使用场景】
                        - 删除临时文件
                        - 清理编译产物
                        - 移除不需要的配置文件
                        - 删除空目录
                        
                        【参数说明】
                        - path: 文件或目录的相对路径（必需），例如："temp/cache.txt"
                        
                        【返回结果】
                        - 成功消息："文件删除成功" 或 "空目录删除成功"
                        
                        【注意事项】
                        - ⚠️ 严禁删除非空目录（防止误删大量文件）
                        - 删除目录前必须先清空目录内容
                        - 只能删除工作目录内的文件
                        - 删除操作不可恢复，请谨慎使用
                        - 不支持通配符批量删除
                        """)
                .parameterType(DeleteFileSpec.class)
                .<DeleteFileSpec>function((caller, spec) -> {
                    try {
                        Path resolved = resolveAndValidate(spec.path());
                        if (!Files.exists(resolved)) {
                            return errorResult("PATH_NOT_FOUND", "路径不存在：" + spec.path());
                        }

                        if (Files.isDirectory(resolved)) {
                            try (var stream = Files.list(resolved)) {
                                if (stream.findAny().isPresent()) {
                                    return errorResult("DIRECTORY_NOT_EMPTY", "目录不为空，请先删除内部文件：" + spec.path());
                                }
                            }
                            Files.delete(resolved);
                            return successResult("空目录删除成功");
                        } else {
                            Files.delete(resolved);
                            return successResult("文件删除成功");
                        }

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "删除失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 重命名或移动文件
     */
    public static FunctionTool moveFile() {
        return FunctionTool.newBuilder()
                .name("file$move_file")
                .description("""
                        重命名文件或移动文件到新位置（原子操作优先）。
                        
                        【使用场景】
                        - 重命名文件
                        - 移动文件到其他目录
                        - 整理项目结构
                        - 备份文件（移动后改名）
                        
                        【参数说明】
                        - source: 源文件的相对路径（必需），例如："old_name.txt"
                        - destination: 目标文件的相对路径（必需），例如："new_name.txt"
                        
                        【返回结果】
                        - 成功消息："文件移动成功"
                        
                        【注意事项】
                        - 如果目标文件已存在会报错（防止覆盖）
                        - 如果目标目录不存在会自动创建
                        - 优先使用原子移动（保证数据一致性）
                        - 跨文件系统时降级为标准移动
                        - 源文件和目标文件都必须是相对路径
                        """)
                .parameterType(MoveFileSpec.class)
                .<MoveFileSpec>function((caller, spec) -> {
                    try {
                        Path source = resolveAndValidate(spec.source());
                        Path dest = resolveAndValidate(spec.destination());

                        if (!Files.exists(source)) {
                            return errorResult("SOURCE_NOT_FOUND", "源路径不存在：" + spec.source());
                        }

                        if (Files.exists(dest)) {
                            return errorResult("DESTINATION_EXISTS", "目标路径已存在：" + spec.destination());
                        }

                        // 确保目标父目录存在
                        Path destParent = dest.getParent();
                        if (destParent != null && !Files.exists(destParent)) {
                            Files.createDirectories(destParent);
                        }

                        // 优先尝试 ATOMIC_MOVE，失败则降级为标准移动
                        try {
                            Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
                        } catch (AtomicMoveNotSupportedException e) {
                            // 降级为非原子移动（跨文件系统等场景）
                            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return successResult("文件移动成功");

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "移动文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 创建目录
     */
    public static FunctionTool createDirectory() {
        return FunctionTool.newBuilder()
                .name("file$create_directory")
                .description("""
                        创建新目录（包括父目录，类似 mkdir -p）。
                        
                        【使用场景】
                        - 创建项目目录结构
                        - 建立分类文件夹
                        - 准备输出目录
                        - 组织文件结构
                        
                        【参数说明】
                        - path: 目录的相对路径（必需），例如："docs/api/v1"
                        
                        【返回结果】
                        - 成功消息："目录创建成功" 或 "目录已存在"
                        
                        【注意事项】
                        - 会自动创建所有必需的父目录
                        - 如果目录已存在不会报错，直接返回成功
                        - 如果路径已存在且是文件会报错
                        - 只能在工作目录内创建目录
                        """)
                .parameterType(CreateDirectorySpec.class)
                .<CreateDirectorySpec>function((caller, spec) -> {
                    try {
                        Path resolved = resolveAndValidate(spec.path());

                        if (Files.exists(resolved)) {
                            if (Files.isDirectory(resolved)) {
                                return successResult("目录已存在");
                            } else {
                                return errorResult("PATH_EXISTS_IS_FILE", "路径已存在且为文件：" + spec.path());
                            }
                        }

                        Files.createDirectories(resolved);
                        return successResult("目录创建成功");

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "创建目录失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 基于文件名/Glob 模式查找文件
     */
    public static FunctionTool searchFiles() {
        return FunctionTool.newBuilder()
                .name("file$search_files")
                .description("基于文件名/Glob 模式查找文件")
                .parameterType(SearchFilesSpec.class)
                .<SearchFilesSpec>function((caller, spec) -> {
                    try {
                        Path root = spec.root() != null ? resolveAndValidate(spec.root()) : Paths.get("");
                        if (!Files.isDirectory(root)) {
                            return errorResult("INVALID_ROOT", "根路径不是目录：" + spec.root());
                        }

                        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + spec.pattern());
                        List<String> results = new ArrayList<>();

                        Files.walkFileTree(root, new SimpleFileVisitor<>() {
                            @Override
                            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                                if (results.size() >= MAX_SEARCH_RESULTS) {
                                    return FileVisitResult.TERMINATE;
                                }
                                if (matcher.matches(file.getFileName())) {
                                    results.add(relativize(file).toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public @NonNull FileVisitResult visitFileFailed(@NonNull Path file, @NonNull IOException exc) {
                                return FileVisitResult.CONTINUE;
                            }
                        });

                        if (results.size() >= MAX_SEARCH_RESULTS) {
                            return warningResult("匹配结果过多，请缩小范围", results);
                        }

                        return successResult(results);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "搜索文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 在文件内容中搜索关键词或正则
     */
    public static FunctionTool grepContent() {
        return FunctionTool.newBuilder()
                .name("file$grep_content")
                .description("在文件内容中搜索关键词或正则表达式")
                .parameterType(GrepContentSpec.class)
                .<GrepContentSpec>function((caller, spec) -> {
                    try {
                        // 安全编译正则表达式
                        java.util.regex.Pattern pattern;
                        try {
                            String patternStr = spec.caseSensitive() ? spec.pattern() : "(?i)" + spec.pattern();
                            pattern = java.util.regex.Pattern.compile(patternStr);
                        } catch (java.util.regex.PatternSyntaxException e) {
                            return errorResult("INVALID_PATTERN", "无效的正则表达式：" + e.getMessage());
                        }

                        Path root = spec.root() != null ? resolveAndValidate(spec.root()) : Paths.get("");
                        if (!Files.isDirectory(root)) {
                            return errorResult("INVALID_ROOT", "根路径不是目录：" + spec.root());
                        }

                        // 空字符串视为无过滤
                        PathMatcher fileMatcher = (spec.filePattern() != null && !spec.filePattern().isEmpty())
                                ? FileSystems.getDefault().getPathMatcher("glob:" + spec.filePattern())
                                : path -> true;

                        List<GrepMatch> matches = new ArrayList<>();

                        Files.walkFileTree(root, new SimpleFileVisitor<>() {
                            @Override
                            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                                if (matches.size() >= MAX_GREP_RESULTS) {
                                    return FileVisitResult.TERMINATE;
                                }

                                if (!fileMatcher.matches(file)) {
                                    return FileVisitResult.CONTINUE;
                                }

                                try {
                                    if (isBinaryFile(file)) {
                                        return FileVisitResult.CONTINUE;
                                    }

                                    List<String> lines = Files.readAllLines(file, Charset.defaultCharset());
                                    for (int i = 0; i < lines.size(); i++) {
                                        if (pattern.matcher(lines.get(i)).find()) {
                                            int start = Math.max(0, i - GREP_CONTEXT_LINES);
                                            int end = Math.min(lines.size(), i + GREP_CONTEXT_LINES + 1);
                                            String context = String.join("\n", lines.subList(start, end));
                                            matches.add(new GrepMatch(
                                                    relativize(file).toString(),
                                                    i + 1,
                                                    lines.get(i),
                                                    context
                                            ));

                                            if (matches.size() >= MAX_GREP_RESULTS) {
                                                return FileVisitResult.TERMINATE;
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    // 跳过无法读取的文件
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public @NonNull FileVisitResult visitFileFailed(@NonNull Path file, @NonNull IOException exc) {
                                return FileVisitResult.CONTINUE;
                            }
                        });

                        return successResult(matches);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "搜索内容失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 获取文件元数据
     */
    public static FunctionTool getFileInfo() {
        return FunctionTool.newBuilder()
                .name("file$get_file_info")
                .description("获取文件元数据（大小、时间等）")
                .parameterType(GetFileInfoSpec.class)
                .<GetFileInfoSpec>function((caller, spec) -> {
                    try {
                        Path resolved = resolveAndValidate(spec.path());
                        if (!Files.exists(resolved)) {
                            return errorResult("PATH_NOT_FOUND", "路径不存在：" + spec.path());
                        }

                        BasicFileAttributes attrs = Files.readAttributes(resolved, BasicFileAttributes.class);
                        FileInfo info = new FileInfo(
                                Files.isDirectory(resolved) ? "directory" : "file",
                                attrs.size(),
                                attrs.lastModifiedTime().toMillis(),
                                isBinaryFile(resolved)
                        );

                        return successResult(info);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "获取文件信息失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 比较两个文件的差异
     */
    public static FunctionTool diffFiles() {
        return FunctionTool.newBuilder()
                .name("file$diff_files")
                .description("比较两个文件的差异，返回统一 diff 格式")
                .parameterType(DiffFilesSpec.class)
                .<DiffFilesSpec>function((caller, spec) -> {
                    try {
                        Path path1 = resolveAndValidate(spec.path1());
                        Path path2 = resolveAndValidate(spec.path2());

                        if (!Files.exists(path1)) {
                            return errorResult("PATH1_NOT_FOUND", "第一个路径不存在：" + spec.path1());
                        }
                        if (!Files.exists(path2)) {
                            return errorResult("PATH2_NOT_FOUND", "第二个路径不存在：" + spec.path2());
                        }

                        if (Files.isDirectory(path1) || Files.isDirectory(path2)) {
                            return errorResult("IS_DIRECTORY", "不支持比较目录");
                        }

                        if (isBinaryFile(path1) || isBinaryFile(path2)) {
                            return errorResult("BINARY_FILE", "不支持比较二进制文件");
                        }

                        // 使用 try-with-resources 确保 Stream 正确关闭
                        List<String> lines1;
                        List<String> lines2;
                        try (var stream1 = Files.lines(path1, Charset.defaultCharset());
                             var stream2 = Files.lines(path2, Charset.defaultCharset())) {
                            lines1 = stream1.limit(MAX_DIFF_LINES).toList();
                            lines2 = stream2.limit(MAX_DIFF_LINES).toList();
                        }

                        // 简单 diff 实现
                        StringBuilder diff = new StringBuilder();
                        diff.append("--- ").append(spec.path1()).append("\n");
                        diff.append("+++ ").append(spec.path2()).append("\n");

                        int i = 0, j = 0;
                        while (i < lines1.size() || j < lines2.size()) {
                            if (i < lines1.size() && j < lines2.size()) {
                                String l1 = lines1.get(i);
                                String l2 = lines2.get(j);
                                if (l1.equals(l2)) {
                                    diff.append(" ").append(l1).append("\n");
                                    i++;
                                    j++;
                                } else {
                                    diff.append("-").append(l1).append("\n");
                                    diff.append("+").append(l2).append("\n");
                                    i++;
                                    j++;
                                }
                            } else if (i < lines1.size()) {
                                diff.append("-").append(lines1.get(i)).append("\n");
                                i++;
                            } else {
                                diff.append("+").append(lines2.get(j)).append("\n");
                                j++;
                            }
                        }

                        return successResult(diff.toString());

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "比较文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析并验证路径，确保不超出工作目录
     * 关键安全修复：防止路径穿越攻击
     */
    private static Path resolveAndValidate(String userPath) throws IOException {
        // 拒绝绝对路径，防止路径穿越
        if (Paths.get(userPath).isAbsolute()) {
            throw new SecurityException("拒绝访问：不支持绝对路径：" + userPath);
        }

        Path resolved = WORKSPACE_ROOT.resolve(userPath).normalize();

        // 双重验证：确保解析后的路径仍在工作目录内
        if (!resolved.startsWith(WORKSPACE_ROOT)) {
            throw new SecurityException("拒绝访问：路径超出工作目录范围：" + userPath);
        }

        // 检查是否包含 .. 试图穿越（虽然 normalize 已经处理，但显式检查更安全）
        if (userPath.contains("..") && !resolved.startsWith(WORKSPACE_ROOT)) {
            throw new SecurityException("拒绝访问：非法路径遍历：" + userPath);
        }

        return resolved;
    }

    /**
     * 将路径转换为相对于工作目录的路径
     */
    private static Path relativize(Path path) {
        return WORKSPACE_ROOT.relativize(path);
    }

    /**
     * 检测是否为二进制文件
     * 通过魔数和控制字符比例综合判断
     */
    private static boolean isBinaryFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] header = new byte[80];
            int bytesRead = is.read(header);
            if (bytesRead == -1) {
                return false; // 空文件视为文本文件
            }

            // 检查常见的二进制文件魔数（扩展支持更多格式）
            if (bytesRead >= 4) {
                int magic = ((header[0] & 0xFF) << 24) |
                        ((header[1] & 0xFF) << 16) |
                        ((header[2] & 0xFF) << 8) |
                        (header[3] & 0xFF);

                // ZIP/JAR/PNG/GIF/PDF/JPEG/Class 等格式
                switch (magic) {
                    case 0x504B0304: // ZIP/JAR
                    case 0x89504E47: // PNG
                    case 0x47494638: // GIF
                    case 0x25504446: // PDF (%PDF)
                    case 0xCAFEBABE: // Java Class
                        return true;
                }

                // 检查 JPEG (FFD8FF)
                if (bytesRead >= 3 &&
                        (header[0] & 0xFF) == 0xFF &&
                        (header[1] & 0xFF) == 0xD8 &&
                        (header[2] & 0xFF) == 0xFF) {
                    return true;
                }

                // 检查 MP3 (ID3)
                if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                    return true;
                }
            }

            // 尝试将字节解码为 UTF-8 字符串，如果能成功解码则为文本文件
            try {
                String content = new String(header, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);

                // 统计控制字符比例（排除常见的空白字符）
                int controlChars = 0;
                int nonWhitespaceChars = 0;
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (!Character.isWhitespace(c)) {
                        nonWhitespaceChars++;
                        // 控制字符且不是制表符、换行、回车
                        if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                            controlChars++;
                        }
                    }
                }

                // 如果没有非空白字符，视为文本文件
                if (nonWhitespaceChars == 0) {
                    return false;
                }

                // 如果控制字符超过非空白字符的 10%，则可能是二进制文件
                // （提高阈值到 10% 以减少误判）
                return (double) controlChars / nonWhitespaceChars > 0.1;

            } catch (Exception e) {
                // 如果 UTF-8 解码失败，可能是二进制文件
                return true;
            }
        }
    }

    // ==================== Spec 数据结构 ====================

    record ListDirectorySpec(
            @JsonPropertyDescription("目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("是否递归列出子目录（默认 false，最大深度 3 层）")
            @JsonProperty("recursive")
            boolean recursive
    ) {
    }

    record ReadFileSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("每次读取的最大行数（默认 200）")
            @JsonProperty("limit_lines")
            int limitLines,

            @JsonPropertyDescription("跳过的行数（默认 0）")
            @JsonProperty("offset_lines")
            int offsetLines
    ) {
    }

    record WriteFileSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要写入的文件内容")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("是否覆盖现有文件（默认 false）")
            @JsonProperty("overwrite")
            boolean overwrite
    ) {
    }

    record AppendFileSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要追加的内容")
            @JsonProperty(value = "content", required = true)
            String content
    ) {
    }

    record DeleteFileSpec(
            @JsonPropertyDescription("文件或目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path
    ) {
    }

    record MoveFileSpec(
            @JsonPropertyDescription("源文件的相对路径")
            @JsonProperty(value = "source", required = true)
            String source,

            @JsonPropertyDescription("目标文件的相对路径")
            @JsonProperty(value = "destination", required = true)
            String destination
    ) {
    }

    record CreateDirectorySpec(
            @JsonPropertyDescription("目录的相对路径")
            @JsonProperty(value = "path", required = true)
            String path
    ) {
    }

    record SearchFilesSpec(
            @JsonPropertyDescription("Glob 模式（如 **/*.java）")
            @JsonProperty(value = "pattern", required = true)
            String pattern,

            @JsonPropertyDescription("搜索根目录（可选，默认为当前目录）")
            @JsonProperty("root")
            String root
    ) {
    }

    record GrepContentSpec(
            @JsonPropertyDescription("搜索词或正则表达式")
            @JsonProperty(value = "pattern", required = true)
            String pattern,

            @JsonPropertyDescription("文件 Glob 模式（可选，如 *.py）")
            @JsonProperty("file_pattern")
            String filePattern,

            @JsonPropertyDescription("是否区分大小写（默认 false）")
            @JsonProperty("case_sensitive")
            boolean caseSensitive,

            @JsonPropertyDescription("搜索根目录（可选，默认为当前目录）")
            @JsonProperty("root")
            String root
    ) {
    }

    record GetFileInfoSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path
    ) {
    }

    record DiffFilesSpec(
            @JsonPropertyDescription("第一个文件的相对路径")
            @JsonProperty(value = "path1", required = true)
            String path1,

            @JsonPropertyDescription("第二个文件的相对路径")
            @JsonProperty(value = "path2", required = true)
            String path2
    ) {
    }

    // ==================== 结果数据结构 ====================

    private static CompletableFuture<Result> successResult(Object data) {
        return CompletableFuture.completedStage(new Result(null, null, data)).toCompletableFuture();
    }

    private static CompletableFuture<Result> errorResult(String error, String message) {
        return CompletableFuture.completedStage(new Result(error, message, null)).toCompletableFuture();
    }

    private static CompletableFuture<Result> warningResult(String warning, Object data) {
        return CompletableFuture.completedStage(new Result(null, warning, data)).toCompletableFuture();
    }

    record Result(
            @JsonProperty("error")
            String error,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Object data
    ) {
    }

    record FileEntry(
            @JsonProperty("name")
            String name,

            @JsonProperty("type")
            String type,

            @JsonProperty("size")
            long size
    ) {
    }

    record FileInfo(
            @JsonProperty("type")
            String type,

            @JsonProperty("size_bytes")
            long sizeBytes,

            @JsonProperty("last_modified")
            long lastModified,

            @JsonProperty("is_binary")
            boolean isBinary
    ) {
    }

    record GrepMatch(
            @JsonProperty("file")
            String file,

            @JsonProperty("line_num")
            int lineNum,

            @JsonProperty("content")
            String content,

            @JsonProperty("context")
            String context
    ) {
    }

    record ReadResult(
            @JsonProperty("text")
            String text,

            @JsonProperty("lines_returned")
            int linesReturned,

            @JsonProperty("total_lines")
            int totalLines,

            @JsonProperty("has_more")
            boolean hasMore
    ) {
    }
}
