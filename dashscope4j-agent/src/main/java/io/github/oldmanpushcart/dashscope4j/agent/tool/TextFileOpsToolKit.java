package io.github.oldmanpushcart.dashscope4j.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * 文本文件操作工具包
 * <p>
 * 提供文本文件的精细化编辑能力：
 * - read: 读取文本文件（支持分页、编码检测）
 * - write: 覆盖写入文本文件（防误覆盖、并发控制）
 * - append: 末尾追加文本内容
 * - replace: 搜索并替换文本内容
 * </p>
 */
public class TextFileOpsToolKit implements ToolKit {

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

    /**
     * 是否只读模式
     */
    private final boolean readOnly;

    private TextFileOpsToolKit(Builder builder) {
        this.workspace = builder.workspace;
        this.maxLines = builder.maxLines;
        this.maxFileSize = builder.maxFileSize;
        this.charset = builder.charset;
        this.readOnly = builder.readOnly;
    }

    @Override
    public List<Tool> tools() {
        if (readOnly) {
            // 只读模式：仅返回读取工具
            return List.of(read());
        } else {
            // 读写模式：返回所有工具
            return List.of(read(), write(), append(), replace());
        }
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
                        - has_more: 是否还有更多内容
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
                            return Result.error("BINARY_FILE", "无法读取二进制文件：" + spec.path());
                        }

                        final int offset = Math.max(0, spec.offsetLines());
                        final int limit = spec.limitLines() > 0 ? Math.min(spec.limitLines(), maxLines) : DEFAULT_READ_LINES;

                        // 读取文件内容
                        final List<String> content = new ArrayList<>();
                        boolean hasMore = false;

                        try (BufferedReader reader = Files.newBufferedReader(resolved, charset)) {

                            // 跳过 offset 行并读取 limit 行
                            String line;
                            int current = 0;
                            while ((line = reader.readLine()) != null) {
                                current++;

                                // 跳过 offset 行
                                if (current <= offset) {
                                    continue;
                                }

                                // 还有更多内容
                                if (current > offset + limit) {
                                    hasMore = true;
                                    break;
                                }

                                // 添加行到内容
                                content.add(line);

                            }

                        }

