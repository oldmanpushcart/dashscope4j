package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.Ret;
import lombok.ToString;

public abstract class Response extends Ret {

    @ToString.Exclude
    private Object context;

    protected Response(String code, String desc) {
        super(code, desc);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Response> T fill(Request request) {
        this.context = request.context();
        return (T) this;
    }

    /**
     * 获取上下文
     * <p>
     * 上下文由于构造请求时，通过{@link Request.Builder#context(Object)}传入
     * </p>
     *
     * @param <C> 上下文类型
     * @return 上下文
     * @since 3.1.0
     */
    @SuppressWarnings("unchecked")
    public <C> C context() {
        return (C) context;
    }

}
