package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.openai;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

public class OpenAiChatResponse extends ApiResponse {

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     */
    protected OpenAiChatResponse(ApiRequest<?> request, String uuid, String code, String desc) {
        super(request, uuid, code, desc);
    }

}
