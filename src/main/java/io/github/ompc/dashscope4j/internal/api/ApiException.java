package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.Ret;

public class ApiException extends RuntimeException {

    private final int status;
    private final Ret ret;

    public ApiException(int status, Ret ret) {
        super("api ret error! status=%s;uuid=%s;code=%s;message=%s;".formatted(
                status,
                ret.uuid(),
                ret.code(),
                ret.message()
        ));
        this.status = status;
        this.ret = ret;
    }

    public int status() {
        return status;
    }

    public Ret ret() {
        return ret;
    }

}
