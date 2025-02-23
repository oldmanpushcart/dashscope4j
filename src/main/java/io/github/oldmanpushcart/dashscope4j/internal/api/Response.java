package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.Ret;
import lombok.ToString;

import java.util.Map;

public abstract class Response extends Ret {

    @ToString.Exclude
    private Map<Class<?>, Object> contextMap;

    protected Response(String code, String desc) {
        super(code, desc);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Response> T fill(Request request) {
        this.contextMap = request.contextMap();
        return (T) this;
    }

    /**
     * 获取上下文
     * <p>
     * 上下文回跟随请求传递到对应的应答报文，可以通过{@link Request#context()}设置
     * </p>
     *
     * @param <C> 上下文类型
     * @return 上下文
     * @since 3.1.0
     */
    @SuppressWarnings("unchecked")
    public <C> C context() {
        return (C) context(Object.class);
    }

    /**
     * 获取上下文
     *
     * @param type 上下文类型
     * @param <C>  上下文类型
     * @return 上下文
     * @since 3.1.0
     */
    @SuppressWarnings("unchecked")
    public <C> C context(Class<C> type) {
        return (C) contextMap.get(type);
    }

}
