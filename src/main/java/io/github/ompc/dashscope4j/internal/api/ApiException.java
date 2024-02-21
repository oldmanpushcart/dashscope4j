package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.Ret;

/**
 * API异常
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final Ret ret;

    /**
     * 构造API异常
     *
     * @param status HTTP状态
     * @param ret    应答结果
     */
    public ApiException(int status, Ret ret) {
        super("api response error! status=%s;uuid=%s;code=%s;message=%s;".formatted(
                status,
                ret.uuid(),
                ret.code(),
                ret.message()
        ));
        this.status = status;
        this.ret = ret;
    }

    /**
     * 获取HTTP状态
     *
     * @return HTTP状态
     */
    public int status() {
        return status;
    }

    /**
     * 获取应答结果
     *
     * @return 应答结果
     */
    public Ret ret() {
        return ret;
    }

}
