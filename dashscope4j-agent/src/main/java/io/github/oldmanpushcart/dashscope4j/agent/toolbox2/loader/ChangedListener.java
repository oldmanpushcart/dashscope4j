package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader;

/**
 * 变更监听器
 */
public interface ChangedListener {

    /**
     * 工具变更通知
     *
     * @param loader 发生变更的加载器
     */
    void onChanged(ToolLoader loader);

}
