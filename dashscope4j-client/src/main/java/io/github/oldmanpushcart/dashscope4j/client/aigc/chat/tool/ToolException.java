package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import java.util.Map;

public class ToolException extends RuntimeException {

    public static final String TOOL_NOT_FOUND = "TOOL-NOT-FOUND";
    public static final String TOOL_MARSHAL_FAILED = "TOOL-MARSHAL-FAILED";
    public static final String TOOL_UNMARSHAL_FAILED = "TOOL-UNMARSHAL-FAILED";
    public static final String TOOL_CALL_FAILED = "TOOL-CALL-FAILED";

    private final String code;
    private final String desc;

    public ToolException(String code, String desc) {
        super(toMessage(code, desc));
        this.code = code;
        this.desc = desc;
    }

    public ToolException(String code, String desc, Throwable cause) {
        super(toMessage(code, desc), cause);
        this.code = code;
        this.desc = desc;
    }

    private static String toMessage(String code, String desc) {
        return code + " : " + desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Object toResult() {
        return Map.of(
                "code", code,
                "desc", desc,
                "detail", getLocalizedMessage(),
                "suggestion", switch (code){
                    case TOOL_NOT_FOUND -> "Please check the tool name.";
                    case TOOL_MARSHAL_FAILED -> "Please check the tool definition.";
                    case TOOL_UNMARSHAL_FAILED -> "Please check the tool response.";
                    case TOOL_CALL_FAILED -> "Please check the tool implementation.";
                    default -> "Please check the tool status.";
                }
        );
    }

    public static ToolException notFound(String name) {
        return new ToolException(
                TOOL_NOT_FOUND,
                "Tool \"%s\" not found!".formatted(name)
        );
    }

    public static ToolException marshalFailed(String name, Throwable cause) {
        return new ToolException(
                TOOL_MARSHAL_FAILED,
                "Tool \"%s\" marshal failed!".formatted(name),
                cause
        );
    }

    public static ToolException unmarshalFailed(String name, Throwable cause) {
        return new ToolException(
                TOOL_UNMARSHAL_FAILED,
                "Tool \"%s\" unmarshal failed!".formatted(name),
                cause
        );
    }

    public static ToolException callFailed(String name, Throwable cause) {
        return new ToolException(
                TOOL_CALL_FAILED,
                "Tool \"%s\" call failed!".formatted(name),
                cause
        );
    }

}
