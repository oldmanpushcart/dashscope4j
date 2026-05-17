package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

class ReActPlugin implements Plugin {

    private final ChatInterceptor settingInterceptor = new SettingInterceptor();
    private final ChatInterceptor loopInterceptor = new LoopInterceptor();

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(settingInterceptor, loopInterceptor);
            case INTERACTION -> List.of();
        };
    }

}
