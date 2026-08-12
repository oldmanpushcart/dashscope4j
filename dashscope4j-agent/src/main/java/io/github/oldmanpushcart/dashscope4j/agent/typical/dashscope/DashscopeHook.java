package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

class DashscopeHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor = new SettingInterceptor();

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

}
