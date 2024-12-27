package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.Usage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 算法应答
 * <pre><code>
 *     {
 *         "request_id":"",
 *         "code":"",
 *         "message":"",
 *         "output":{
 *             // ...
 *         },
 *         "usage":{
 *             // ...
 *         }
 *     }
 * </code></pre>
 *
 * @param <D> 应答数据类型
 */
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class AlgoResponse<D> extends ApiResponse<D> {

    private final Usage usage;

    /**
     * 构建算法应答结果
     *
     * @param uuid  唯一编号
     * @param code  应答编码
     * @param desc  应答信息
     * @param usage 用量
     */
    protected AlgoResponse(String uuid, String code, String desc, Usage usage) {
        super(uuid, code, desc);
        this.usage = null != usage ? usage : Usage.empty();
    }

}
