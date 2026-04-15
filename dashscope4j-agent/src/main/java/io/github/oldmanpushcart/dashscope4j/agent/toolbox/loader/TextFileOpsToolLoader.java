package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 文本文件操作工具加载器
 * <p>
 * 提供文本文件的精细化编辑能力：
 * - read: 读取文本文件（支持分页、编码检测）
 * - write: 覆盖写入文本文件（防误覆盖、并发控制）
 * - append: 末尾追加文本内容
 * - replace_lines: 替换指定行范围
 * - insert_lines: 在指定位置插入行
 * </p>
 */
public class TextFileOpsToolLoader implements ToolLoader {

    // ==================== 常量定义 ====================

    /**
     * 默认单次最大读取行数
     */
    private static final int DEFAULT_MAX_READ_LINES = 5000;

    /**
     * 默认读取行数
     */
    private static final int DEFAULT_READ_LINES = 200;

    /**
     * 默认最大文件大小（10MB）
     */
    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    // ==================== 实例字段 ====================

    /**
     * 工作区根路径
     */
    private final Path workspace;

    /**
     * 单次最大读取行数
     */
    private final int maxLines;

    /**
     * 最大文件大小（字节）
     */
    private final long maxFileSize;

    /**
     * 默认字符编码
     */
    private final Charset charset;

