package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;

import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 */
public interface ToolLoader extends AutoCloseable {

    /**
     * 将加载器安装到工具箱中
     * <p>
     * 一旦完成安装
     * <li>该加载器将不能再被安装。</li>
     * <li>该加载器生命周期由工具集管理。</li>
     * </p>
     *
     * @param toolbox 工具箱
     * @return 安装结果
     */
    CompletionStage<Void> install(Toolbox toolbox);

    /**
     * 关闭加载器
     */
    @Override
    void close();

}
