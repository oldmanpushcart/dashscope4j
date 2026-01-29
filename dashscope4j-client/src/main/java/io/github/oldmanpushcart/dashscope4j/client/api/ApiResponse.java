package io.github.oldmanpushcart.dashscope4j.client.api;

/**
 * 应答
 */
public abstract class ApiResponse extends Ret {

    private final ApiRequest<?> request;
    private final String uuid;

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     */
    protected ApiResponse(ApiRequest<?> request, String uuid, String code, String desc) {
        super(code, desc);
        this.request = request;
        this.uuid = uuid;
    }

    /**
     * @return 请求
     */
    public ApiRequest<?> request() {
        return request;
    }

    /**
     * @return 请求唯一标识
     */
    public String uuid() {
        return uuid;
    }

}
