package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class McpResourceFunctionTool implements FunctionTool {

    private static final Type mapType = new TypeReference<HashMap<String, Object>>() {
    }.getType();

    private final McpAsyncClient client;
    private final McpSchema.Resource mcpResource;
    private final Meta meta;

    public McpResourceFunctionTool(McpAsyncClient client, McpSchema.Resource mcpResource) {
        this.client = client;
        this.mcpResource = mcpResource;
        this.meta = newMeta(mcpResource);
    }

    /**
     * 构建函数元数据
     * <p>
     * 从 Resource 的定义中提取参数 schema
     * </p>
     *
     * @param mcpResource MCP Resource 定义
     * @return 函数元数据
     */
    private static Meta newMeta(McpSchema.Resource mcpResource) {
        // Resource 通常需要一个 uri 参数来读取
        final var parameterSchema = JacksonJsonUtils.toNode(new ResourceArgumentsSchema(mcpResource));
        return new Meta(
                mcpResource.name(),
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
        final var serverInfo = client.getServerInfo();
        final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpResource.name());

        // 解析用户输入的参数
        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapType);

        // 获取 resource URI，如果没有提供则使用默认的 URI
        final var uri = (String) argumentMap.getOrDefault("uri", mcpResource.uri());

        // 调用 MCP 服务器的 readResource API
        final var request = new McpSchema.ReadResourceRequest(uri);

        return client.readResource(request)
                .toFuture()
                .thenApply(result -> {
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
                .thenApply(McpSchema.ReadResourceResult::contents)
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
            // 尝试获取文本内容（使用反射兼容不同版本）
            String text = null;
            byte[] blob = null;
            String mimeType = null;
            
            try {
                final var getTextMethod = content.getClass().getMethod("getText");
                text = (String) getTextMethod.invoke(content);
                final var getMimeTypeMethod = content.getClass().getMethod("getMimeType");
                mimeType = (String) getMimeTypeMethod.invoke(content);
            } catch (Exception e) {
                // 不是 Text 类型
            }
            
            if (text == null) {
                try {
                    final var getBlobMethod = content.getClass().getMethod("getBlob");
                    blob = (byte[]) getBlobMethod.invoke(content);
                    final var getMimeTypeMethod = content.getClass().getMethod("getMimeType");
                    mimeType = (String) getMimeTypeMethod.invoke(content);
                } catch (Exception e) {
                    // 也不是 Blob 类型
                }
            }
            
            if (text != null) {
                return processTextContent(resultMap, text, mimeType);
            } else if (blob != null) {
                return processBlobContent(resultMap, blob, mimeType);
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
                .map(content -> {
                    try {
                        final var getTextMethod = content.getClass().getMethod("getText");
                        return (String) getTextMethod.invoke(content);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(text -> text != null)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    /**
     * Resource 参数的 JSON Schema 表示
     */
    private record ResourceArgumentsSchema(

            @JsonProperty("type")
            String type,

            @JsonProperty("properties")
            Map<String, ArgumentProperty> properties,

            @JsonProperty("required")
            List<String> required

    ) {
        public ResourceArgumentsSchema(McpSchema.Resource resource) {
            this(
                    "object",
                    Map.of(
                            "uri", new ArgumentProperty("string", "Resource URI to read (default: " + resource.uri() + ")")
                    ),
                    List.of() // uri 是可选的
            );
        }
    }

    /**
     * 参数属性定义
     */
    private record ArgumentProperty(
            @JsonProperty("type")
            String type,

            @JsonProperty("description")
            String description
    ) {
    }
}
