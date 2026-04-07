package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;

import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 */
public interface ToolLoader extends AutoCloseable {

    /**
     * 初始化
     *
     * @param toolbox 工具箱
     * @return 初始化回调
     */
    CompletionStage<Void> init(Toolbox toolbox);

    /**
     * 关闭加载器
     */
    @Override
    void close();

}