    private TextFileOpsToolLoader(Builder builder) {
        this.workspace = builder.workspace;
        this.maxLines = builder.maxLines;
        this.maxFileSize = builder.maxFileSize;
        this.charset = builder.charset;
    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {
        List<FunctionTool> tools = List.of(
                read(),
                write(),
                append(),
                replaceLines(),
                insertLines()
        );

        final var stages = tools.stream()
                .map(tool -> toolbox.register(tool.meta().name(), tool))
                .toList();
        return CompletableFutureUtils.allOf(stages);
    }

    @Override
    public void close() {
        // 无资源需要关闭
    }

    // ==================== 工具方法 ====================

    /**
     * 读取文本文件
     */
    public FunctionTool read() {
        return FunctionTool.newBuilder()
                .name("text_file$read")
                .description("""
                        读取文本文件的内容，支持分页读取和编码自动检测。
                        
                        【使用场景】
                        - 查看源代码文件内容
                        - 读取配置文件
                        - 检查日志文件
                        - 阅读文本文档
                        
                        【返回结果】
                        - content: 实际读取的文本内容
                        - lines_returned: 本次返回的行数
                        - total_lines: 文件总行数
                        - has_more: 是否还有更多内容
                        - encoding: 检测到的文件编码
                        - last_modified: 最后修改时间戳（毫秒），用于并发控制
                        
                        【注意事项】
                        - 仅支持文本文件，二进制文件会被拒绝
                        - 大文件建议分批读取（使用 offset_lines 和 limit_lines）
                        - last_modified 字段在写操作时必须传入，确保并发安全
                        """)
                .parameterType(ReadSpec.class)
                .<ReadSpec>function((caller, spec) -> {
                    try {
                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return errorResult("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        if (Files.isDirectory(resolved)) {
                            return errorResult("IS_DIRECTORY", "路径是目录而非文件：" + spec.path());
                        }

                        // 检查文件大小
                        final long fileSize = Files.size(resolved);
                        if (fileSize > maxFileSize) {
                            return errorResult("SIZE_EXCEEDED",
                                    String.format("文件大小 %.2f MB 超过限制 %.2f MB",
                                            fileSize / 1024.0 / 1024.0,
                                            maxFileSize / 1024.0 / 1024.0));
                        }

                        // 检测是否为二进制文件
                        if (FileUtils.isBinaryFile(resolved)) {
                            return errorResult("BINARY_FILE", "无法读取二进制文件：" + spec.path());
                        }

                        // 检测编码（如果 Spec 未指定）
                        final String encoding = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? spec.encoding()
                                : FileUtils.detectEncoding(resolved);

                        final int offset = Math.max(0, spec.offsetLines());
                        final int limit = spec.limitLines() > 0 ? Math.min(spec.limitLines(), maxLines) : DEFAULT_READ_LINES;

                        // 读取文件内容并统计总行数
                        final List<String> content = new ArrayList<>();
                        int totalLines = 0;

                        try (BufferedReader reader = Files.newBufferedReader(resolved, Charset.forName(encoding))) {
                            // 跳过 offset 行
                            if (offset > 0) {
                                final long skipped = reader.skip(offset);
                            }

                            // 读取 limit 行
                            String line;
                            int count = 0;
                            while (count < limit && (line = reader.readLine()) != null) {
                                content.add(line);
                                count++;
                            }

                            // 计算总行数
                            if (count == limit) {
                                // 可能还有更多行，继续统计
                                while (reader.readLine() != null) {
                                    totalLines++;
                                }
                                totalLines += offset + count;
                            } else {
                                totalLines = offset + count;
                            }
                        }

                        final boolean hasMore = totalLines > offset + content.size();
                        final String text = String.join("\n", content);
                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        final Map<String, Object> data = new HashMap<>();
                        data.put("content", text);
                        data.put("lines_returned", content.size());
                        data.put("total_lines", totalLines);
                        data.put("has_more", hasMore);
                        data.put("encoding", encoding);
                        data.put("last_modified", lastModified);
                        return successResult(data);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "读取文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 覆盖写入文本文件
     */
    public FunctionTool write() {
        return FunctionTool.newBuilder()
                .name("text_file$write")
                .description("""
                        创建新文件或覆盖现有文件（需显式声明覆盖）。
                        
                        【使用场景】
                        - 创建新的源代码文件
                        - 生成配置文件
                        - 完全重写文件内容
                        
                        【返回结果】
                        - operation: 操作类型（created 或 overwritten）
                        - bytes_written: 写入的字节数
                        - lines_count: 写入的行数
                        
                        【注意事项】
                        - 默认禁止覆盖现有文件，防止误操作
                        - last_modified 用于并发控制，必须与文件当前时间戳一致
                        - 新文件创建时 last_modified 传 0
                        - 如果父目录不存在会自动创建
                        """)
                .parameterType(WriteSpec.class)
                .<WriteSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "文件内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        // 并发控制校验
                        if (Files.exists(resolved)) {
                            FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                            if (!spec.overwrite()) {
                                return errorResult("FILE_EXISTS",
                                        "文件已存在，设置 overwrite=true 以覆盖：" + spec.path());
                            }
                        } else {
                            // 新文件，last_modified 应为 0
                            if (spec.lastModified() != 0) {
                                return errorResult("INVALID_TIMESTAMP",
                                        "新文件的 last_modified 应为 0，当前值：" + spec.lastModified());
                            }
                        }

                        // 确保父目录存在
                        final Path parent = resolved.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        Files.writeString(resolved, spec.content(), fileCharset);

                        final long bytesWritten = Files.size(resolved);
                        final long linesCount = spec.content().split("\\n", -1).length;
                        final String operation = Files.exists(resolved) ? "overwritten" : "created";

                        final Map<String, Object> data = new HashMap<>();
                        data.put("operation", operation);
                        data.put("bytes_written", bytesWritten);
                        data.put("lines_count", linesCount);
                        return successResult(data);

                    } catch (SecurityException e) {
                        return errorResult("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "写入文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 末尾追加文本内容
     */
    public FunctionTool append() {
        return FunctionTool.newBuilder()
                .name("text_file$append")
                .description("""
                        向文件末尾追加内容（适合日志记录、累积数据等场景）。
                        
                        【使用场景】
                        - 向日志文件追加新的日志条目
                        - 在数据文件末尾添加新记录
                        - 累积输出结果
                        
                        【返回结果】
                        - bytes_appended: 追加的字节数
                        
                        【注意事项】
                        - 内容总是添加到文件末尾
                        - 如果文件不存在会自动创建
                        - 不需要 last_modified 参数（追加操作天然安全）
                        - 如果父目录不存在会自动创建
                        """)
                .parameterType(AppendSpec.class)
                .<AppendSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "追加内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        // 确保父目录存在
                        final Path parent = resolved.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 处理换行符
                        String contentToAppend = spec.content();
                        if (spec.addNewline() && Files.exists(resolved) && Files.size(resolved) > 0) {
                            // 检查文件末尾是否已有换行符
                            final byte[] lastBytes = Files.readAllBytes(resolved);
                            if (lastBytes.length > 0 && lastBytes[lastBytes.length - 1] != '\n') {
                                contentToAppend = "\n" + contentToAppend;
                            }
                        }

                        Files.writeString(resolved, contentToAppend, fileCharset,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        long bytesAppended = contentToAppend.getBytes(StandardCharsets.UTF_8).length;

                        final Map<String, Object> data = new HashMap<>();
                        data.put("bytes_appended", bytesAppended);
                        return successResult(data);

                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "追加内容失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 替换指定行范围
     */
    public FunctionTool replaceLines() {
        return FunctionTool.newBuilder()
                .name("text_file$replace_lines")
                .description("""
                        替换文件中指定行范围的内容。
                        
                        【使用场景】
                        - 修改函数实现
                        - 更新配置项
                        - 修正错误代码
                        
                        【返回结果】
                        - operation: 操作类型（replace_lines）
                        - lines_affected: 受影响的行数
                        - lines_added: 新增的行数
                        - lines_removed: 删除的行数
                        - new_total_lines: 修改后的总行数
                        
                        【注意事项】
                        - 必须先调用 read 获取 last_modified
                        - 新内容的行数可以与原范围不同
                        """)
                .parameterType(ReplaceLinesSpec.class)
                .<ReplaceLinesSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "替换内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return errorResult("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        // 并发控制校验
                        FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                        // 读取所有行
                        final List<String> allLines = Files.readAllLines(resolved, StandardCharsets.UTF_8);
                        final int totalLines = allLines.size();

                        // 验证行号范围
                        final int startLine = spec.startLine();
                        final int endLine = spec.endLine() != null ? spec.endLine() : totalLines;

                        if (startLine < 1 || startLine > totalLines) {
                            return errorResult("INVALID_LINE_NUMBER",
                                    String.format("起始行号超出范围：1-%d", totalLines));
                        }

                        if (endLine < startLine || endLine > totalLines) {
                            return errorResult("INVALID_LINE_NUMBER",
                                    String.format("结束行号超出范围：%d-%d", startLine, totalLines));
                        }

                        final int range = endLine - startLine + 1;
                        if (range > maxLines) {
                            return errorResult("RANGE_TOO_LARGE",
                                    String.format("单次最多操作 %d 行，当前请求：%d", maxLines, range));
                        }

                        // 执行替换
                        final List<String> newLines = new ArrayList<>();
                        // 添加前面的行
                        for (int i = 0; i < startLine - 1; i++) {
                            newLines.add(allLines.get(i));
                        }
                        // 添加新内容
                        final String[] newContentLines = spec.content().split("\\n", -1);
                        newLines.addAll(Arrays.asList(newContentLines));
                        // 添加后面的行
                        for (int i = endLine; i < totalLines; i++) {
                            newLines.add(allLines.get(i));
                        }

                        // 写回文件（保持原编码）
                        Files.write(resolved, newLines, StandardCharsets.UTF_8);

                        final int linesAdded = newContentLines.length;
                        final int newTotalLines = newLines.size();

                        final Map<String, Object> data = new HashMap<>();
                        data.put("operation", "replace_lines");
                        data.put("lines_affected", range);
                        data.put("lines_added", linesAdded);
                        data.put("lines_removed", range);
                        data.put("new_total_lines", newTotalLines);
                        return successResult(data);

                    } catch (SecurityException e) {
                        return errorResult("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "替换行失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 在指定位置插入行
     */
    public FunctionTool insertLines() {
        return FunctionTool.newBuilder()
                .name("text_file$insert_lines")
                .description("""
                        在文件的指定位置插入新行。
                        
                        【使用场景】
                        - 在函数前添加注释
                        - 在 import 区域添加新导入
                        - 在配置文件添加新配置项
                        
                        【返回结果】
                        - operation: 操作类型（insert_lines）
                        - lines_inserted: 插入的行数
                        - new_total_lines: 插入后的总行数
                        - inserted_at_line: 实际插入的行号
                        
                        【注意事项】
                        - 必须先调用 read 获取 last_modified
                        - line_number=0 表示在文件开头插入
                        - line_number > 总行数 表示在文件末尾追加
                        """)
                .parameterType(InsertLinesSpec.class)
                .<InsertLinesSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return errorResult("INVALID_CONTENT", "插入内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return errorResult("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        // 并发控制校验
                        FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                        // 读取所有行
                        final List<String> allLines = Files.readAllLines(resolved, StandardCharsets.UTF_8);
                        final int totalLines = allLines.size();

                        // 验证行号
                        final int lineNumber = spec.lineNumber();
                        if (lineNumber < 0 || lineNumber > totalLines) {
                            return errorResult("INVALID_LINE_NUMBER",
                                    String.format("行号超出范围：0-%d", totalLines));
                        }

                        // 分割新内容
                        final String[] newContentLines = spec.content().split("\\n", -1);
                        if (newContentLines.length > maxLines) {
                            return errorResult("RANGE_TOO_LARGE",
                                    String.format("单次最多插入 %d 行，当前请求：%d", maxLines, newContentLines.length));
                        }

                        // 执行插入
                        final List<String> newLines = new ArrayList<>();
                        // 添加前面的行
                        for (int i = 0; i < lineNumber; i++) {
                            newLines.add(allLines.get(i));
                        }
                        // 添加新内容
                        newLines.addAll(Arrays.asList(newContentLines));
                        // 添加后面的行
                        for (int i = lineNumber; i < totalLines; i++) {
                            newLines.add(allLines.get(i));
                        }

                        // 写回文件（保持原编码）
                        Files.write(resolved, newLines, StandardCharsets.UTF_8);

                        final int insertedAtLine = lineNumber == 0 ? 1 : lineNumber;

                        final Map<String, Object> data = new HashMap<>();
                        data.put("operation", "insert_lines");
                        data.put("lines_inserted", newContentLines.length);
                        data.put("new_total_lines", newLines.size());
                        data.put("inserted_at_line", insertedAtLine);
                        return successResult(data);

                    } catch (SecurityException e) {
                        return errorResult("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return errorResult("IO_ERROR", "插入行失败：" + e.getMessage());
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    private Result successResult(Object data) {
        return new Result(null, null, data);
    }

    private Result errorResult(String error, String message) {
        return new Result(error, message, null);
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<TextFileOpsToolLoader, Builder> {

        private Path workspace;
        private int maxLines = DEFAULT_MAX_READ_LINES;
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE_BYTES;
        private Charset charset = StandardCharsets.UTF_8;

        /**
         * 设置工作区根路径
         *
         * @param workspace 工作区根路径
         * @return 当前构建器
         */
        public Builder workspace(Path workspace) {
            this.workspace = workspace.toAbsolutePath().normalize();
            return this;
        }

        /**
         * 设置单次最大读取行数
         *
         * @param maxLines 最大行数
         * @return 当前构建器
         */
        public Builder maxLines(int maxLines) {
            this.maxLines = maxLines;
            return this;
        }

        /**
         * 设置最大文件大小（字节）
         *
         * @param maxFileSize 最大文件大小
         * @return 当前构建器
         */
        public Builder maxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * 设置默认字符编码
         *
         * @param charset 默认编码
         * @return 当前构建器
         */
        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        @Override
        public TextFileOpsToolLoader build() {
            // 如果未设置工作区，使用当前目录
            if (workspace == null) {
                workspace = Paths.get("").toAbsolutePath().normalize();
            }
            return new TextFileOpsToolLoader(this);
        }
    }

    // ==================== Spec 数据结构 ====================

    record ReadSpec(
            @JsonPropertyDescription("文件的相对路径，例如：src/main.java")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("起始行号，从0开始（可选，默认0）")
            @JsonProperty("offset_lines")
            int offsetLines,

            @JsonPropertyDescription("最大读取行数（可选，默认200，最大5000）")
            @JsonProperty("limit_lines")
            int limitLines,

            @JsonPropertyDescription("文件编码（可选，不传则自动检测）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record WriteSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要写入的文件内容（不能为空）")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("是否覆盖现有文件（可选，默认false）")
            @JsonProperty("overwrite")
            boolean overwrite,

            @JsonPropertyDescription("最后修改时间戳（从read获取，新文件传0）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record AppendSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要追加的内容（不能为空）")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("是否在开头添加换行符（可选，默认true）")
            @JsonProperty("add_newline")
            boolean addNewline,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record ReplaceLinesSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("起始行号，从1开始计数")
            @JsonProperty(value = "start_line", required = true)
            int startLine,

            @JsonPropertyDescription("结束行号（包含），不传则替换到文件末尾")
            @JsonProperty("end_line")
            Integer endLine,

            @JsonPropertyDescription("新的内容（每行用\\n分隔）")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("最后修改时间戳（从read获取）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified
    ) {
    }

    record InsertLinesSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("插入位置行号，从1开始（0表示文件开头）")
            @JsonProperty(value = "line_number", required = true)
            int lineNumber,

            @JsonPropertyDescription("要插入的内容（每行用\\n分隔）")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("最后修改时间戳（从read获取）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified
    ) {
    }

    // ==================== 结果数据结构 ====================

    record Result(
            @JsonProperty("error")
            String error,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Object data
    ) {
    }

}
