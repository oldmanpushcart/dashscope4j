package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;

import lombok.Getter;

/**
 * 工具未找到异常
 *
 * @since 3.1.3
 */
@Getter
public class FunctionToolNotFoundException extends RuntimeException {

    private final String functionName;

    public FunctionToolNotFoundException(String functionName) {
        super("Function tool not found: " + functionName);
        this.functionName = functionName;
    }

}
