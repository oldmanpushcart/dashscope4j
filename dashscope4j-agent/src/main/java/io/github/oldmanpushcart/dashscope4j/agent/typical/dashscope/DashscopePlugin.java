package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.Objects;

public class DashscopePlugin implements Plugin {

    private final ChatInterceptor settingInterceptor;

    public DashscopePlugin() {
        this.settingInterceptor = new SettingInterceptor();
    }

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        if (Objects.requireNonNull(phases) == Phases.PREPARATION) {
            return List.of(settingInterceptor);
        }
        return List.of();
    }

}
