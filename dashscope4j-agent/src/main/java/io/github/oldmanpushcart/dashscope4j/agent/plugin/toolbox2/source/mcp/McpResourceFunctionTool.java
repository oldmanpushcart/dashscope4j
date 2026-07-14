package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * MCP Resource 函数工具
 * <p>
 * 将 MCP 的 Resource 包装为 FunctionTool，调用时会：
 * 1. 接收用户输入的参数（通常是 resource URI）
 * 2. 调用 MCP 服务器的 readResource API 读取资源内容
 * 3. 根据内容类型（Text/Blob）保存到临时文件
 * 4. 返回临时文件的 URI，Text 类型还会返回编码信息
 * </p>
 */
class McpResourceFunctionTool implements McpFunctionTool {

    private static final TypeReference<HashMap<String, Object>> mapType = new TypeReference<>() {
    };

    private final McpAsyncClient mcpClient;
    private final McpSchema.Resource mcpResource;
    private final Meta meta;

    public McpResourceFunctionTool(String namespace, McpAsyncClient mcpClient, McpSchema.Resource mcpResource) {
        this.mcpClient = mcpClient;
        this.mcpResource = mcpResource;
        this.meta = newMeta(namespace, mcpResource);
    }

    @Override
    public Type type() {
        return Type.RESOURCE;
    }

    /**
     * 构建函数元数据
     * <p>
     * 从 Resource 的定义中提取参数 schema
     * </p>
     *
     * @param namespace   函数命名空间
     * @param mcpResource MCP Resource 定义
     * @return 函数元数据
     */
    private static Meta newMeta(String namespace, McpSchema.Resource mcpResource) {
        // Resource 通常需要一个 uri 参数来读取
        final var parameterSchema = JacksonJsonUtils.toNode(McpSchemaHelper.buildResourceArgumentsSchema(mcpResource));
        return new Meta(
                "mcp$%s$resource$%s".formatted(namespace, mcpResource.name()),
                mcpResource.description() != null ? mcpResource.description() : "MCP Resource: " + mcpResource.name(),
                parameterSchema
        );
    }

    @Override
    public Meta meta() {
        return meta;
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
        final var serverInfo = mcpClient.getServerInfo();
        final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpResource.name());

        // 解析用户输入的参数
        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapType.getType());

        // 获取 resource URI，如果没有提供则使用默认的 URI
        final var uri = (String) argumentMap.getOrDefault("uri", mcpResource.uri());

        // 调用 MCP 服务器的 readResource API
        final var request = McpSchema.ReadResourceRequest.builder(uri)
                .build();

        return mcpClient.readResource(request)
                .toFuture()
                .thenApply(result -> {

                    if (null == result) {
                        throw new IllegalStateException("Reading resource: /%s failed: result is null!".formatted(
                                prefix
                        ));
                    }

                    // 检查是否有错误
                    if (isErrorResult(result)) {
                        throw new IllegalStateException("Reading resource: /%s failed: %s".formatted(
                                prefix,
                                parseResultText(result)
                        ));
                    }
                    return result;
                })
                // 处理资源内容，保存到临时文件
                .thenApply(readResourceResult -> Objects.requireNonNull(readResourceResult).contents())
                .thenApply(this::processContents);
    }

    /**
     * 处理资源内容列表
     *
     * @param contents 资源内容列表
     * @return JSON 格式的文件信息
     */
    private String processContents(List<McpSchema.ResourceContents> contents) {
        return JacksonJsonUtils.toJson(contents.stream()
                .map(this::processContent)
                .toList());
    }

    /**
     * 处理单个资源内容
     *
     * @param content 资源内容
     * @return 文件信息 Map
     */
    private Map<String, Object> processContent(McpSchema.ResourceContents content) {
        final var resultMap = new HashMap<String, Object>();

        try {

            // 处理文本内容
            if (content instanceof McpSchema.TextResourceContents textContent) {
                return processTextContent(resultMap, textContent.text(), textContent.mimeType());
            }

            // 处理二进制内容
            else if (content instanceof McpSchema.BlobResourceContents blobContent) {
                return processBlobContent(resultMap, blobContent.blob().getBytes(StandardCharsets.UTF_8), blobContent.mimeType());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to process resource content", e);
        }

        return resultMap;
    }

    /**
     * 处理文本内容
     */
    private Map<String, Object> processTextContent(Map<String, Object> resultMap, String text, String mimeType) throws IOException {
        final var tempFile = Files.createTempFile("mcp-resource-", ".txt");
        Files.writeString(tempFile, text, StandardCharsets.UTF_8);

        resultMap.put("type", "text");
        resultMap.put("uri", tempFile.toUri().toString());
        resultMap.put("encoding", StandardCharsets.UTF_8.name());
        resultMap.put("mimeType", mimeType != null ? mimeType : "text/plain");

        return resultMap;
    }

    /**
     * 处理二进制内容
     */
    private Map<String, Object> processBlobContent(Map<String, Object> resultMap, byte[] blob, String mimeType) throws IOException {
        final var tempFile = Files.createTempFile("mcp-resource-", ".bin");
        Files.write(tempFile, blob);

        resultMap.put("type", "blob");
        resultMap.put("uri", tempFile.toUri().toString());
        resultMap.put("mimeType", mimeType != null ? mimeType : "application/octet-stream");

        return resultMap;
    }

    /**
     * 判断结果是否为错误
     *
     * @param result 结果
     * @return TRUE | FALSE
     */
    private static boolean isErrorResult(McpSchema.ReadResourceResult result) {
        // 如果 contents 为空则表示出错
        return null == result || null == result.contents() || result.contents().isEmpty();
    }

    /**
     * 解析结果中的文本
     *
     * @param result 结果
     * @return 文本
     */
    private static String parseResultText(McpSchema.ReadResourceResult result) {
        if (null == result || null == result.contents()) {
            return "Unknown error";
        }
        return result.contents().stream()
                .filter(content -> content instanceof McpSchema.TextResourceContents)
                .map(content -> ((McpSchema.TextResourceContents) content).text())
                .collect(java.util.stream.Collectors.joining("\n"));
    }


}
