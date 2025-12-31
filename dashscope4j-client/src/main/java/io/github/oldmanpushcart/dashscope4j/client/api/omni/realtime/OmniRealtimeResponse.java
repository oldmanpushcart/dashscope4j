package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ExchangeResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

public class OmniRealtimeResponse extends ExchangeResponse {

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     * @param usage   使用情况
     */
    protected OmniRealtimeResponse(ApiRequest<?> request, String uuid, String code, String desc, Usage usage) {
        super(request, uuid, code, desc, usage);
    }

    @Override
    public Void output() {
        return null;
    }

}
