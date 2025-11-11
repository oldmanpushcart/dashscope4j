package io.github.oldmanpushcart.dashscope4j.client.api;

public abstract class AlgoResponse<D> extends ApiResponse {

    private final Usage usage;

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     * @param usage   使用情况
     */
    protected AlgoResponse(ApiRequest<?> request, String uuid, String code, String desc, Usage usage) {
        super(request, uuid, code, desc);
        this.usage = usage;
    }

    public Usage usage() {
        return usage;
    }

    /**
     * @return 应答数据
     */
    abstract public D output();

}
