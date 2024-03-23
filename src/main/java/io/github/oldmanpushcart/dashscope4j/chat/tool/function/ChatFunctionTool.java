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

    /**
     * @return 函数元数据
     * @since 1.2.2
     */
    Meta meta();

    /**
     * @return 对话函数
     * @since 1.2.2
     */
    ChatFunction<?, ?> function();

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

        /**
         * @return 函数名
         */
        String name();

        /**
         * @return 函数描述
         */
        String description();

        /**
         * @return 参数类型结构
         */
        TypeSchema parameterTs();

        /**
         * 类型结构(json-schema)
         *
         * @since 1.2.2
         */
        interface TypeSchema {

            /**
             * @return 类型
             */
            Type type();

            /**
             * @return json-schema
             */
            String schema();

        }

    }

    /**
     * 对话函数工具构建器
     *
     * @since 1.2.2
     */
    interface Builder extends Buildable<ChatFunctionTool, Builder> {

        /**
         * 设置函数名称
         *
         * @param name 函数名称
         * @return this
         */
        Builder name(String name);

        /**
         * 设置函数描述
         *
         * @param description 函数描述
         * @return this
         */
        Builder description(String description);

        /**
         * 设置函数参数类型
         *
         * @param parameterType 参数类型
         * @return this
         */
        Builder parameterType(Type parameterType);

        /**
         * 设置函数参数类型
         *
         * @param parameterType 参数类型
         * @param schema        参数类型结构(json-schema)
         * @return this
         */
        Builder parameterType(Type parameterType, String schema);

        /**
         * 设置对话函数
         *
         * @param function 对话函数
         * @param <T>      参数类型
         * @param <R>      返回值类型
         * @return this
         */
        <T, R> Builder chatFunction(ChatFunction<T, R> function);

        /**
         * 设置函数
         *
         * @param function 函数
         * @param <T>      参数类型
         * @param <R>      返回值类型
         * @return this
         */
        <T, R> Builder function(Function<T, R> function);

    }


}
