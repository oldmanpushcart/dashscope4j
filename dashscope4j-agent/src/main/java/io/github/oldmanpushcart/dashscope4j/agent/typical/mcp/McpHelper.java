package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.stream.Collectors;

class McpHelper {

    /**
     * 判断结果是否为错误
     *
     * @param result 结果
     * @return TRUE | FALSE
     */
    public static boolean isErrorResult(McpSchema.CallToolResult result) {
        return null != result
               && null != result.isError()
               && result.isError();
    }

    /**
     * 解析结果中的文本
     *
     * @param result 结果
     * @return 文本
     */
    public static String parseResultText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining("\n"));
    }

}
