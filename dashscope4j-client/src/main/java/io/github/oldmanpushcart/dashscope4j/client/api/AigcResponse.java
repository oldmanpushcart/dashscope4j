package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

/**
 * 模型应答
 *
 * @param <O> 模型输出类型
 */
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
     * 累计合并
     * <p>
     * 算法应答累计合并，主要是用于将增量流式应答的结果进行还原。合并的原则如下：
     *     <ul>
     *         <li>{@code request}是同一个，不需要合并</li>
     *         <li>{@code uuid}、{@code code}、{@code desc}、{@code usage}采用下一个应答作为最新值</li>
     *         <li>{@code output}为应答的模型输出，里面是增量信息，需要进行合并。具体合并策略由{@code output}的实现类决定。</li>
     *     </ul>
     * </p>
     *
     * @param next 下一个应答
     * @return 累计后的应答
     */
    @Override
    public AigcResponse<O> accumulate(AigcResponse<O> next) {

        //noinspection unchecked
        final O mergeOutput = (output == null)
                ? next.output
                : ((Accumulator<O>) output).accumulate(next.output);

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
