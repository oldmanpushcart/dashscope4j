package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import java.util.Optional;

public class ToolExecutionException extends RuntimeException {

    private static final int MAX_CAUSE_MESSAGE_LENGTH = 250;

    public static final String TOOL_NOT_FOUND = "TOOL-NOT-FOUND";
    public static final String TOOL_MARSHAL_FAILED = "TOOL-MARSHAL-FAILED";
    public static final String TOOL_UNMARSHAL_FAILED = "TOOL-UNMARSHAL-FAILED";
    public static final String TOOL_CALL_FAILED = "TOOL-CALL-FAILED";
    public static final String TOOL_INTERNAL_ERROR = "TOOL-INTERNAL-ERROR";

    private final String name;
    private final String code;
    private final String suggestion;

    public ToolExecutionException(String name, String code, String message, String suggestion) {
        super(message);
        this.name = name;
        this.code = code;
        this.suggestion = suggestion;
    }

    public ToolExecutionException(String name, String code, String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.name = name;
        this.code = code;
        this.suggestion = suggestion;
    }

    public String getCode() {
        return code;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public static ToolExecutionException notFound(String name) {
        return new ToolExecutionException(
                name,
                TOOL_NOT_FOUND,
                "Tool not found: %s".formatted(name),
                """
                        Check the list of available tools and ensure the name matches exactly.
                        Do not hallucinate tool names.
                        """
        );
    }

    private static String parseCause(Throwable cause) {
        return Optional.ofNullable(cause.getMessage())
                .filter(msg -> !msg.isBlank())
                .map(msg -> msg.length() > MAX_CAUSE_MESSAGE_LENGTH ? msg.substring(0, MAX_CAUSE_MESSAGE_LENGTH) + "..." : msg)
                .orElseGet(() -> cause.getClass().getSimpleName());
    }

    public static ToolExecutionException marshalFailed(String name, Throwable cause) {
        return new ToolExecutionException(
                name,
                TOOL_MARSHAL_FAILED,
                "Failed to marshal input for Tool: %s, reason: %s".formatted(name, parseCause(cause)),
                """
                        Verify that the arguments match the tool's expected schema strictly.
                        Check for type mismatches or missing required fields.
                        """,
                cause
        );
    }

    public static ToolExecutionException unmarshalFailed(String name, Throwable cause) {
        return new ToolExecutionException(
                name,
                TOOL_UNMARSHAL_FAILED,
                "Failed to unmarshal output for Tool: %s, reason: %s".formatted(name, parseCause(cause)),
                """
                        The tool returned malformed data.
                        Ask the user to verify the tool status or try a different tool if this persists.
                        """,
                cause
        );
    }

    public static ToolExecutionException callFailed(String name, Throwable cause) {
        return new ToolExecutionException(
                name,
                TOOL_CALL_FAILED,
                "Failed to call Tool: %s, reason: %s".formatted(name, parseCause(cause)),
                """
                        Transient failure detected.
                        Retry the tool call once.
                        If it fails again, inform the user and suggest an alternative approach.
                        """,
                cause
        );
    }

    public static ToolExecutionException callFailed(String name, String message, String suggestion, Throwable cause) {
        return new ToolExecutionException(
                name,
                TOOL_CALL_FAILED,
                "Failed to call Tool: %s, reason: %s".formatted(name, message),
                suggestion,
                cause
        );
    }

    public static ToolExecutionException callFailed(String name, String message, String suggestion) {
        return new ToolExecutionException(
                name,
                TOOL_CALL_FAILED,
                "Failed to call Tool: %s, reason: %s".formatted(name, message),
                suggestion
        );
    }



    public static ToolExecutionException wrap(String name, Throwable cause) {
        if (cause instanceof ToolExecutionException teCause) {
            return teCause;
        }
        return new ToolExecutionException(
                name,
                TOOL_INTERNAL_ERROR,
                "Internal error: %s".formatted(parseCause(cause)),
                """
                        An unexpected internal error occurred.
                        Please retry the operation.
                        If the problem persists, check system logs or contact support.
                        """,
                cause
        );
    }

}
