package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.Ret;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ApiException extends RuntimeException {

    private final String uuid;
    private final Ret ret;

    public ApiException(String uuid, Ret ret) {
        super(String.format("api response failed! uuid=%s;code=%s;message=%s", uuid, ret.code(), ret.message()));
        this.uuid = uuid;
        this.ret = ret;
    }

    public ApiException(ApiResponse<?> response) {
        this(response.uuid(), response.ret());
    }

}
