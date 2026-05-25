package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class DashscopePlugin implements Plugin {

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        final var settingInterceptor = new SettingInterceptor();
        
        final Extension extension = new Extension() {
            @Override
            public Plugin plugin() {
                return DashscopePlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                if (Objects.requireNonNull(phases) == Phases.PREPARATION) {
                    return List.of(settingInterceptor);
                }
                return List.of();
            }
        };
        
        return CompletableFuture.completedStage(extension);
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }

}
