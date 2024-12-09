package io.github.oldmanpushcart.dashscope4j.api;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class ApiException extends RuntimeException {

    private final String uuid;
    private final String code;
    private final String desc;

    public ApiException(String uuid, String code, String desc) {
        super(String.format("api response failed! uuid=%s;code=%s;desc=%s", uuid, code, desc));
        this.uuid = uuid;
        this.code = code;
        this.desc = desc;
    }

    public ApiException(ApiResponse<?> response) {
        this(response.uuid(), response.code(), response.desc());
    }

}
