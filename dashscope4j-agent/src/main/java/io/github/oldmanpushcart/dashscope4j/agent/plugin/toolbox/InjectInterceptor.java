package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 注入工具箱拦截器
 * <p>
 * 负责在请求准备阶段将工具箱中的工具注入到请求参数中。
 * </p>
 */
class InjectInterceptor implements ChatInterceptor {

    private final Toolbox toolbox;
    private final Tool searchToolsTool;

    public InjectInterceptor(Toolbox toolbox, Tool searchToolsTool) {
        this.toolbox = toolbox;
        this.searchToolsTool = searchToolsTool;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {

                    // 来自请求的工具
                    //noinspection unchecked
                    final var requestTools = (List<Tool>) (parameters.getOrDefault("tools", List.of()));

                    // 来自工具箱的工具（FIXED模式）
                    final var toolboxTools = toolbox.lookupAll().stream()
                            .filter(use -> use.mode() == ToolUse.Mode.FIXED)
                            .map(ToolUse::tool)
                            .toList();

                    /*
                     * 合并工具集合
                     * 1. 按照工具名称进行去重，toolbox会覆盖request的同名工具
                     * 2. 添加特殊工具：search_tools
                     */
                    final var mergeMap = new HashMap<String, Tool>();
                    requestTools.forEach(tool -> mergeMap.put(tool.meta().name(), tool));
                    toolboxTools.forEach(tool -> mergeMap.putIfAbsent(tool.meta().name(), tool));
                    mergeMap.put(searchToolsTool.meta().name(), searchToolsTool);

                    // 重新注入回请求
                    parameters.put("tools", new ArrayList<>(mergeMap.values()));

                    return parameters;
                })
                .context(context -> {
                    context.put("toolbox", toolbox);
                    return context;
                })
                .build();
        return chain.proceed(newRequest);
    }

}
