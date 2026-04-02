package io.github.oldmanpushcart.dashscope4j.agent.tool.loader;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;

import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 */
public interface ToolLoader extends AutoCloseable {

    /**
     * 初始化
     *
     * @param registry 工具注册器
     * @return 初始化回调
     */
    CompletionStage<Void> init(ToolRegistry registry);

    /**
     * 关闭加载器
     */
    @Override
    void close();

}
