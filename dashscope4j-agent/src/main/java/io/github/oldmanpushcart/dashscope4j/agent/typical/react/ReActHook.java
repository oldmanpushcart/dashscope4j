package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

/**
 * ReAct钩子
 */
class ReActHook implements PreparationHook {

    private final ChatInterceptor loopInterceptor = new LoopInterceptor();
    private final ChatInterceptor settingInterceptor = new SettingInterceptor();

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(
                settingInterceptor,
                loopInterceptor
        );
    }

}
