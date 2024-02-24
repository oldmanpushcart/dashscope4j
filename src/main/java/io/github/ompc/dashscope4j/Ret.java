package io.github.ompc.dashscope4j;

import static io.github.ompc.dashscope4j.internal.util.CommonUtils.isNotBlankString;

/**
 * 应答结果
 *
 * @param code    结果编码
 * @param message 结果信息
 */
public record Ret(String code, String message) {

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

    public static Ret of(String code, String message) {
        return new Ret(
                isNotBlankString(code) ? code : Ret.CODE_SUCCESS,
                isNotBlankString(code) ? message : "succeeded"
        );
    }

}
