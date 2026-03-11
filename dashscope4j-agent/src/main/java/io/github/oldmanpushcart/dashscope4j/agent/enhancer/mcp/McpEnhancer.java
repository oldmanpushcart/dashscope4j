package io.github.oldmanpushcart.dashscope4j.agent.enhancer.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.enhancer.Enhancer;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.util.concurrent.CompletionStage;

public class McpEnhancer implements Enhancer {

    private final McpClientTransport transport;

    public McpEnhancer(McpClientTransport transport) {
        this.transport = transport;
    }

    @Override
    public CompletionStage<Agent> enhance(Agent agent) {
        return null;
    }

}
