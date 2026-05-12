package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Plan-Execute Plugin
 * <p>
 * 采用 Plan-Execute-Observation-Replan 模式，适用于复杂多步骤任务。
 * </p>
 */
class PlanExecutePlugin implements Plugin {
    
    private final ChatInterceptor settingInterceptor = new SettingInterceptor();
    private final ChatInterceptor loopInterceptor;
    
    /**
     * 构造 PlanExecutePlugin
     */
    PlanExecutePlugin(Supplier<Agent> subAgentSupplier, int maxReplanCount, int maxSubTasks) {
        this.loopInterceptor = new LoopInterceptor(subAgentSupplier, maxReplanCount, maxSubTasks);
    }
    
    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(settingInterceptor);
            case INTERACTION -> List.of(loopInterceptor);
        };
    }
}
