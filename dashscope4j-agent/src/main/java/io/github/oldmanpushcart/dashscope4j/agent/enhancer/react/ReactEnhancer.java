package io.github.oldmanpushcart.dashscope4j.agent.enhancer.react;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.enhancer.Enhancer;
import io.github.oldmanpushcart.dashscope4j.agent.enhancer.react.interceptor.ReactInterceptor;

import java.util.concurrent.CompletionStage;

public class ReactEnhancer implements Enhancer {

    @Override
    public CompletionStage<Agent> enhance(Agent agent) {
        return Agent.newBuilder(agent)
                .interceptors(interceptors -> {
                    interceptors.add(new ReactInterceptor());
                    return interceptors;
                })
                .buildAsync();
    }

}
