package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.ExchangeAlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

public class RecognitionRequestImpl
        extends ExchangeAlgoRequestImpl<RecognitionModel, RecognitionResponse>
        implements RecognitionRequest {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    protected RecognitionRequestImpl(RecognitionModel model, Option option, Duration timeout) {
        super(model, option, timeout, RecognitionResponse.class);
    }

    @JsonProperty("task_group")
    private final String group = "audio";

    @JsonProperty("task")
    private final String task = "asr";

    @JsonProperty("function")
    private final String fn = "recognition";

    @Override
    public String suite() {
        return "dashscope://audio/asr";
    }

    @Override
    protected Object input() {
        return new HashMap<>();
    }

    @Override
    public Function<? super ExchangeApiRequest<?>, String> newExchangeRequestEncoder(String uuid) {
        return request -> {
            final var body = JacksonUtils.toJson(request);
            logger.debug("{}/{} => {}", protocol(), uuid, body);
            return body;
        };
    }

    @Override
    public Function<String, RecognitionResponse> newExchangeResponseDecoder(String uuid) {
        return body -> {
            logger.debug("{}/{} <= {}", protocol(), uuid, body);
            final var response = JacksonUtils.toObject(body, RecognitionResponseImpl.class);
            response.uuid(uuid);
            response.ret(Ret.ofSuccess(Ret.CODE_SUCCESS));
            return response;
        };
    }

}
