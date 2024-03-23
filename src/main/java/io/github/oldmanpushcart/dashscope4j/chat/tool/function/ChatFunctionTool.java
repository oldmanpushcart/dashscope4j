package io.github.oldmanpushcart.dashscope4j.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function.ChatFunctionToolBuilderImpl;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * 对话函数工具
 *
 * @since 1.2.0
 */
public interface ChatFunctionTool extends Tool {

    Meta meta();

    ChatFunction<?,?> function();

    static Builder newBuilder() {
        return new ChatFunctionToolBuilderImpl();
    }

    /**
     * 对话函数调用
     *
     * @since 1.2.0
     */
    interface Call extends Tool.Call {

        /**
         * @return 函数名称
         */
        String name();

        /**
         * @return 函数参数
         */
        String arguments();

    }

    /**
     * 函数元数据
     *
     * @since 1.2.2
     */
    interface Meta extends Tool.Meta {

        String name();

        String description();

        TypeSchema parameterTs();

        TypeSchema returnTs();

        interface TypeSchema {

            Type type();

            String schema();

        }

    }

    interface Builder extends Buildable<ChatFunctionTool, Builder> {

        Builder name(String name);

        Builder description(String description);

        Builder parameterType(Type parameterType);

        Builder parameterType(Type parameterType, String schema);

        Builder returnType(Type returnType);

        Builder returnType(Type returnType, String schema);

        <T, R> Builder function(ChatFunction<T, R> function);

        <T, R> Builder function(Function<T, R> function);

    }


}
