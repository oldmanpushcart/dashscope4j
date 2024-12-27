package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.internal.util.GenericReflectUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonBlankString;
import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.toSnakeCase;

/**
 * ChatFunction助手类
 */
class ChatFunctionToolHelper {

    /**
     * parse {@link ChatFunction} to {@link ChatFunctionTool}
     *
     * @param function 对话函数实例
     * @return 对话函数工具
     */
    public static ChatFunctionTool parse(ChatFunction<?, ?> function) {

        // 获取函数类
        final Class<?> functionClass = function.getClass();

        // 找到ChatFunction接口
        final ParameterizedType interfaceType = Optional
                .ofNullable(GenericReflectUtils.findFirst(functionClass, ChatFunction.class))
                .orElseThrow(() -> new IllegalArgumentException(String.format("required implements interface: %s",
                        ChatFunction.class.getName()
                )));

        final String fnName = parseFnName(functionClass);
        final String fnDesc = parseFnDesc(functionClass);
        final Type parameterType = interfaceType.getActualTypeArguments()[0];
        final ChatFunctionTool.Meta.TypeSchema schema = new ChatFunctionTool.Meta.TypeSchema(parameterType);

        return new ChatFunctionTool(
                new ChatFunctionTool.Meta(fnName, fnDesc, schema),
                function
        );

    }

    private static String parseFnName(Class<?> functionClass) {
        final ChatFnName anChatFnName = functionClass.getAnnotation(ChatFnName.class);
        final String fnName = Objects.nonNull(anChatFnName)
                ? anChatFnName.value()
                : toSnakeCase(functionClass.getSimpleName());
        requireNonBlankString(fnName, () -> String.format("ChatFunction name is blank in class: %s", functionClass.getName()));
        return fnName;
    }

    private static String parseFnDesc(Class<?> functionClass) {
        final ChatFnDescription anChatFnDesc = functionClass.getAnnotation(ChatFnDescription.class);
        return Objects.nonNull(anChatFnDesc)
                ? anChatFnDesc.value()
                : "";
    }

}
