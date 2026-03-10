package io.github.oldmanpushcart.dashscope4j.agent.hook;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;

import java.util.concurrent.CompletionStage;

public interface Enhancer {

    CompletionStage<Agent> enhance(Agent agent);

}
