package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;

import java.time.Duration;

public abstract class AlgoRequestImpl<M extends Model>
        implements AlgoRequest<M> {

    private final M model;
    private final Option option;
    private final Duration timeout;

    protected AlgoRequestImpl(M model, Option option, Duration timeout) {
        this.model = model;
        this.option = option;
        this.timeout = timeout;
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

    @JsonProperty("model")
    @Override
    public M model() {
        return model;
    }

    @JsonProperty("parameters")
    @Override
    public Option option() {
        if (model.option() == null || model.option().isEmpty()) {
            return option;
        }
        final var merged = new Option();
        model.option().export().forEach(merged::option);
        option.export().forEach(merged::option);
        return merged;
    }

    @JsonProperty("input")
    abstract protected Object input();

}
