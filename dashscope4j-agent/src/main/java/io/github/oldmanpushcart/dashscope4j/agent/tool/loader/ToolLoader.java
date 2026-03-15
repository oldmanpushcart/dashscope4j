package io.github.oldmanpushcart.dashscope4j.agent.tool.loader;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;

/**
 * 工具加载器
 */
public interface ToolLoader {

    /**
     * 初始化
     *
     * @param updater 工具更新器
     */
    void init(Updater updater);

    /**
     * 工具更新器
     */
    interface Updater {

        /**
         * 更新工具
         *
         * @param tools 工具列表
         */
        void update(List<Tool> tools);

    }

}
