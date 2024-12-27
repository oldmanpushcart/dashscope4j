package io.github.oldmanpushcart.dashscope4j.api;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * API异常
 */
@Getter
@Accessors(fluent = true)
public class ApiException extends RuntimeException {

    private final int status;
    private final String uuid;
    private final String code;
    private final String desc;

    /**
     * 构建API异常
     *
     * @param status HTTP状态码
     * @param uuid   唯一编号
     * @param code   应答编码
     * @param desc   应答消息
     */
    public ApiException(int status, String uuid, String code, String desc) {
        super(String.format("api response failed! status=%s;uuid=%s;code=%s;desc=%s", status, uuid, code, desc));
        this.status = status;
        this.uuid = uuid;
        this.code = code;
        this.desc = desc;
    }

    /**
     * 构建API异常
     * @param status HTTP状态码
     * @param response 应答
     */
    public ApiException(int status, ApiResponse<?> response) {
        this(status, response.uuid(), response.code(), response.desc());
    }

}
