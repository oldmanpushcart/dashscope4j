package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.internal.api.Response;
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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class ApiResponse<D> extends Response {

    private final String uuid;

    /**
     * 构建API应答
     *
     * @param uuid 唯一编号
     * @param code 应答编码
     * @param desc 应答信息
     */
    protected ApiResponse(String uuid, String code, String desc) {
        super(code, desc);
        this.uuid = uuid;
    }

    /**
     * @return 应答数据
     */
    abstract public D output();

}
