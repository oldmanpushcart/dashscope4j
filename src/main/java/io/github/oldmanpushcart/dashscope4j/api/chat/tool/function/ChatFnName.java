package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对话函数名称注解
 * <p>
 * 用于指定对哈函数名称，LLM在实际发起调用时将采用该函数名称来和客户端交互。<br/>
 * 如果不指定则采用全限定类名代替
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatFnName {

    String value();

}
