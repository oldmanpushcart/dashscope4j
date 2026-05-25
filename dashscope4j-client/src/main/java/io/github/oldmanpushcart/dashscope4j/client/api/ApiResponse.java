package io.github.oldmanpushcart.dashscope4j.client.api;

import java.util.Objects;

/**
 * API 应答
 */
public class ApiResponse extends Ret {

    private final ApiRequest<?> request;
    private final String uuid;

    /**
     * 构造 API 应答
     *
     * @param request 原始请求
     * @param uuid    唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     */
    protected ApiResponse(ApiRequest<?> request, String uuid, String code, String desc) {
        super(code, desc);
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(uuid, "uuid cannot be null");
        this.request = request;
        this.uuid = uuid;
    }

    /**
     * @return 原始请求
     */
    public ApiRequest<?> request() {
        return request;
    }

    /**
     * @return 唯一标识
     */
    public String uuid() {
        return uuid;
    }

}
