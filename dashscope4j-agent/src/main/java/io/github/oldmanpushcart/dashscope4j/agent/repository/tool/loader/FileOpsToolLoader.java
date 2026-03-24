package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$list_directory")
                        .description("列出指定目录下的文件和子目录（类似 ls）")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

                                Files.walkFileTree(resolved, new FileVisitor<Path>() {
                                    private int currentDepth = 0;

                                    @Override
                                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                        if (currentDepth >= maxDepth) {
                                            return FileVisitResult.SKIP_SUBTREE;
                                        }
                                        currentDepth++;
                                        if (dir.equals(resolved)) {
                                            return FileVisitResult.CONTINUE;
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                        try {
                                            entries.add(new FileEntry(
                                                    relativize(file).toString(),
                                                    Files.isDirectory(file) ? "directory" : "file",
                                                    Files.size(file)
                                            ));
                                        } catch (IOException e) {
                                            // 忽略无法访问的文件
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                        currentDepth--;
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

            record Spec(
                    @JsonPropertyDescription("目录的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path,

                    @JsonPropertyDescription("是否递归列出子目录（默认 false，最大深度 3 层）")
                    @JsonProperty("recursive")
                    boolean recursive
            ) {}
        }.get();
    }

    /**
     * 读取文本文件内容
     */
    public static FunctionTool readFile() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$read_file")
                        .description("读取文本文件内容，支持分页读取")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

                                List<String> allLines = Files.readAllLines(resolved, Charset.defaultCharset());
                                if (offset >= allLines.size()) {
                                    return successResult(new ReadResult("", 0, allLines.size(), true));
                                }

                                int endIdx = Math.min(offset + limit, allLines.size());
                                List<String> content = allLines.subList(offset, endIdx);
                                String text = String.join("\n", content);

                                boolean hasMore = endIdx < allLines.size();
                                return successResult(new ReadResult(text, content.size(), allLines.size(), hasMore));

                            } catch (IOException e) {
                                return errorResult("IO_ERROR", "读取文件失败：" + e.getMessage());
                            }
                        })
                        .build();
            }

            record Spec(
                    @JsonPropertyDescription("文件的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path,

                    @JsonPropertyDescription("每次读取的最大行数（默认 200）")
                    @JsonProperty("limit_lines")
                    int limitLines,

                    @JsonPropertyDescription("跳过的行数（默认 0）")
                    @JsonProperty("offset_lines")
                    int offsetLines
            ) {}
        }.get();
    }

    /**
     * 创建新文件或覆盖现有文件
     */
    public static FunctionTool writeFile() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$write_file")
                        .description("创建新文件或覆盖现有文件。默认禁止覆盖，除非显式声明 overwrite=true")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            try {
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

            record Spec(
                    @JsonPropertyDescription("文件的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path,

                    @JsonPropertyDescription("要写入的文件内容")
                    @JsonProperty(value = "content", required = true)
                    String content,

                    @JsonPropertyDescription("是否覆盖现有文件（默认 false）")
                    @JsonProperty("overwrite")
                    boolean overwrite
            ) {}
        }.get();
    }

    /**
     * 向文件末尾追加内容
     */
    public static FunctionTool appendFile() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$append_file")
                        .description("向文件末尾追加内容，若文件不存在则自动创建")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            try {
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

            record Spec(
                    @JsonPropertyDescription("文件的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path,

                    @JsonPropertyDescription("要追加的内容")
                    @JsonProperty(value = "content", required = true)
                    String content
            ) {}
        }.get();
    }

    /**
     * 删除指定文件或空目录
     */
    public static FunctionTool deleteFile() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$delete_file")
                        .description("删除指定文件或空目录。严禁递归删除非空目录")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

            record Spec(
                    @JsonPropertyDescription("文件或目录的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path
            ) {}
        }.get();
    }

    /**
     * 重命名或移动文件
     */
    public static FunctionTool moveFile() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$move_file")
                        .description("重命名文件或移动文件位置。若目标已存在则报错")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

                                Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
                                return successResult("文件移动成功");

                            } catch (IOException e) {
                                return errorResult("IO_ERROR", "移动文件失败：" + e.getMessage());
                            }
                        })
                        .build();
            }

            record Spec(
                    @JsonPropertyDescription("源文件的相对路径")
                    @JsonProperty(value = "source", required = true)
                    String source,

                    @JsonPropertyDescription("目标文件的相对路径")
                    @JsonProperty(value = "destination", required = true)
                    String destination
            ) {}
        }.get();
    }

    /**
     * 创建目录
     */
    public static FunctionTool createDirectory() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$create_directory")
                        .description("创建新目录（含父目录），类似 mkdir -p")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

            record Spec(
                    @JsonPropertyDescription("目录的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path
            ) {}
        }.get();
    }

    /**
     * 基于文件名/Glob 模式查找文件
     */
    public static FunctionTool searchFiles() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$search_files")
                        .description("基于文件名/Glob 模式查找文件")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            try {
                                Path root = spec.root() != null ? resolveAndValidate(spec.root()) : Paths.get("");
                                if (!Files.isDirectory(root)) {
                                    return errorResult("INVALID_ROOT", "根路径不是目录：" + spec.root());
                                }

                                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + spec.pattern());
                                List<String> results = new ArrayList<>();

                                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                        if (results.size() >= MAX_SEARCH_RESULTS) {
                                            return FileVisitResult.TERMINATE;
                                        }
                                        if (matcher.matches(file.getFileName())) {
                                            results.add(relativize(file).toString());
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
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

            record Spec(
                    @JsonPropertyDescription("Glob 模式（如 **/*.java）")
                    @JsonProperty(value = "pattern", required = true)
                    String pattern,

                    @JsonPropertyDescription("搜索根目录（可选，默认为当前目录）")
                    @JsonProperty("root")
                    String root
            ) {}
        }.get();
    }

    /**
     * 在文件内容中搜索关键词或正则
     */
    public static FunctionTool grepContent() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$grep_content")
                        .description("在文件内容中搜索关键词或正则表达式")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            try {
                                String patternStr = spec.caseSensitive() ? spec.pattern() : "(?i)" + spec.pattern();
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);

                                Path root = spec.root() != null ? resolveAndValidate(spec.root()) : Paths.get("");
                                if (!Files.isDirectory(root)) {
                                    return errorResult("INVALID_ROOT", "根路径不是目录：" + spec.root());
                                }

                                PathMatcher fileMatcher = spec.filePattern() != null
                                        ? FileSystems.getDefault().getPathMatcher("glob:" + spec.filePattern())
                                        : path -> true;

                                List<GrepMatch> matches = new ArrayList<>();

                                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
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
                                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
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

            record Spec(
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
            ) {}
        }.get();
    }

    /**
     * 获取文件元数据
     */
    public static FunctionTool getFileInfo() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$get_file_info")
                        .description("获取文件元数据（大小、时间等）")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

            record Spec(
                    @JsonPropertyDescription("文件的相对路径")
                    @JsonProperty(value = "path", required = true)
                    String path
            ) {}
        }.get();
    }

    /**
     * 比较两个文件的差异
     */
    public static FunctionTool diffFiles() {
        return new Supplier<FunctionTool>() {
            @Override
            public FunctionTool get() {
                return FunctionTool.newBuilder()
                        .name("file$diff_files")
                        .description("比较两个文件的差异，返回统一 diff 格式")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

                                List<String> lines1 = Files.readAllLines(path1, Charset.defaultCharset());
                                List<String> lines2 = Files.readAllLines(path2, Charset.defaultCharset());

                                // 简单 diff 实现
                                StringBuilder diff = new StringBuilder();
                                diff.append("--- ").append(spec.path1()).append("\n");
                                diff.append("+++ ").append(spec.path2()).append("\n");

                                int maxSize = Math.max(lines1.size(), lines2.size());
                                if (maxSize > 1000) {
                                    return errorResult("FILE_TOO_LARGE", "文件过大，仅比较前 1000 行");
                                }

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

            record Spec(
                    @JsonPropertyDescription("第一个文件的相对路径")
                    @JsonProperty(value = "path1", required = true)
                    String path1,

                    @JsonPropertyDescription("第二个文件的相对路径")
                    @JsonProperty(value = "path2", required = true)
                    String path2
            ) {}
        }.get();
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析并验证路径，确保不超出工作目录
     */
    private static Path resolveAndValidate(String userPath) throws IOException {
        Path workspaceRoot = Paths.get("").toAbsolutePath().normalize();
        Path resolved = workspaceRoot.resolve(userPath).normalize();

        if (!resolved.startsWith(workspaceRoot)) {
            throw new SecurityException("拒绝访问：路径超出工作目录范围：" + userPath);
        }

        return resolved;
    }

    /**
     * 将路径转换为相对于工作目录的路径
     */
    private static Path relativize(Path path) {
        Path workspaceRoot = Paths.get("").toAbsolutePath();
        return workspaceRoot.relativize(path);
    }

    /**
     * 检测是否为二进制文件
     */
    private static boolean isBinaryFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] header = new byte[80];
            int bytesRead = is.read(header);
            if (bytesRead == -1) {
                return false; // 空文件视为文本文件
            }

            // 检查常见的二进制文件魔数
            if (bytesRead >= 4) {
                int magic = ((header[0] & 0xFF) << 24) |
                        ((header[1] & 0xFF) << 16) |
                        ((header[2] & 0xFF) << 8) |
                        (header[3] & 0xFF);
                // ZIP/JAR/PNG/GIF 等格式
                if (magic == 0x504B0304 || magic == 0x89504E47 || magic == 0x47494638) {
                    return true;
                }
            }

            // 尝试将字节解码为 UTF-8 字符串，如果能成功解码则为文本文件
            try {
                String content = new String(header, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                // 如果能正常解码，再检查是否包含大量控制字符（排除正常的空白字符）
                int controlChars = 0;
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    // 控制字符且不是常见的空白字符（制表符、换行、回车）
                    if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                        controlChars++;
                    }
                }
                // 如果控制字符超过 5%，则可能是二进制文件
                return controlChars > content.length() * 0.05;
            } catch (Exception e) {
                // 如果 UTF-8 解码失败，可能是二进制文件
                return true;
            }
        }
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
    ) {}

    record FileEntry(
            @JsonProperty("name")
            String name,

            @JsonProperty("type")
            String type,

            @JsonProperty("size")
            long size
    ) {}

    record FileInfo(
            @JsonProperty("type")
            String type,

            @JsonProperty("size_bytes")
            long sizeBytes,

            @JsonProperty("last_modified")
            long lastModified,

            @JsonProperty("is_binary")
            boolean isBinary
    ) {}

    record GrepMatch(
            @JsonProperty("file")
            String file,

            @JsonProperty("line_num")
            int lineNum,

            @JsonProperty("content")
            String content,

            @JsonProperty("context")
            String context
    ) {}

    record ReadResult(
            @JsonProperty("text")
            String text,

            @JsonProperty("lines_returned")
            int linesReturned,

            @JsonProperty("total_lines")
            int totalLines,

            @JsonProperty("has_more")
            boolean hasMore
    ) {}
}
