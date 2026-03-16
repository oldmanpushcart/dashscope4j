package io.github.oldmanpushcart.dashscope4j.agent.tool.loader;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 */
public interface ToolLoader {

    /**
     * 初始化
     *
     * @param updater 工具更新器
     * @return 初始化完成的异步回调
     */
    CompletionStage<Void> init(Updater updater);

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
