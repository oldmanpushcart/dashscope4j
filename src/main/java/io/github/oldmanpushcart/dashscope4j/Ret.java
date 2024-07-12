package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils.isNotBlank;

/**
 * 应答结果
 *
 * @param code    结果编码
 * @param message 结果信息
 */
public record Ret(String code, String message) {

    /**
     * 空编码
     */
    public static final String EMPTY_CODE = "";

    /**
     * 成功编码
     */
    public static final String CODE_SUCCESS = "SUCCESS";

    /**
     * 判断是否成功
     *
     * @return TRUE | FALSE
     */
    public boolean isSuccess() {
        return Ret.CODE_SUCCESS.equals(code);
    }

    /**
     * 创建成功应答
     *
     * @param message 结果信息
     * @return 应答结果
     */
    public static Ret ofSuccess(String message) {
        return new Ret(Ret.CODE_SUCCESS, message);
    }

    @JsonCreator
    public static Ret of(

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message

    ) {
        return new Ret(
                isNotBlank(code) ? code : Ret.CODE_SUCCESS,
                isNotBlank(message) ? message : "succeeded"
        );
    }

}
