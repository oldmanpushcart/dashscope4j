package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 工具包
 * <p>
 * 提供 HTTP 请求能力给 LLM Agent 使用：
 * - http$get: GET 请求获取数据
 * - http$post: POST 请求提交数据（支持文件上传）
 * - http$put: PUT 请求更新资源（支持文件上传）
 * - http$delete: DELETE 请求删除资源
 * - http$download: 下载文件到工作区
 * </p>
 * <p>
 * 所有下载操作都限制在 workspace 范围内，防止目录穿越攻击。
 * OkHttpClient 可由外部传入，支持自定义配置。
 * </p>
 */
public class HttpToolkit implements Toolkit {

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 默认最大下载文件大小（50MB）
     */
    private static final long DEFAULT_MAX_DOWNLOAD_SIZE = 50 * 1024 * 1024;

    /**
     * HTTP 客户端
     */
    private final OkHttpClient httpClient;

    /**
     * 工作区根路径（用于下载文件）
     */
    private final Path workspace;

    /**
     * 默认超时时间（秒）
     */
    private final int defaultTimeoutSeconds;

    /**
     * 最大下载文件大小（字节）
     */
    private final long maxDownloadSize;

    /**
     * 是否只读模式
     */
    private final boolean readOnly;

    private HttpToolkit(Builder builder) {
        this.httpClient = builder.httpClient != null ? builder.httpClient : new OkHttpClient.Builder()
                .connectTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                .build();
        this.workspace = builder.workspace;
        this.defaultTimeoutSeconds = builder.defaultTimeoutSeconds;
        this.maxDownloadSize = builder.maxDownloadSize;
        this.readOnly = builder.readOnly;
    }

