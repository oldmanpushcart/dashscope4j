package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对话函数注解
 * <p>用于标记函数相关信息</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatFn {

    /**
     * @return 函数名称
     */
    String name();

    /**
     * @return 函数描述
     */
    String description() default "";

}
