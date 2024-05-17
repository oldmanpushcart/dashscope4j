package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiRequestBuilderImpl;

import static java.util.Objects.requireNonNull;

public abstract class AlgoRequestBuilderImpl<M extends Model, T extends AlgoRequest<?>, B extends AlgoRequest.Builder<M, T, B>>
        extends ApiRequestBuilderImpl<T, B>
        implements AlgoRequest.Builder<M, T, B> {

    private M model;
    private final Option option = new Option();

    protected AlgoRequestBuilderImpl() {
    }

    protected AlgoRequestBuilderImpl(M model, T request) {
        super(request);
        this.model = model;
        request.option().export().forEach(this.option::option);
    }

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