    @Override
    public List<Tool> tools() {
        if (readOnly) {
            // 只读模式：仅返回 GET 请求
            return List.of(get());
        } else {
            // 读写模式：返回所有工具
            return List.of(get(), post(), put(), delete(), download());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * http$get 工具
     */
    private FunctionTool get() {
        return FunctionTool.newBuilder()
                .name("http$get")
                .description("""
                        发送 HTTP GET 请求获取数据。
                        
                        【使用场景】
                        - 调用 REST API 获取数据
                        - 抓取网页内容
                        - 查询在线服务
                        
                        【返回结果】
                        - status_code: HTTP 状态码
                        - headers: 响应头 Map
                        - body: 响应体（文本或 base64 编码的二进制）
                        - content_type: Content-Type
                        - response_time_ms: 响应时间（毫秒）
                        
                        【注意事项】
                        - 适用于无副作用的数据获取
                        - 大响应体会被截断（最大 1MB）
                        - 二进制内容会自动 base64 编码
                        """)
                .parameterType(GetSpec.class)
                .<GetSpec>function((caller, spec) -> {
                    try {
                        final long startTime = System.currentTimeMillis();

                        // 构建请求
                        final Request.Builder requestBuilder = new Request.Builder()
                                .url(spec.url())
                                .get();

                        // 添加自定义请求头
                        if (spec.headers() != null && !spec.headers().isEmpty()) {
                            spec.headers().forEach(requestBuilder::addHeader);
                        }

                        // 执行请求
                        final int timeout = spec.timeout() != null && spec.timeout() > 0
                                ? spec.timeout()
                                : defaultTimeoutSeconds;

                        try (Response response = httpClient.newBuilder()
                                .readTimeout(timeout, TimeUnit.SECONDS)
                                .build()
                                .newCall(requestBuilder.build())
                                .execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            // 读取响应
                            final int statusCode = response.code();
                            final Headers headers = response.headers();
                            final ResponseBody body = response.body();

                            // 构建响应头 Map
                            final Map<String, String> headerMap = new HashMap<>();
                            for (String name : headers.names()) {
                                headerMap.put(name, headers.get(name));
                            }

                            // 读取响应体
                            final String contentType = headerMap.getOrDefault("Content-Type", "");
                            final Map<String, Object> result = new HashMap<>();
                            result.put("status_code", statusCode);
                            result.put("headers", headerMap);
                            result.put("content_type", contentType);
                            result.put("response_time_ms", responseTime);

                            final byte[] bytes = body.bytes();

                            // 检查响应体大小（限制 1MB）
                            if (bytes.length > 1024 * 1024) {
                                result.put("body", "[Response too large, truncated]");
                                result.put("truncated", true);
                                result.put("actual_size", bytes.length);
                            } else {
                                // 判断是否为文本
                                if (isTextContent(contentType)) {
                                    result.put("body", new String(bytes, determineCharset(contentType)));
                                    result.put("truncated", false);
                                } else {
                                    // 二进制内容 base64 编码
                                    result.put("body", Base64.getEncoder().encodeToString(bytes));
                                    result.put("encoding", "base64");
                                    result.put("truncated", false);
                                }
                            }

                            return Result.success(result);

                        }

                    } catch (IOException e) {
                        return Result.error("HTTP_ERROR", "GET 请求失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * http$post 工具
     */
    private FunctionTool post() {
        return FunctionTool.newBuilder()
                .name("http$post")
                .description("""
                        发送 HTTP POST 请求提交数据，支持文件上传。
                        
                        【使用场景】
                        - 提交表单数据
                        - 调用 API 创建资源
                        - 上传文件到服务器
                        
                        【参数说明】
                        - body: JSON 字符串或表单数据
                        - files: 文件上传 Map，key 为字段名，value 为 workspace 内的文件路径
                        - contentType: Content-Type（可选，默认 application/json）
                        
                        【文件上传】
                        如果提供 files 参数，会自动构建 multipart/form-data 请求：
                        - 读取 files 中的文件内容
                        - 自动设置 boundary 和 Content-Type
                        - body 参数作为额外表单字段
                        
                        【返回结果】
                        - status_code: HTTP 状态码
                        - headers: 响应头 Map
                        - body: 响应体
                        - response_time_ms: 响应时间
                        
                        【注意事项】
                        - 文件路径相对于 workspace
                        - 单个文件最大 10MB
                        """)
                .parameterType(PostSpec.class)
                .<PostSpec>function((caller, spec) -> {
                    try {
                        final long startTime = System.currentTimeMillis();

                        // 构建请求体
                        RequestBody requestBody;

                        if (spec.files() != null && !spec.files().isEmpty()) {
                            // multipart 文件上传
                            requestBody = buildMultipartBody(spec);
                        } else {
                            // 普通 POST
                            final String contentType = spec.contentType() != null
                                    ? spec.contentType()
                                    : "application/json";

                            requestBody = RequestBody.create(
                                    spec.body() != null ? spec.body() : "",
                                    MediaType.parse(contentType)
                            );
                        }

                        // 构建请求
                        final Request.Builder requestBuilder = new Request.Builder()
                                .url(spec.url())
                                .post(requestBody);

                        // 添加自定义请求头（不覆盖 Content-Type）
                        if (spec.headers() != null && !spec.headers().isEmpty()) {
                            spec.headers().forEach((key, value) -> {
                                if (!"Content-Type".equalsIgnoreCase(key)) {
                                    requestBuilder.addHeader(key, value);
                                }
                            });
                        }

                        // 执行请求
                        final int timeout = spec.timeout() != null && spec.timeout() > 0
                                ? spec.timeout()
                                : defaultTimeoutSeconds;

                        try (Response response = httpClient.newBuilder()
                                .readTimeout(timeout, TimeUnit.SECONDS)
                                .writeTimeout(timeout, TimeUnit.SECONDS)
                                .build()
                                .newCall(requestBuilder.build())
                                .execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        return Result.error("HTTP_ERROR", "POST 请求失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * http$put 工具
     */
    private FunctionTool put() {
        return FunctionTool.newBuilder()
                .name("http$put")
                .description("""
                        发送 HTTP PUT 请求更新资源，支持文件上传。
                        
                        【使用场景】
                        - 更新已有资源
                        - 上传覆盖文件
                        - 替换配置信息
                        
                        【参数说明】
                        - body: 更新的内容（JSON 或文本）
                        - files: 文件上传 Map（可选）
                        - contentType: Content-Type（可选）
                        
                        【返回结果】
                        - status_code: HTTP 状态码
                        - headers: 响应头 Map
                        - body: 响应体
                        - response_time_ms: 响应时间
                        
                        【注意事项】
                        - PUT 通常用于完整替换资源
                        - 与 POST 语义不同，不要混用
                        """)
                .parameterType(PutSpec.class)
                .<PutSpec>function((caller, spec) -> {
                    try {
                        final long startTime = System.currentTimeMillis();

                        // 构建请求体
                        RequestBody requestBody;

                        if (spec.files() != null && !spec.files().isEmpty()) {
                            // multipart 文件上传
                            requestBody = buildMultipartBodyForPut(spec);
                        } else {
                            // 普通 PUT
                            final String contentType = spec.contentType() != null
                                    ? spec.contentType()
                                    : "application/json";

                            requestBody = RequestBody.create(
                                    spec.body() != null ? spec.body() : "",
                                    MediaType.parse(contentType)
                            );
                        }

                        // 构建请求
                        final Request.Builder requestBuilder = new Request.Builder()
                                .url(spec.url())
                                .put(requestBody);

                        // 添加自定义请求头
                        if (spec.headers() != null && !spec.headers().isEmpty()) {
                            spec.headers().forEach((key, value) -> {
                                if (!"Content-Type".equalsIgnoreCase(key)) {
                                    requestBuilder.addHeader(key, value);
                                }
                            });
                        }

                        // 执行请求
                        final int timeout = spec.timeout() != null && spec.timeout() > 0
                                ? spec.timeout()
                                : defaultTimeoutSeconds;

                        try (Response response = httpClient.newBuilder()
                                .readTimeout(timeout, TimeUnit.SECONDS)
                                .writeTimeout(timeout, TimeUnit.SECONDS)
                                .build()
                                .newCall(requestBuilder.build())
                                .execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        return Result.error("HTTP_ERROR", "PUT 请求失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * http$delete 工具
     */
    private FunctionTool delete() {
        return FunctionTool.newBuilder()
                .name("http$delete")
                .description("""
                        发送 HTTP DELETE 请求删除资源。
                        
                        【使用场景】
                        - 删除数据库记录
                        - 取消订阅
                        - 移除资源
                        
                        【参数说明】
                        - url: 要删除的资源 URL
                        - body: 请求体（可选，某些 API 允许 DELETE 带 body）
                        - headers: 自定义请求头（可选）
                        
                        【返回结果】
                        - status_code: HTTP 状态码
                        - headers: 响应头 Map
                        - body: 响应体
                        - response_time_ms: 响应时间
                        
                        【注意事项】
                        - DELETE 是幂等操作
                        - 某些 API 可能不允许 DELETE 带 body
                        """)
                .parameterType(DeleteSpec.class)
                .<DeleteSpec>function((caller, spec) -> {
                    try {
                        final long startTime = System.currentTimeMillis();

                        // 构建请求
                        final Request.Builder requestBuilder = new Request.Builder()
                                .url(spec.url());

                        // 如果有 body，使用 DELETE with body
                        if (spec.body() != null && !spec.body().isEmpty()) {
                            final String contentType = spec.contentType() != null
                                    ? spec.contentType()
                                    : "application/json";

                            final RequestBody body = RequestBody.create(
                                    spec.body(),
                                    MediaType.parse(contentType)
                            );
                            requestBuilder.delete(body);
                        } else {
                            requestBuilder.delete();
                        }

                        // 添加自定义请求头
                        if (spec.headers() != null && !spec.headers().isEmpty()) {
                            spec.headers().forEach(requestBuilder::addHeader);
                        }

                        // 执行请求
                        final int timeout = spec.timeout() != null && spec.timeout() > 0
                                ? spec.timeout()
                                : defaultTimeoutSeconds;

                        try (Response response = httpClient.newBuilder()
                                .readTimeout(timeout, TimeUnit.SECONDS)
                                .build()
                                .newCall(requestBuilder.build())
                                .execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        return Result.error("HTTP_ERROR", "DELETE 请求失败：" + e.getMessage());
                    }
                })
                .build();
    }

    /**
     * http$download 工具
     */
    private FunctionTool download() {
        return FunctionTool.newBuilder()
                .name("http$download")
                .description("""
                        下载文件并保存到工作区。
                        
                        【使用场景】
                        - 下载图片、文档等资源
                        - 获取远程文件供后续处理
                        - 保存 API 返回的文件
                        
                        【参数说明】
                        - url: 下载地址
                        - savePath: 保存路径（相对于 workspace）
                        - overwrite: 是否覆盖已存在文件（默认 false）
                        - timeout: 超时时间（秒，可选）
                        
                        【返回结果】
                        - file_size: 文件大小（字节）
                        - saved_to: 实际保存路径
                        - content_type: Content-Type
                        - response_time_ms: 下载耗时
                        
                        【注意事项】
                        - 文件保存在 workspace 范围内
                        - 默认不覆盖已存在文件
                        - 最大下载大小可配置（默认 50MB）
                        - 下载完成后可用 file$view 查看文本文件
                        """)
                .parameterType(DownloadSpec.class)
                .<DownloadSpec>function((caller, spec) -> {
                    try {
                        final long startTime = System.currentTimeMillis();

                        // 验证保存路径
                        final Path savePath = FileUtils.checkPathEscape(workspace, spec.savePath());

                        // 检查文件是否已存在
                        if (Files.exists(savePath) && !spec.overwrite()) {
                            return Result.error("FILE_EXISTS",
                                    "文件已存在，设置 overwrite=true 以覆盖：" + spec.savePath());
                        }

                        // 确保父目录存在
                        final Path parent = savePath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        // 构建请求
                        final Request request = new Request.Builder()
                                .url(spec.url())
                                .get()
                                .build();

                        // 执行请求
                        final int timeout = spec.timeout() != null && spec.timeout() > 0
                                ? spec.timeout()
                                : defaultTimeoutSeconds;

                        try (Response response = httpClient.newBuilder()
                                .readTimeout(timeout, TimeUnit.SECONDS)
                                .build()
                                .newCall(request)
                                .execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            if (!response.isSuccessful()) {
                                return Result.error("HTTP_ERROR",
                                        "下载失败，状态码：" + response.code());
                            }

                            final ResponseBody body = response.body();

                            // 检查文件大小
                            final long contentLength = body.contentLength();
                            if (contentLength > 0 && contentLength > maxDownloadSize) {
                                return Result.error("SIZE_EXCEEDED",
                                        String.format("文件大小 %.2f MB 超过限制 %.2f MB",
                                                contentLength / 1024.0 / 1024.0,
                                                maxDownloadSize / 1024.0 / 1024.0));
                            }

                            // 写入文件
                            try (BufferedSink sink = Okio.buffer(Okio.sink(savePath))) {
                                sink.writeAll(body.source());
                            }

                            final long fileSize = Files.size(savePath);
                            final String contentType = response.header("Content-Type", "");

                            final Map<String, Object> result = new HashMap<>();
                            result.put("file_size", fileSize);
                            result.put("saved_to", spec.savePath());
                            result.put("content_type", contentType);
                            result.put("response_time_ms", responseTime);

                            return Result.success(result);

                        }

                    } catch (SecurityException e) {
                        return Result.error("ACCESS_DENIED", e.getMessage());
                    } catch (IOException e) {
                        return Result.error("IO_ERROR", "下载失败：" + e.getMessage());
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建 multipart 请求体（POST）
     */
    private MultipartBody buildMultipartBody(PostSpec spec) throws IOException {
        final MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 添加 body 作为表单字段
        if (spec.body() != null && !spec.body().isEmpty()) {
            builder.addFormDataPart("data", spec.body());
        }

        // 添加文件
        if (spec.files() != null) {
            spec.files().forEach((fieldName, filePath) -> {
                try {
                    final Path resolved = FileUtils.checkPathEscape(workspace, filePath);

                    if (!Files.exists(resolved)) {
                        throw new IOException("文件不存在：" + filePath);
                    }

                    final long fileSize = Files.size(resolved);
                    if (fileSize > 10 * 1024 * 1024) {
                        throw new IOException(String.format("文件 %.2f MB 超过限制 10MB", fileSize / 1024.0 / 1024.0));
                    }

                    final String fileName = resolved.getFileName().toString();
                    final String mimeType = Files.probeContentType(resolved);

                    builder.addFormDataPart(fieldName, fileName,
                            RequestBody.create(Files.readAllBytes(resolved),
                                    MediaType.parse(mimeType != null ? mimeType : "application/octet-stream")));

                } catch (IOException e) {
                    throw new RuntimeException("处理文件失败：" + filePath, e);
                }
            });
        }

        return builder.build();
    }

    /**
     * 构建 multipart 请求体（PUT）
     */
    private MultipartBody buildMultipartBodyForPut(PutSpec spec) throws IOException {
        final MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 添加 body 作为表单字段
        if (spec.body() != null && !spec.body().isEmpty()) {
            builder.addFormDataPart("data", spec.body());
        }

        // 添加文件
        if (spec.files() != null) {
            spec.files().forEach((fieldName, filePath) -> {
                try {
                    final Path resolved = FileUtils.checkPathEscape(workspace, filePath);

                    if (!Files.exists(resolved)) {
                        throw new IOException("文件不存在：" + filePath);
                    }

                    final long fileSize = Files.size(resolved);
                    if (fileSize > 10 * 1024 * 1024) {
                        throw new IOException(String.format("文件 %.2f MB 超过限制 10MB", fileSize / 1024.0 / 1024.0));
                    }

                    final String fileName = resolved.getFileName().toString();
                    final String mimeType = Files.probeContentType(resolved);

                    builder.addFormDataPart(fieldName, fileName,
                            RequestBody.create(Files.readAllBytes(resolved),
                                    MediaType.parse(mimeType != null ? mimeType : "application/octet-stream")));

                } catch (IOException e) {
                    throw new RuntimeException("处理文件失败：" + filePath, e);
                }
            });
        }

        return builder.build();
    }

    /**
     * 读取响应
     */
    private Result readResponse(Response response, long responseTime) throws IOException {
        final int statusCode = response.code();
        final Headers headers = response.headers();
        final ResponseBody body = response.body();

        // 构建响应头 Map
        final Map<String, String> headerMap = new HashMap<>();
        for (String name : headers.names()) {
            headerMap.put(name, headers.get(name));
        }

        // 读取响应体
        final Map<String, Object> result = new HashMap<>();
        result.put("status_code", statusCode);
        result.put("headers", headerMap);
        result.put("response_time_ms", responseTime);

        final byte[] bytes = body.bytes();
        final String contentType = headerMap.getOrDefault("Content-Type", "");

        if (isTextContent(contentType)) {
            result.put("body", new String(bytes, determineCharset(contentType)));
        } else {
            result.put("body", Base64.getEncoder().encodeToString(bytes));
            result.put("encoding", "base64");
        }
        result.put("content_type", contentType);

        return Result.success(result);
    }

    /**
     * 判断是否为文本内容
     */
    private boolean isTextContent(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return true; // 默认视为文本
        }
        return contentType.startsWith("text/") ||
                contentType.contains("json") ||
                contentType.contains("xml") ||
                contentType.contains("javascript");
    }

    /**
     * 确定字符编码
     */
    private java.nio.charset.Charset determineCharset(String contentType) {
        if (contentType != null && contentType.contains("charset=")) {
            try {
                final String charsetName = contentType.substring(contentType.indexOf("charset=") + 8);
                return java.nio.charset.Charset.forName(charsetName);
            } catch (Exception e) {
                // 忽略，使用默认
            }
        }
        return java.nio.charset.StandardCharsets.UTF_8;
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements io.github.oldmanpushcart.dashscope4j.client.util.Buildable<HttpToolkit, Builder> {

        private OkHttpClient httpClient;
        private Path workspace;
        private int defaultTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        private long maxDownloadSize = DEFAULT_MAX_DOWNLOAD_SIZE;
        private boolean readOnly = false;

        /**
         * 设置自定义 OkHttpClient
         *
         * @param httpClient HTTP 客户端
         * @return 当前构建器
         */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * 设置工作区根路径（用于下载文件）
         *
         * @param workspace 工作区路径
         * @return 当前构建器
         */
        public Builder workspace(Path workspace) {
            this.workspace = workspace.toAbsolutePath().normalize();
            return this;
        }

        /**
         * 设置默认超时时间（秒）
         *
         * @param timeoutSeconds 超时时间
         * @return 当前构建器
         */
        public Builder defaultTimeoutSeconds(int timeoutSeconds) {
            this.defaultTimeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * 设置最大下载文件大小（字节）
         *
         * @param maxDownloadSize 最大文件大小
         * @return 当前构建器
         */
        public Builder maxDownloadSize(long maxDownloadSize) {
            this.maxDownloadSize = maxDownloadSize;
            return this;
        }

        /**
         * 设置是否为只读模式
         * <p>
         * 当设置为 true 时，只会安装 http$get 工具，
         * 不会安装 POST/PUT/DELETE/DOWNLOAD 等写操作工具。
         *
         * @param readOnly 是否只读
         * @return 当前构建器
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        @Override
        public HttpToolkit build() {
            // 如果未设置工作区，使用当前目录
            if (workspace == null) {
                workspace = Paths.get("").toAbsolutePath().normalize();
            }
            return new HttpToolkit(this);
        }
    }

    // ==================== Spec 数据结构 ====================

    record GetSpec(
            @JsonPropertyDescription("目标 URL")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("自定义请求头 Map")
            @JsonProperty("headers")
            Map<String, String> headers,

            @JsonPropertyDescription("超时时间（秒），不填则使用默认值")
            @JsonProperty("timeout")
            Integer timeout
    ) {
    }

    record PostSpec(
            @JsonPropertyDescription("目标 URL")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("请求体（JSON 字符串或表单数据）")
            @JsonProperty("body")
            String body,

            @JsonPropertyDescription("Content-Type（可选，默认 application/json）")
            @JsonProperty("content_type")
            String contentType,

            @JsonPropertyDescription("自定义请求头 Map")
            @JsonProperty("headers")
            Map<String, String> headers,

            @JsonPropertyDescription("文件上传 Map，key 为字段名，value 为 workspace 内的文件路径")
            @JsonProperty("files")
            Map<String, String> files,

            @JsonPropertyDescription("超时时间（秒），不填则使用默认值")
            @JsonProperty("timeout")
            Integer timeout
    ) {
    }

    record PutSpec(
            @JsonPropertyDescription("目标 URL")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("请求体")
            @JsonProperty("body")
            String body,

            @JsonPropertyDescription("Content-Type（可选）")
            @JsonProperty("content_type")
            String contentType,

            @JsonPropertyDescription("自定义请求头 Map")
            @JsonProperty("headers")
            Map<String, String> headers,

            @JsonPropertyDescription("文件上传 Map（可选）")
            @JsonProperty("files")
            Map<String, String> files,

            @JsonPropertyDescription("超时时间（秒），不填则使用默认值")
            @JsonProperty("timeout")
            Integer timeout
    ) {
    }

    record DeleteSpec(
            @JsonPropertyDescription("目标 URL")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("请求体（可选，某些 API 允许 DELETE 带 body）")
            @JsonProperty("body")
            String body,

            @JsonPropertyDescription("Content-Type（可选）")
            @JsonProperty("content_type")
            String contentType,

            @JsonPropertyDescription("自定义请求头 Map")
            @JsonProperty("headers")
            Map<String, String> headers,

            @JsonPropertyDescription("超时时间（秒），不填则使用默认值")
            @JsonProperty("timeout")
            Integer timeout
    ) {
    }

    record DownloadSpec(
            @JsonPropertyDescription("下载地址")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("保存路径（相对于 workspace）")
            @JsonProperty(value = "save_path", required = true)
            String savePath,

            @JsonPropertyDescription("是否覆盖已存在文件（默认 false）")
            @JsonProperty("overwrite")
            boolean overwrite,

            @JsonPropertyDescription("超时时间（秒），不填则使用默认值")
            @JsonProperty("timeout")
            Integer timeout
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
        static Result success(Object data) {
            return new Result(null, null, data);
        }

        static Result error(String error, String message) {
            return new Result(error, message, null);
        }
    }

}
