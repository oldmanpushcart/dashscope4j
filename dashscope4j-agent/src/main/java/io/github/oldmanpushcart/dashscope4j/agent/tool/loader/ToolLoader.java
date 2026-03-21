package io.github.oldmanpushcart.dashscope4j.agent.tool.loader;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工具加载器
 */
public interface ToolLoader {

    /**
     * @return 加载器名称
     */
    String name();

    /**
     * 初始化加载器
     *
     * @param registrar 工具注册器
     * @return 初始化完成的异步回调
     */
    CompletionStage<Void> init(Registrar registrar);

    /**
     * 工具注册器
     */
    interface Registrar {

        /**
         * 注册、更新工具列表
         *
         * @param tools 工具列表（全量）
         */
        void register(List<Tool> tools);

    }

}
