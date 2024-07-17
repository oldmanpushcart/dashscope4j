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
        super("api response error! status=%s;uuid=%s;code=%s;message=%s;".formatted(
                status,
                response.uuid(),
                response.ret().code(),
                response.ret().message()
        ));
        this.status = status;
        this.uuid = response.uuid();
        this.ret = response.ret();
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
