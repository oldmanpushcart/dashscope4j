package io.github.oldmanpushcart.dashscope4j.client.aigc;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;

public class AigcResponse<O> extends ApiResponse {

    private final O output;

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     */
    @JsonCreator
    protected AigcResponse(

            @JacksonInject("dashscope/request")
            AigcRequest<?,?,?> request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            O output

    ) {
        super(request, uuid, code, desc);
        this.output = output;
    }

    public O output() {
        return output;
    }

}
