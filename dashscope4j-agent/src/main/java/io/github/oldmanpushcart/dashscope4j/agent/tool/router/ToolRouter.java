package io.github.oldmanpushcart.dashscope4j.agent.tool.router;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 工具路由器
 */
public interface ToolRouter {

    /**
     * 路由工具
     *
     * @param repository 工具仓库
     * @param intent     意图
     * @return 路由回调
     */
    CompletionStage<Map<String, Tool>> routing(Map<String, Tool> repository, String intent);

}
