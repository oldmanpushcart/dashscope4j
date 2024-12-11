package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.Usage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public abstract class ApiResponse<D> {

    public static final String CODE_SUCCESS = "SUCCESS";

    private final String uuid;
    private final String code;
    private final String desc;
    private final Usage usage;

    protected ApiResponse(String uuid, String code, String desc, Usage usage) {
        this.uuid = uuid;
        this.code = null != code ? code : CODE_SUCCESS;
        this.desc = desc;
        this.usage = null != usage ? usage : Usage.empty();
    }

    abstract public D output();

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

}
