package io.github.oldmanpushcart.dashscope4j.client.api;

/**
 * 应答结果
 */
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
        this.desc = null != desc ? desc : "";
    }

    /**
     * @return 应答编码
     */
    public String code() {
        return code;
    }

    /**
     * @return 应答描述
     */
    public String desc() {
        return desc;
    }

    /**
     * @return 是否成功
     */
    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }


}
