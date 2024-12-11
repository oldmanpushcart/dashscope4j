package io.github.oldmanpushcart.dashscope4j.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class ApiRequest<M extends Model, R extends ApiResponse<?>> {

    @JsonProperty
    private final M model;

    private final Option option;
    private final Class<R> responseType;
    private final Map<String, String> headers;

    protected ApiRequest(Class<R> responseType, Builder<M, ?, ?> builder) {
        this.responseType = responseType;
        this.model = builder.model;
        this.option = builder.option;
        this.headers = builder.headers;
    }

    @JsonProperty
    protected Object input() {
        return new HashMap<>();
    }

    @JsonProperty("parameters")
    public Option option() {
        return new Option()
                .merge(model.option())
                .merge(option)
                .unmodifiable();
    }

    public static abstract class Builder<M extends Model, T extends ApiRequest<M, ?>, B extends Builder<M, T, B>> implements Buildable<T, B> {

        private M model;
        private final Option option;
        private final Map<String, String> headers;

        protected Builder() {
            this.option = new Option();
            this.headers = new LinkedHashMap<>();
        }

        protected Builder(T request) {
            this.model = request.model();
            this.option = request.option().clone();
            this.headers = new LinkedHashMap<>(request.headers());
        }

        public B model(M model) {
            this.model = model;
            return self();
        }

        public <OT, OR> B option(Option.Opt<OT, OR> opt, OT value) {
            this.option.option(opt, value);
            return self();
        }

        public B option(String name, Object value) {
            this.option.option(name, value);
            return self();
        }

        public B header(String name, String value) {
            this.headers.put(name, value);
            return self();
        }

    }

}
