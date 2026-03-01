package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * 算法应答
 *
 * @param <O> 算法输出类型
 */
public class AigcResponse<O> extends ApiResponse implements Accumulator<AigcResponse<O>> {

    private final O output;
    private final Usage usage;

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
        this.usage = null != usage ? usage : Usage.empty();
    }

    @Override
    public AigcRequest<?, ?> request() {
        return (AigcRequest<?, ?>) super.request();
    }

    /**
     * @return 模型输出
     */
    public O output() {
        return output;
    }

    /**
     * @return 使用情况
     */
    public Usage usage() {
        return usage;
    }

    /**
     * 修改模型输出
     *
     * @param operator 修改操作
     * @return 修改后的应答
     */
    public AigcResponse<O> changeOutput(UnaryOperator<O> operator) {
        Objects.requireNonNull(operator, "operator cannot be null");
        return new AigcResponse<>(
                request(),
                uuid(),
                code(),
                desc(),
                usage,
                operator.apply(output)
        );
    }

    @Override
    public AigcResponse<O> accumulate(AigcResponse<O> oAigcResponse) {

        /*
         * 如果 output 也实现了 Accumulator 接口，则进行累加。
         * 否则直接用后者进行替换
         */
        //noinspection unchecked
        final var newOutput = output instanceof Accumulator<?>
                ? ((Accumulator<O>) output).accumulate(oAigcResponse.output)
                : oAigcResponse.output;

        return changeOutput(o -> newOutput);
    }

}