                        final String text = String.join("\n", content);
                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        return Result.success(Map.of(
                                "content", text,
                                "lines_returned", content.size(),
                                "has_more", hasMore,
                                "last_modified", lastModified
                        ));

                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "读取文件失败：" + e.getMessage());
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
                            return Result.error("INVALID_CONTENT", "文件内容不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        // 如果文件已经存在
                        if (Files.exists(resolved)) {

                            // 检查并发修改
                            FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                            // 如果文件已经存在则检查是否设置了覆盖改写
                            if (!spec.overwrite()) {
                                return Result.error("FILE_EXISTS", "文件已存在，设置 overwrite=true 以覆盖：" + spec.path());
                            }

                        }

                        // 如果文件不存在，则按照新文件处理
                        // 新文件的last_modified = 0
                        else {
                            if (spec.lastModified() != 0) {
                                return Result.error("INVALID_TIMESTAMP", "新文件的 last_modified 应为 0，当前值：" + spec.lastModified());
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
                        Files.writeString(resolved, spec.content(), fileCharset);

                        final long bytesWritten = Files.size(resolved);
                        final long linesCount = spec.content().split("\\n", -1).length;
                        final String operation = Files.exists(resolved) ? "overwritten" : "created";

                        return Result.success(Map.of(
                                "operation", operation,
                                "bytes_written", bytesWritten,
                                "lines_count_written", linesCount
                        ));

                    } catch (SecurityException e) {
                        return Result.error("SECURITY_VIOLATION", e.getMessage());
                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "写入文件失败：" + e.getMessage());
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
                        - lines_count_appended: 追加的行数
                        
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
                            return Result.error("INVALID_CONTENT", "追加内容不能为空");
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

                        // 追加文件写入
                        Files.writeString(resolved, contentToAppend, fileCharset, CREATE, APPEND);
                        final long bytesAppended = contentToAppend.getBytes(StandardCharsets.UTF_8).length;
                        final int linesCountAppended = contentToAppend.split("\\n", -1).length;

                        return Result.success(Map.of(
                                "bytes_appended", bytesAppended,
                                "lines_count_appended", linesCountAppended
                        ));

                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "追加内容失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * 搜索并替换文本内容
     */
    public FunctionTool replace() {
        return FunctionTool.newBuilder()
                .name("text_file$replace")
                .description("""
                        在文件中搜索并替换文本内容。
                        
                        【使用场景】
                        - 批量替换变量名或函数名
                        - 修改配置项的值
                        - 更新文档中的特定文本
                        
                        【搜索模式】
                        - 精确匹配：search_text="oldValue"
                        - 正则匹配：use_regex=true, search_text="old\\w+"
                        
                        【返回结果】
                        - operation: 操作类型（replace）
                        - replacements_count: 替换次数
                        - last_modified: 修改后的时间戳
                        
                        【典型工作流】
                        1. read(path="Main.java", offset_lines=20, limit_lines=10)
                           → 查看上下文，确认要替换的内容
                        2. replace(path="Main.java", search_text="oldMethod",
                                      replacement="newMethod", replace_all=true)
                        
                        【注意事项】
                        - 必须先调用 read 获取 last_modified
                        - replace_all=false 时只替换第一个匹配项
                        - 使用正则时注意转义特殊字符
                        - 大小写敏感
                        """)
                .parameterType(ReplaceTextSpec.class)
                .<ReplaceTextSpec>function((caller, spec) -> {
                    try {
                        // 验证内容
                        if (spec.searchText() == null || spec.searchText().isEmpty()) {
                            throw new IllegalArgumentException("搜索文本不能为空");
                        }

                        final Path resolved = FileUtils.checkPathEscape(workspace, spec.path());

                        if (!Files.exists(resolved)) {
                            throw new FileNotFoundException("文件不存在：" + spec.path());
                        }

                        // 并发控制校验
                        FileUtils.checkFileUnmodified(resolved, spec.lastModified());

                        // 确定编码
                        final Charset fileCharset = spec.encoding() != null && !spec.encoding().isEmpty()
                                ? Charset.forName(spec.encoding())
                                : charset;

                        // 读取文件内容
                        final String content = Files.readString(resolved, fileCharset);

                        // 准备替换
                        final String replacement = spec.replacement() != null ? spec.replacement() : "";
                        final String newContent;
                        int replacementsCount;

                        // 正则替换
                        if (spec.useRegex()) {
                            try {
                                final Pattern pattern = Pattern.compile(spec.searchText());
                                final Matcher matcher = pattern.matcher(content);

                                // 全部替换
                                if (spec.replaceAll()) {
                                    newContent = matcher.replaceAll(replacement);
                                    // 计算替换次数
                                    replacementsCount = 0;
                                    final Matcher countMatcher = pattern.matcher(content);
                                    while (countMatcher.find()) {
                                        replacementsCount++;
                                    }
                                }
                                // 只替换最初匹配
                                else {
                                    newContent = matcher.replaceFirst(replacement);
                                    replacementsCount = matcher.hitEnd() ? 0 : 1;
                                }
                            } catch (PatternSyntaxException e) {
                                throw new IllegalArgumentException("正则表达式语法错误：" + e.getMessage(), e);
                            }
                        }
                        // 精确替换
                        else {
                            // 全部替换
                            if (spec.replaceAll()) {
                                newContent = content.replace(spec.searchText(), replacement);
                                replacementsCount = (content.length() - newContent.length()) / spec.searchText().length();
                            }
                            // 只替换最初匹配
                            else {
                                final int index = content.indexOf(spec.searchText());
                                if (index == -1) {
                                    // 未找到匹配项，返回成功但替换次数为0
                                    final long lastModified = Files.getLastModifiedTime(resolved).toMillis();
                                    return Result.success(Map.of(
                                            "operation", "replace",
                                            "replacements_count", 0,
                                            "last_modified", lastModified
                                    ));
                                }
                                newContent = content.substring(0, index) + replacement + content.substring(index + spec.searchText().length());
                                replacementsCount = 1;
                            }
                        }

                        // 如果没有变化，直接返回
                        if (newContent.equals(content)) {
                            final long lastModified = Files.getLastModifiedTime(resolved).toMillis();
                            return Result.success(Map.of(
                                    "operation", "replace",
                                    "replacements_count", 0,
                                    "last_modified", lastModified
                            ));
                        }

                        // 写回文件
                        Files.writeString(resolved, newContent, fileCharset);
                        final long lastModified = Files.getLastModifiedTime(resolved).toMillis();

                        return Result.success(Map.of(
                                "operation", "replace",
                                "replacements_count", replacementsCount,
                                "last_modified", lastModified
                        ));

                    } catch (SecurityException e) {
                        return Result.error("SECURITY_VIOLATION", e.getMessage());
                    } catch (IllegalArgumentException e) {
                        return Result.error("INVALID_CONTENT", e.getMessage());
                    } catch (FileNotFoundException e) {
                        return Result.error("FILE_NOT_FOUND", e.getMessage());
                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "替换文本失败：" + e.getMessage());
                    }
                })
                .build();
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<TextFileOpsToolKit, Builder> {

        private Path workspace;
        private int maxLines = DEFAULT_MAX_READ_LINES;
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

        /**
         * 设置是否为只读模式
         * <p>
         * 当设置为 true 时，只会安装读取相关的工具（text_file$read），
         * 不会安装写入、追加和替换工具。
         *
         * @param readOnly 是否只读
         * @return 当前构建器
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        @Override
        public TextFileOpsToolKit build() {
            // 如果未设置工作区，使用当前目录
            if (workspace == null) {
                workspace = Paths.get("").toAbsolutePath().normalize();
            }
            return new TextFileOpsToolKit(this);
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
            int limitLines
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

    record ReplaceTextSpec(
            @JsonPropertyDescription("文件的相对路径")
            @JsonProperty(value = "path", required = true)
            String path,

            @JsonPropertyDescription("要搜索的文本内容")
            @JsonProperty(value = "search_text", required = true)
            String searchText,

            @JsonPropertyDescription("替换后的文本内容（可选，默认为空字符串）")
            @JsonProperty("replacement")
            String replacement,

            @JsonPropertyDescription("是否替换所有匹配项（可选，默认false只替换第一个）")
            @JsonProperty("replace_all")
            boolean replaceAll,

            @JsonPropertyDescription("是否使用正则表达式（可选，默认false）")
            @JsonProperty("use_regex")
            boolean useRegex,

            @JsonPropertyDescription("最后修改时间戳（从read获取）")
            @JsonProperty(value = "last_modified", required = true)
            long lastModified,

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
