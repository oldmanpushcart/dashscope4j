package io.github.oldmanpushcart.dashscope4j.client.internal.api;

import io.github.oldmanpushcart.dashscope4j.client.Ret;
import lombok.ToString;

public abstract class Response extends Ret {

    @ToString.Exclude
    private final Request request;

    protected Response(Request request, String code, String desc) {
        super(code, desc);
        this.request = request;
    }

    /**
     * @return 获取请求
     * @since 3.1.0
     */
    public Request request() {
        return request;
    }

}
