package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 应答结果
 *
 * @param code    结果编码
 * @param message 结果信息
 */
public record Ret(

        @JsonProperty("request_id")
        String uuid,

        @JsonProperty("code")
        String code,

        @JsonProperty("message")
        String message

) {

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

}
