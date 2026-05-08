package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.agent.util.FileUtils;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNullElse;

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
     * 默认小文本阈值（10KB），小于此值的文本直接返回内容
     */
    private static final long DEFAULT_SMALL_TEXT_THRESHOLD = 10 * 1024;

    /**
     * HTTP 客户端
     */
    private final OkHttpClient httpClient;

    /**
     * 工作区根路径（用于下载文件）
     */
    private final Path workspace;

    /**
     * 最大下载文件大小（字节）
     */
    private final long maxDownloadSize;

    /**
     * 小文本阈值（字节），小于此值的文本直接返回内容
     */
    private final long smallTextThreshold;

    /**
     * 是否只读模式
     */
    private final boolean readOnly;

    private HttpToolkit(Builder builder) {
        Objects.requireNonNull(builder.workspace, "workspace must not be null!");

        // 如果外部传入了 httpClient，直接使用；否则创建默认的
        this.httpClient = Objects.requireNonNullElseGet(
                builder.httpClient,
                () -> new OkHttpClient.Builder()
                        .connectTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                        .readTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                        .writeTimeout(builder.defaultTimeoutSeconds, TimeUnit.SECONDS)
                        .build());

        this.workspace = builder.workspace.toAbsolutePath().normalize();
        this.maxDownloadSize = builder.maxDownloadSize;
        this.smallTextThreshold = builder.smallTextThreshold;
        this.readOnly = builder.readOnly;
    }

    @Override
    public List<Tool> tools() {
        if (readOnly) {
            // 只读模式：仅返回 GET 请求
            return List.of(get(), download());
        } else {
            // 读写模式：返回所有工具
            return List.of(get(), post(), put(), delete());
        }
    }

    // ==================== Builder ====================

    public static HttpToolkit create() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<HttpToolkit, Builder> {

        private OkHttpClient httpClient;
        private Path workspace = Path.of("./");
        private int defaultTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        private long maxDownloadSize = DEFAULT_MAX_DOWNLOAD_SIZE;
        private long smallTextThreshold = DEFAULT_SMALL_TEXT_THRESHOLD;
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
         * 设置小文本阈值（字节），小于此值的文本直接返回内容
         *
         * @param smallTextThreshold 小文本阈值
         * @return 当前构建器
         */
        public Builder smallTextThreshold(long smallTextThreshold) {
            this.smallTextThreshold = smallTextThreshold;
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
            return new HttpToolkit(this);
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
                        - content_type: Content-Type
                        - response_time_ms: 响应时间（毫秒）
                        - body: 小文本直接返回文本内容
                        - file_uri: 大文本或二进制文件保存后的文件 URI（相对于 workspace）
                        - file_size: 文件大小（字节）
                        
                        【处理策略】
                        - 小文本（< %d 且为文本类型）：直接返回 body 字段
                        - 大文本（≥ %d）：保存到临时文件，返回 file_uri
                        - 二进制文件：保存到临时文件，返回 file_uri
                        - 超过最大限制（%d）：拒绝下载
                        
                        【注意事项】
                        - 适用于无副作用的数据获取
                        - 文件自动保存到 workspace 下的 http_cache 目录
                        """.formatted(smallTextThreshold, smallTextThreshold, maxDownloadSize))
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
                        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
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
                            final long fileSize = bytes.length;
                            result.put("file_size", fileSize);

                            // 检查是否超过最大下载限制
                            if (fileSize > maxDownloadSize) {
                                throw new IOException(String.format("Response size %.2f MB exceeds limit %.2f MB",
                                        fileSize / 1024.0 / 1024.0,
                                        maxDownloadSize / 1024.0 / 1024.0));
                            }

                            // 判断是否为文本内容
                            final boolean isText = isTextContent(contentType);
                            final boolean isSmallText = isText && fileSize < smallTextThreshold;

                            if (isSmallText) {
                                // 小文本：直接返回内容
                                result.put("body", new String(bytes, determineCharset(contentType)));
                                result.put("saved_to_file", false);
                            } else {
                                // 大文本或二进制：保存到文件
                                try {
                                    final Path cacheDir = workspace.resolve("http_cache");
                                    if (!Files.exists(cacheDir)) {
                                        Files.createDirectories(cacheDir);
                                    }

                                    // 生成唯一文件名
                                    final String extension = getFileExtension(contentType);
                                    final String fileName = "http_response_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
                                    final Path savePath = cacheDir.resolve(fileName);

                                    // 写入文件
                                    Files.write(savePath, bytes);

                                    // 返回文件 URI（相对于 workspace）
                                    final String fileUri = workspace.relativize(savePath).toString();
                                    result.put("file_uri", fileUri);
                                    result.put("saved_to_file", true);
                                    result.put("message", isText
                                            ? "Large text saved to file. Use file$info or file$view to view."
                                            : "Binary file saved. Use file$info to view file information.");

                                } catch (IOException ex) {
                                    throw new IOException("Failed to save response to file!", ex);
                                }
                            }

                            return result;
                        }

                    } catch (IOException e) {
                        throw ToolExecutionException.callFailed(
                                "http$get",
                                "GET request failed: " + spec.url(),
                                "Check the URL is correct and the server is accessible. Retry if this is a transient error.",
                                e
                        );
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
                            final String contentType = requireNonNullElse(spec.contentType(), "application/json");

                            requestBody = RequestBody.create(
                                    requireNonNullElse(spec.body(), ""),
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
                        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        throw ToolExecutionException.callFailed(
                                "http$post",
                                "POST request failed: " + spec.url(),
                                "Check the URL, request body, and headers. Retry if this is a transient error.",
                                e
                        );
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
                            final String contentType = requireNonNullElse(spec.contentType(), "application/json");
                            requestBody = RequestBody.create(
                                    requireNonNullElse(spec.body(), ""),
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
                        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        throw ToolExecutionException.callFailed(
                                "http$put",
                                "PUT request failed: " + spec.url(),
                                "Check the URL, request body, and headers. Retry if this is a transient error.",
                                e
                        );
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
                            final String contentType = requireNonNullElse(spec.contentType(), "application/json");

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
                        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            return readResponse(response, responseTime);

                        }

                    } catch (IOException e) {
                        throw ToolExecutionException.callFailed(
                                "http$delete",
                                "DELETE request failed: " + spec.url(),
                                "Check the URL is correct and the resource exists. Retry if this is a transient error.",
                                e
                        );
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
                            throw new IOException("File already exists. Set overwrite=true to override: " + spec.savePath());
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
                        try (Response response = httpClient.newCall(request).execute()) {
                            final long responseTime = System.currentTimeMillis() - startTime;
                            if (!response.isSuccessful()) {
                                throw new IOException("Download failed with status code: " + response.code());
                            }

                            final ResponseBody body = response.body();

                            // 检查文件大小
                            final long contentLength = body.contentLength();
                            if (contentLength > 0 && contentLength > maxDownloadSize) {
                                throw new IOException(String.format("File size %.2f MB exceeds limit %.2f MB",
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

                            return result;

                        }

                    } catch (SecurityException e) {
                        throw ToolExecutionException.callFailed(
                                "http$download",
                                "Access denied: " + e.getMessage(),
                                "Ensure the file path is within the workspace and you have write permissions.",
                                e
                        );
                    } catch (IOException e) {
                        throw ToolExecutionException.callFailed(
                                "http$download",
                                "Download failed: " + spec.url(),
                                "Check the URL is accessible and you have network connectivity. Retry if this is a transient error.",
                                e
                        );
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
                        throw new IOException("File not found: " + filePath);
                    }

                    final long fileSize = Files.size(resolved);
                    if (fileSize > 10 * 1024 * 1024) {
                        throw new IOException(String.format("File size %.2f MB exceeds limit 10MB", fileSize / 1024.0 / 1024.0));
                    }

                    final String fileName = resolved.getFileName().toString();
                    final String mimeType = Files.probeContentType(resolved);

                    builder.addFormDataPart(fieldName, fileName,
                            RequestBody.create(Files.readAllBytes(resolved),
                                    MediaType.parse(requireNonNullElse(mimeType, "application/octet-stream"))));

                } catch (IOException e) {
                    throw ToolExecutionException.callFailed(
                            "http$post",
                            "Failed to process file: " + filePath,
                            "Check the file path and ensure you have read permissions.",
                            e
                    );
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
                        throw new IOException("File not found: " + filePath);
                    }

                    final long fileSize = Files.size(resolved);
                    if (fileSize > 10 * 1024 * 1024) {
                        throw new IOException(String.format("File size %.2f MB exceeds limit 10MB", fileSize / 1024.0 / 1024.0));
                    }

                    final String fileName = resolved.getFileName().toString();
                    final String mimeType = Files.probeContentType(resolved);

                    builder.addFormDataPart(fieldName, fileName,
                            RequestBody.create(Files.readAllBytes(resolved),
                                    MediaType.parse(requireNonNullElse(mimeType, "application/octet-stream"))));

                } catch (IOException e) {
                    throw ToolExecutionException.callFailed(
                            "http$put",
                            "Failed to process file: " + filePath,
                            "Check the file path and ensure you have read permissions.",
                            e
                    );
                }
            });
        }

        return builder.build();
    }

    /**
     * 读取响应
     */
    private Map<String, Object> readResponse(Response response, long responseTime) throws IOException {
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

        return result;
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

    /**
     * 根据 Content-Type 获取文件扩展名
     */
    private String getFileExtension(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return ".txt";
        }

        // 移除 charset 等参数
        final String mainType = contentType.split(";")[0].trim().toLowerCase();

        return switch (mainType) {
            case "application/json" -> ".json";
            case "application/xml", "text/xml" -> ".xml";
            case "text/html" -> ".html";
            case "text/plain" -> ".txt";
            case "text/css" -> ".css";
            case "text/javascript", "application/javascript" -> ".js";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            case "application/pdf" -> ".pdf";
            case "application/zip" -> ".zip";
            case "application/gzip" -> ".gz";
            case "application/octet-stream" -> ".bin";
            default -> ".dat"; // 默认二进制文件
        };
    }

    // ==================== Spec 数据结构 ====================

    record GetSpec(
            @JsonPropertyDescription("目标 URL")
            @JsonProperty(value = "url", required = true)
            String url,

            @JsonPropertyDescription("自定义请求头 Map")
            @JsonProperty("headers")
            Map<String, String> headers
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
            Map<String, String> files
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
            Map<String, String> files
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
            Map<String, String> headers
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
            boolean overwrite
    ) {
    }

}
