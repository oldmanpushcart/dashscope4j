package io.github.oldmanpushcart.dashscope4j.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@SuperBuilder
public abstract class ApiResponse<D> {

    public static final String CODE_SUCCESS = "SUCCESS";

    @JsonProperty("request_id")
    private final String uuid;

    @Builder.Default
    @JsonProperty
    private final String code = CODE_SUCCESS;

    @JsonProperty("message")
    private final String desc;

    @JsonProperty
    private final Usage usage;

    abstract public D output();

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

}
