package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class ReActPlugin implements Plugin {

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        final var settingInterceptor = new SettingInterceptor();
        final var loopInterceptor = new LoopInterceptor();
        
        final Extension extension = new Extension() {
            @Override
            public Plugin plugin() {
                return ReActPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(settingInterceptor, loopInterceptor);
                    case INTERACTION -> List.of();
                };
            }
        };
        
        return CompletableFuture.completedStage(extension);
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }

}
