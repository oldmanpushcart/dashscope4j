package io.github.oldmanpushcart.dashscope4j.api;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * API异常
 */
@Getter
@Accessors(fluent = true)
public class ApiException extends RuntimeException {

    private final String uuid;
    private final String code;
    private final String desc;

    /**
     * 构建API异常
     *
     * @param uuid 唯一编号
     * @param code 应答编码
     * @param desc 应答消息
     */
    public ApiException(String uuid, String code, String desc) {
        super(String.format("api response failed! uuid=%s;code=%s;desc=%s", uuid, code, desc));
        this.uuid = uuid;
        this.code = code;
        this.desc = desc;
    }

    /**
     * 构建API异常
     *
     * @param response 应答
     */
    public ApiException(ApiResponse<?> response) {
        this(response.uuid(), response.code(), response.desc());
    }

}
