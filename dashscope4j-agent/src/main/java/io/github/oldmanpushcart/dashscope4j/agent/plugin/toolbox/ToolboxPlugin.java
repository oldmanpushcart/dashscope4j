package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class ToolboxPlugin implements Plugin {

    private final Toolbox toolbox;
    private final Tool searchToolsTool;
    private final ChatInterceptor injectInterceptor = new InjectInterceptor();

    private ToolboxPlugin(Builder builder) {
        Objects.requireNonNull(builder.toolbox, "toolbox must not be null!");
        this.toolbox = builder.toolbox;
        this.searchToolsTool = new SearchToolsFunction(toolbox).asTool();
    }

    @Override
    public List<ChatInterceptor> interceptors(Phases phases) {
        return switch (phases) {
            case PREPARATION -> List.of(injectInterceptor);
            case INTERACTION -> List.of();
        };
    }

    /**
     * 注入工具箱
     */
    private class InjectInterceptor implements ChatInterceptor {

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

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder implements Buildable<ToolboxPlugin, Builder> {

        private Toolbox toolbox;

        public Builder toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return this;
        }

        @Override
        public ToolboxPlugin build() {
            return new ToolboxPlugin(this);
        }

    }

}
