package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对话函数描述注解
 * <p>
 * 用于告知函数的使用场景，便于LLM路由决策
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatFnDescription {

    String value();

}
