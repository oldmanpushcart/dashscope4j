package io.github.ompc.internal.dashscope4j.algo;

import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.algo.AlgoRequest;
import io.github.ompc.internal.dashscope4j.api.ApiRequestBuilderImpl;

import static java.util.Objects.requireNonNull;

public abstract class AlgoRequestBuilderImpl<M extends Model, T extends AlgoRequest<?>, B extends AlgoRequest.Builder<M, T, B>>
        extends ApiRequestBuilderImpl<T, B>
        implements AlgoRequest.Builder<M, T, B> {

    private M model;
    private final Option option = new Option();

    @Override
    public B model(M model) {
        this.model = requireNonNull(model);
        return self();
    }

    @Override
    public <OT, OR> B option(Option.Opt<OT, OR> opt, OT value) {
        this.option.option(opt, value);
        return self();
    }

    @Override
    public B option(String name, Object value) {
        this.option.option(name, value);
        return self();
    }

    protected M model() {
        return model;
    }

    protected Option option() {
        return option;
    }

}
