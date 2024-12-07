package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 应答结果
 */
@Value
@Accessors(fluent = true)
public class Ret {

    /**
     * 空编码
     */
    public static final String EMPTY_CODE = "";

    /**
     * 成功编码
     */
    public static final String CODE_SUCCESS = "SUCCESS";

    String code;
    String message;

    /**
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Ret.CODE_SUCCESS.equals(code);
    }

    @JsonCreator
    public static Ret of(
            @JsonProperty String code,
            @JsonProperty String message
    ) {
        return new Ret(
                Objects.nonNull(code) ? code : Ret.CODE_SUCCESS,
                Objects.nonNull(message) ? message : "succeeded"
        );
    }

}
