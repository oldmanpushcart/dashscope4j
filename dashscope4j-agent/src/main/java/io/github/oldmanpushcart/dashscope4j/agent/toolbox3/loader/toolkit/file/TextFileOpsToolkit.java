package io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader.toolkit.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文本文件操作工具包
 * <p>
 * 提供智能体友好的文本文件编辑能力：
 * - view: 查看文件内容（支持行范围）
 * - search: 关键词搜索（返回匹配行及上下文）
 * - str_replace: 字符串精确替换（要求唯一匹配）
 * - insert_line: 在指定行插入内容
 * - create: 创建新文件（可选覆盖）
 * </p>
 */
public class TextFileOpsToolkit implements Toolkit {

    // ==================== 常量定义 ====================

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
     * 最大文件大小（字节）
     */
    private final long maxFileSize;

    /**
     * 默认字符编码
     */
    private final Charset charset;

    /**
     * 是否只读模式
     */
    private final boolean readOnly;

    private TextFileOpsToolkit(Builder builder) {
        this.workspace = builder.workspace;
        this.maxFileSize = builder.maxFileSize;
        this.charset = builder.charset;
        this.readOnly = builder.readOnly;
    }

    @Override
    public List<Tool> tools() {
        if (readOnly) {
            // 只读模式：仅返回查看和搜索工具
            return List.of(view(), search());
        } else {
            // 读写模式：返回所有工具
            return List.of(view(), search(), strReplace(), insertLine(), create());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 查看文件内容
     */
    public FunctionTool view() {
        return FunctionTool.newBuilder()
                .name("file$view")
                .description("""
                        查看文件内容，支持指定行范围。
                        
                        【使用场景】
                        - 查看源代码文件内容
                        - 阅读配置文件
                        - 检查日志片段
                        
                        【返回结果】
                        - content: 带行号的文件内容（格式："1: line1\\n2: line2"）
                        - total_lines: 文件总行数
                        - lines_returned: 实际返回的行数
                        - last_modified: 最后修改时间戳（毫秒），用于后续编辑的并发控制
                        
                        【注意事项】
                        - viewRange 从1开始计数，如 [10, 20] 表示第10-20行
                        - 不指定 viewRange 时返回全部内容
                        - 超出范围的行号会自动截断
                        - 仅支持文本文件，二进制文件会被拒绝
                        """)
                .parameterType(ViewSpec.class)
                .<ViewSpec>function((caller, spec) -> {
                    try {
                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        if (Files.isDirectory(resolved)) {
                            return Result.error("IS_DIRECTORY", "路径是目录而非文件：" + spec.path());
                        }

                        // 检查文件大小
                        final long fileSize = Files.size(resolved);
                        if (fileSize > maxFileSize) {
                            return Result.error("SIZE_EXCEEDED",
                                    "文件大小 %.2f MB 超过限制 %.2f MB".formatted(
                                            fileSize / 1024.0 / 1024.0,
                                            maxFileSize / 1024.0 / 1024.0));
                        }

                        // 检测是否为二进制文件
                        if (FileUtils.isBinaryFile(resolved)) {
                            return Result.error("BINARY_FILE", "无法查看二进制文件：" + spec.path());
                        }

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 读取所有行
                        final List<String> allLines = Files.readAllLines(resolved, fileCharset);
                        final int totalLines = allLines.size();

                        // 确定查看范围
                        int startLine = 1;
                        int endLine = totalLines;

                        if (spec.viewRange() != null && spec.viewRange().length == 2) {
                            startLine = Math.max(1, spec.viewRange()[0]);
                            endLine = Math.min(totalLines, spec.viewRange()[1]);
                        }

                        // 提取指定范围的行
                        final List<String> selectedLines = allLines.subList(
                                Math.max(0, startLine - 1),
                                Math.min(totalLines, endLine)
                        );

                        // 构建带行号的内容
                        final StringBuilder contentBuilder = new StringBuilder();
                        for (int i = 0; i < selectedLines.size(); i++) {
                            if (i > 0) {
                                contentBuilder.append("\n");
                            }
                            contentBuilder.append(startLine + i).append(": ").append(selectedLines.get(i));
                        }

                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        return Result.success(Map.of(
                                "content", contentBuilder.toString(),
                                "total_lines", totalLines,
                                "lines_returned", selectedLines.size(),
                                "last_modified", lastModified
                        ));

                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "查看文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 文件内关键词搜索
     */
    public FunctionTool search() {
        return FunctionTool.newBuilder()
                .name("file$search")
                .description("""
                        在文件中搜索包含关键词的行，返回匹配项及上下文。
                        
                        【使用场景】
                        - 定位函数或方法定义
                        - 查找特定变量或常量
                        - 搜索TODO注释或错误信息
                        
                        【返回结果】
                        - count: 总匹配数
                        - results: 匹配结果列表，每项包含：
                          * line_number: 行号（从1开始）
                          * content: 匹配的完整行内容
                          * before: 前N行内容（如果 contextLines > 0）
                          * after: 后N行内容（如果 contextLines > 0）
                        
                        【典型工作流】
                        1. 定位方法：search(path="Main.java", searchTerm="public void calculate")
                        2. 确认上下文后，使用 str_replace 进行精确替换
                        
                        【注意事项】
                        - 简单文本匹配（非正则），大小写敏感
                        - 返回所有匹配项，建议配合 contextLines 使用
                        - 大文件搜索可能较慢
                        """)
                .parameterType(SearchSpec.class)
                .<SearchSpec>function((caller, spec) -> {
                    try {
                        // 验证搜索词
                        if (spec.searchTerm() == null || spec.searchTerm().isEmpty()) {
                            return Result.error("INVALID_SEARCH_TERM", "搜索关键词不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        if (Files.isDirectory(resolved)) {
                            return Result.error("IS_DIRECTORY", "路径是目录而非文件：" + spec.path());
                        }

                        // 检查文件大小
                        final long fileSize = Files.size(resolved);
                        if (fileSize > maxFileSize) {
                            return Result.error("SIZE_EXCEEDED",
                                    "文件大小 %.2f MB 超过限制 %.2f MB".formatted(
                                            fileSize / 1024.0 / 1024.0,
                                            maxFileSize / 1024.0 / 1024.0));
                        }

                        // 检测是否为二进制文件
                        if (FileUtils.isBinaryFile(resolved)) {
                            return Result.error("BINARY_FILE", "无法搜索二进制文件：" + spec.path());
                        }

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 读取所有行
                        final List<String> allLines = Files.readAllLines(resolved, fileCharset);
                        final int totalLines = allLines.size();

                        // 执行搜索
                        final List<Map<String, Object>> results = new ArrayList<>();
                        final int contextLines = Math.max(0, spec.contextLines());
                        final String searchTerm = spec.searchTerm();

                        for (int i = 0; i < totalLines; i++) {
                            final String line = allLines.get(i);

                            if (line.contains(searchTerm)) {
                                // 构建结果项
                                final Map<String, Object> resultItem = new java.util.HashMap<>();
                                resultItem.put("line_number", i + 1); // 行号从1开始
                                resultItem.put("content", line);

                                // 添加上下文
                                if (contextLines > 0) {
                                    // 前N行
                                    final int startIdx = Math.max(0, i - contextLines);
                                    final List<String> beforeLines = allLines.subList(startIdx, i);
                                    resultItem.put("before", beforeLines);

                                    // 后N行
                                    final int endIdx = Math.min(totalLines, i + contextLines + 1);
                                    final List<String> afterLines = allLines.subList(i + 1, endIdx);
                                    resultItem.put("after", afterLines);
                                }

                                results.add(resultItem);
                            }
                        }

                        return Result.success(Map.of(
                                "count", results.size(),
                                "results", results
                        ));

                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "搜索文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 字符串精确替换
     */
    public FunctionTool strReplace() {
        return FunctionTool.newBuilder()
                .name("file$str_replace")
                .description("""
                        在文件中精确替换文本内容，要求 oldStr 必须唯一匹配。
                        
                        【使用场景】
                        - 修改变量名或方法名
                        - 更新配置项的值
                        - 修复代码中的特定文本
                        
                        【工作流程】
                        1. 使用 file$search 定位目标内容
                        2. 使用 file$view 确认上下文
                        3. 调用 file$str_replace 执行替换（提供足够的上下文使 oldStr 唯一）
                        
                        【返回结果】
                        - operation: 操作类型（str_replace）
                        - replacements_count: 替换次数（成功时为1）
                        - last_modified: 修改后的时间戳
                        
                        【注意事项】
                        - oldStr 必须在文件中唯一匹配，否则报错
                        - 必须提供 lastModified（从 file$view 获取）
                        - 大小写敏感，空格和缩进必须完全匹配
                        - 如果文件已被修改（lastModified 不匹配），操作会失败
                        """)
                .parameterType(StrReplaceSpec.class)
                .<StrReplaceSpec>function((caller, spec) -> {
                    try {
                        // 验证参数
                        if (spec.oldStr() == null || spec.oldStr().isEmpty()) {
                            return Result.error("INVALID_OLD_STR", "oldStr 不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        // 并发控制校验
                        FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 读取文件内容
                        final String content = Files.readString(resolved, fileCharset);

                        // 查找匹配位置
                        final int firstIndex = content.indexOf(spec.oldStr());
                        if (firstIndex == -1) {
                            return Result.error("NOT_FOUND",
                                    "未找到匹配的文本。建议先用 file$search 定位，并提供更多上下文使 oldStr 唯一。");
                        }

                        // 检查是否有多个匹配
                        final int secondIndex = content.indexOf(spec.oldStr(), firstIndex + 1);
                        if (secondIndex != -1) {
                            return Result.error("MULTIPLE_MATCHES",
                                    "找到多个匹配项，oldStr 不够唯一。请提供更多上下文（如前后几行代码）使匹配唯一。");
                        }

                        // 执行替换
                        final String newContent = content.substring(0, firstIndex) +
                                spec.newStr() +
                                content.substring(firstIndex + spec.oldStr().length());

                        // 写回文件
                        Files.writeString(resolved, newContent, fileCharset);
                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        return Result.success(Map.of(
                                "operation", "str_replace",
                                "replacements_count", 1,
                                "last_modified", lastModified
                        ));

                    } catch (SecurityException e) {
                        return Result.error("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "替换文本失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 在指定行插入内容
     */
    public FunctionTool insertLine() {
        return FunctionTool.newBuilder()
                .name("file$insert_line")
                .description("""
                        在文件的指定行位置插入新内容。
                        
                        【使用场景】
                        - 在函数中添加新语句
                        - 在类中添加新方法或字段
                        - 在文件中插入新的配置项
                        
                        【行号语义】
                        - lineNumber=1：在第1行之前插入（成为新的第1行）
                        - lineNumber=50：在第50行之前插入（原第50行变为51行）
                        - lineNumber > 总行数：追加到文件末尾
                        - lineNumber <= 0：视为在第1行之前插入
                        
                        【返回结果】
                        - operation: 操作类型（insert_line）
                        - inserted_at_line: 实际插入的行号
                        - lines_inserted: 插入的行数
                        - last_modified: 修改后的时间戳
                        
                        【注意事项】
                        - 必须提供 lastModified（从 file$view 获取）
                        - content 可以包含多行（用 \\n 分隔）
                        - 如果文件已被修改（lastModified 不匹配），操作会失败
                        """)
                .parameterType(InsertLineSpec.class)
                .<InsertLineSpec>function((caller, spec) -> {
                    try {
                        // 验证参数
                        if (spec.content() == null || spec.content().isEmpty()) {
                            return Result.error("INVALID_CONTENT", "插入内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            return Result.error("FILE_NOT_FOUND", "文件不存在：" + spec.path());
                        }

                        // 并发控制校验
                        FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 读取所有行
                        final List<String> allLines = Files.readAllLines(resolved, fileCharset);
                        final int totalLines = allLines.size();

                        // 计算插入位置（转换为索引）
                        int insertIndex;
                        if (spec.lineNumber() <= 0) {
                            insertIndex = 0; // 在文件开头插入
                        } else if (spec.lineNumber() > totalLines) {
                            insertIndex = totalLines; // 在文件末尾插入
                        } else {
                            insertIndex = spec.lineNumber() - 1; // 转换为0-based索引
                        }

                        // 分割插入内容为多行
                        final String[] linesToInsert = spec.content().split("\n", -1);

                        // 构建新内容
                        final List<String> newLines = new ArrayList<>();
                        newLines.addAll(allLines.subList(0, insertIndex));
                        newLines.addAll(List.of(linesToInsert));
                        newLines.addAll(allLines.subList(insertIndex, totalLines));

                        // 写回文件
                        final String newContent = String.join("\n", newLines);
                        Files.writeString(resolved, newContent, fileCharset);
                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        return Result.success(Map.of(
                                "operation", "insert_line",
                                "inserted_at_line", insertIndex + 1, // 返回1-based行号
                                "lines_inserted", linesToInsert.length,
                                "last_modified", lastModified
                        ));

                    } catch (SecurityException e) {
                        return Result.error("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "插入内容失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 创建新文件
     */
    public FunctionTool create() {
        return FunctionTool.newBuilder()
                .name("file$create")
                .description("""
                        创建新文件并写入内容。
                        
                        【使用场景】
                        - 创建新的源代码文件
                        - 生成配置文件
                        - 创建文档或脚本
                        
                        【返回结果】
                        - operation: 操作类型（created 或 overwritten）
                        - bytes_written: 写入的字节数
                        - lines_count: 写入的行数
                        
                        【注意事项】
                        - 默认不允许覆盖已存在的文件
                        - 设置 overwrite=true 可以强制覆盖
                        - 如果父目录不存在会自动创建
                        - 新文件不需要 lastModified
                        """)
                .parameterType(CreateFileSpec.class)
                .<CreateFileSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.fileText() == null || spec.fileText().isEmpty()) {
                            return Result.error("INVALID_CONTENT", "文件内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        // 检查文件是否存在
                        if (Files.exists(resolved)) {
                            if (!spec.overwrite()) {
                                return Result.error("FILE_EXISTS",
                                        "文件已存在，设置 overwrite=true 以覆盖：" + spec.path());
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

                        // 写入文件
                        Files.writeString(resolved, spec.fileText(), fileCharset);

                        final long bytesWritten = Files.size(resolved);
                        final long linesCount = spec.fileText().split("\n", -1).length;
                        final String operation = Files.exists(resolved) ? "overwritten" : "created";

                        return Result.success(Map.of(
                                "operation", operation,
                                "bytes_written", bytesWritten,
                                "lines_count", linesCount
                        ));

                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "创建文件失败：" + e.getMessage());
                    }
                })
                .build();
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<TextFileOpsToolkit, Builder> {

        private Path workspace;
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE_BYTES;
        private Charset charset = StandardCharsets.UTF_8;
        private boolean readOnly = false;

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

        /**
         * 设置是否为只读模式
         * <p>
         * 当设置为 true 时，只会安装查看和搜索工具（file$view, file$search），
         * 不会安装编辑工具。
         *
         * @param readOnly 是否只读
         * @return 当前构建器
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        @Override
        public TextFileOpsToolkit build() {
            // 如果未设置工作区，使用当前目录
            if (workspace == null) {
                workspace = Paths.get("").toAbsolutePath().normalize();
            }
            return new TextFileOpsToolkit(this);
        }
    }

    // ==================== Spec 数据结构 ====================

    record ViewSpec(
            @JsonPropertyDescription("文件的相对路径，例如：src/main.java")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("查看的行范围 [start, end]，从1开始计数（可选，不填则查看全部）")
            @JsonProperty("view_range")
            int[] viewRange,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record SearchSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("搜索关键词（简单文本匹配，大小写敏感）")
            @JsonProperty(value = "search_term", required = true)
            String searchTerm,

            @JsonPropertyDescription("上下文行数，返回匹配行的前后N行（可选，默认2）")
            @JsonProperty("context_lines")
            int contextLines,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record StrReplaceSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要替换的原始文本（必须在文件中唯一匹配）")
            @JsonProperty(value = "old_str", required = true)
            String oldStr,

            @JsonPropertyDescription("替换后的新文本")
            @JsonProperty(value = "new_str", required = true)
            String newStr,

            @JsonPropertyDescription("最后修改时间戳（从 file$view 获取）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record InsertLineSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("插入位置的行号（从1开始，<=0视为第1行前，>总行数视为末尾）")
            @JsonProperty(value = "line_number", required = true)
            int lineNumber,

            @JsonPropertyDescription("要插入的内容（可包含多行，用\\n分隔）")
            @JsonProperty(value = "content", required = true)
            String content,

            @JsonPropertyDescription("最后修改时间戳（从 file$view 获取）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
    ) {
    }

    record CreateFileSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("文件内容")
            @JsonProperty(value = "file_text", required = true)
            String fileText,

            @JsonPropertyDescription("是否覆盖已存在的文件（可选，默认false）")
            @JsonProperty("overwrite")
            boolean overwrite,

            @JsonPropertyDescription("文件编码（可选，默认UTF-8）")
            @JsonProperty("encoding")
            String encoding
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
        /**
         * 创建成功结果
         *
         * @param data 返回数据
         * @return 成功结果
         */
        static Result success(Object data) {
            return new Result(null, null, data);
        }

        /**
         * 创建错误结果
         *
         * @param error   错误码
         * @param message 错误消息
         * @return 错误结果
         */
        static Result error(String error, String message) {
            return new Result(error, message, null);
        }
    }

}
