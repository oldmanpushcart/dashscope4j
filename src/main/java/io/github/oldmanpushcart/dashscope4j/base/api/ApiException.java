package io.github.oldmanpushcart.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.Ret;

/**
 * API异常
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String uuid;
    private final Ret ret;

    /**
     * 构造API异常
     *
     * @param status   HTTP状态
     * @param response 应答
     */
    public ApiException(int status, ApiResponse<?> response) {
        this(status, response.uuid(), response.ret());
    }

    /**
     * 构建API异常
     *
     * @param status HTTP状态
     * @param uuid   唯一标识
     * @param ret    应答结果
     * @since 2.2.0
     */
    public ApiException(int status, String uuid, Ret ret) {
        super("api response error! status=%s;uuid=%s;code=%s;message=%s;".formatted(
                status,
                uuid,
                ret.code(),
                ret.message()
        ));
        this.status = status;
        this.uuid = uuid;
        this.ret = ret;
    }

    /**
     * @return HTTP状态
     */
    public int status() {
        return status;
    }

    /**
     * @return 唯一标识
     */
    public String uuid() {
        return uuid;
    }

    /**
     * @return 应答结果
     */
    public Ret ret() {
        return ret;
    }

}
