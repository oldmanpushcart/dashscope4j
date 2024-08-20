package io.github.oldmanpushcart.internal.dashscope4j.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.ExchangeAlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils.isNotBlank;

public class SpeechSynthesisRequestImpl
        extends ExchangeAlgoRequestImpl<SpeechSynthesisModel, SpeechSynthesisResponse>
        implements SpeechSynthesisRequest {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final String text;

    @JsonProperty("task_group")
    private final String group = "audio";

    @JsonProperty("task")
    private final String task = "tts";

    @JsonProperty("function")
    private final String fn = "SpeechSynthesizer";

    protected SpeechSynthesisRequestImpl(SpeechSynthesisModel model, Option option, Duration timeout, String text) {
        super(model, option, timeout, SpeechSynthesisResponse.class);
        this.text = text;
    }

    @Override
    public String suite() {
        return "dashscope://audio/tts";
    }

    @Override
    public String text() {
        return text;
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
    public Function<String, SpeechSynthesisResponse> newExchangeResponseDecoder(String uuid) {
        return body -> {
            logger.debug("{}/{} <= {}", protocol(), uuid, body);
            final var response = JacksonUtils.toObject(body, SpeechSynthesisResponseImpl.class);
            response.uuid(uuid);
            response.ret(Ret.ofSuccess(Ret.CODE_SUCCESS));
            return response;
        };
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{

            if (isNotBlank(text)) {
                put("text", text);
            }

        }};
    }

}
