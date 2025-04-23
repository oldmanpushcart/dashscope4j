package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.Getter;
import lombok.experimental.Accessors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.isNotBlank;

/**
 * 聊天工具选择
 *
 * @since 3.1.3
 */
@Getter
@Accessors(fluent = true, chain = true)
public class ChatToolChoice {

    private final Type type;
    private final String functionName;

    private ChatToolChoice(Type type, String functionName) {
        this.type = type;
        this.functionName = functionName;
    }

    @JsonValue
    Object extract() {
        if (type == Type.NONE) {
            return Type.NONE;
        }
        if (type == Type.FUNCTION && isNotBlank(functionName)) {
            return new ObjectMap() {{
                put("type", Type.FUNCTION);
                put("function", new ObjectMap() {{
                    put("name", functionName);
                }});
            }};
        }
        return Type.AUTO;
    }

    /**
     * 聊天工具选择类型
     */
    public enum Type {

        @JsonProperty("auto")
        AUTO,

        @JsonProperty("none")
        NONE,

        @JsonProperty("function")
        FUNCTION

    }

    /**
     * @return 自动选择
     */
    public static ChatToolChoice ofAuto() {
        return new ChatToolChoice(Type.AUTO, null);
    }

    /**
     * @return 无工具
     */
    public static ChatToolChoice ofNone() {
        return new ChatToolChoice(Type.NONE, null);
    }

    /**
     * 指定函数
     *
     * @param functionName 函数名称
     * @return 工具选择
     */
    public static ChatToolChoice ofFunctionName(String functionName) {
        return new ChatToolChoice(Type.FUNCTION, functionName);
    }

    /**
     * 指定函数
     *
     * @param function 函数对象
     * @return 工具选择
     */
    public static ChatToolChoice ofFunction(ChatFunction<?, ?> function) {
        return ofFunctionToll(ChatFunctionTool.of(function));
    }

    /**
     * 指定函数
     *
     * @param tool 函数工具
     * @return 工具选择
     */
    public static ChatToolChoice ofFunctionToll(ChatFunctionTool tool) {
        return ofFunctionName(tool.meta().name());
    }

}
