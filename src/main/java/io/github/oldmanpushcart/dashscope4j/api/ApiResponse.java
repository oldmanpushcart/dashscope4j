package io.github.oldmanpushcart.dashscope4j.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * API应答
 * <pre><code>
 *     {
 *         "request_id":"",
 *         "code":"",
 *         "message":""
 *     }
 * </code></pre>
 *
 * @param <D> 应答数据
 */
@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public abstract class ApiResponse<D> {

    /**
     * 成功应答编码
     */
    public static final String CODE_SUCCESS = "SUCCESS";

    private final String uuid;
    private final String code;
    private final String desc;

    /**
     * 构建API应答
     *
     * @param uuid 唯一编号
     * @param code 应答编码
     * @param desc 应答信息
     */
    protected ApiResponse(String uuid, String code, String desc) {
        this.uuid = uuid;
        this.code = code;
        this.desc = desc;
    }

    /**
     * @return 应答数据
     */
    abstract public D output();

    /**
     * @return 是否成功
     */
    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

}
