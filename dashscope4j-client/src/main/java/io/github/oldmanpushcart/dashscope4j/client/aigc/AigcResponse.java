package io.github.oldmanpushcart.dashscope4j.client.aigc;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

public class AigcResponse<O> extends ApiResponse implements Accumulator<AigcResponse<O>> {

    private final O output;
    private final Usage usage;

    /**
     * 构造应答
     *
     * @param request 请求
     * @param uuid    请求唯一标识
     * @param code    应答编码
     * @param desc    应答描述
     */
    @JsonCreator
    public AigcResponse(

            @JacksonInject("dashscope/request")
            AigcRequest<?, ?> request,

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
        this.usage = usage;
    }

    @Override
    public AigcRequest<?, ?> request() {
        return (AigcRequest<?, ?>) super.request();
    }

    public O output() {
        return output;
    }

    public Usage usage() {
        return usage;
    }

    @Override
    public AigcResponse<O> accumulate(AigcResponse<O> next) {

        O mergeOutput;
        if (output == null) {
            mergeOutput = next.output;
        } else if (output instanceof Accumulator<?> && next.output instanceof Accumulator<?>) {
            //noinspection unchecked
            mergeOutput = ((Accumulator<O>) output).accumulate(next.output);
        } else {
            throw new UnsupportedOperationException("Output is not accumulator");
        }

        return new AigcResponse<>(
                next.request(),
                next.uuid(),
                next.code(),
                next.desc(),
                next.usage,
                mergeOutput
        );
    }

}
