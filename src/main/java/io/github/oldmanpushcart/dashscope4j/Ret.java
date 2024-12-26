package io.github.oldmanpushcart.dashscope4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class Ret {

    /**
     * 成功应答编码
     */
    public static final String CODE_SUCCESS = "SUCCESS";

    /**
     * 失败应答编码
     */
    public static final String CODE_FAILURE = "FAILURE";

    private final String code;
    private final String desc;

    protected Ret(String code, String desc) {
        this.code = null != code ? code : CODE_SUCCESS;
        this.desc = null != desc ? desc : this.code;
    }

    /**
     * @return 是否成功
     */
    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

}
