package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;


/**
 * 工具未找到异常
 */
public class FunctionToolNotFoundException extends RuntimeException {

    private final String functionName;

    public FunctionToolNotFoundException(String functionName) {
        super("Function tool not found: " + functionName);
        this.functionName = functionName;
    }

    public String functionName() {
        return functionName;
    }

}
