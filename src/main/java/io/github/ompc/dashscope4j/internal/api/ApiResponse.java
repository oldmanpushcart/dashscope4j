package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;

import static io.github.ompc.dashscope4j.internal.util.CommonUtils.isNotBlankString;

/**
 * API响应
 *
 * @param <D> 数据类型
 */
public abstract class ApiResponse<D extends ApiResponse.Output> {

    private final String uuid;
    private final Ret ret;
    private final Usage usage;
    private final D output;

    /**
     * 构造API响应
     *
     * @param uuid    唯一标识
     * @param code    返回结果编码
     * @param message 返回结果信息
     * @param usage   使用情况
     * @param output  数据
     */
    protected ApiResponse(String uuid, String code, String message, Usage usage, D output) {
        this.uuid = uuid;
        this.ret = ofRet(uuid, code, message);
        this.usage = usage;
        this.output = output;
    }

    /**
     * 构造返回结果
     *
     * @param uuid    唯一标识
     * @param code    返回结果编码
     * @param message 返回结果信息
     * @return 返回结果
     */
    private static Ret ofRet(String uuid, String code, String message) {
        return new Ret(
                uuid,
                isNotBlankString(code) ? code : Ret.CODE_SUCCESS,
                message
        );
    }

    /**
     * 获取唯一标识
     *
     * @return 唯一标识
     */
    public String uuid() {
        return uuid;
    }

    /**
     * 获取返回结果
     *
     * @return 返回结果
     */
    public Ret ret() {
        return ret;
    }

    /**
     * 获取使用情况
     *
     * @return 使用情况
     */
    public Usage usage() {
        return usage;
    }

    /**
     * 获取数据
     *
     * @return 数据
     */
    public D output() {
        return output;
    }

    /**
     * 应答数据
     */
    public interface Output {
    }

}
