package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;

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
     * @param uuid   请求标识
     * @param ret    返回结果
     * @param usage  使用情况
     * @param output 数据
     */
    protected ApiResponse(String uuid, Ret ret, Usage usage, D output) {
        this.uuid = uuid;
        this.ret = ret;
        this.usage = usage;
        this.output = output;
    }

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
