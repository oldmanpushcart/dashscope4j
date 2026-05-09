package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 动态工具注入拦截器
 */
public class DynamicToolInjectInterceptor implements ChatInterceptor {

    private final Toolbox toolbox;

    public DynamicToolInjectInterceptor(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        final var newRequest = cleanUpRequest(request);

        return null;
    }

    private AigcRequest<Input, Output> cleanUpRequest(AigcRequest<Input, Output> request) {

        final var lastMessage = request.input().lastMessage();

        //noinspection unchecked
        final var tools = (List<Tool>) request.parameters().get("tools");
        if (null == tools || tools.isEmpty()) {
            return request;
        }

        final var newTools = tools.stream()
                .filter(tool -> {
                    if (tool instanceof ReferenceTool referenceTool) {

                        if (lastMessage instanceof ToolMessage toolMessage) {

                        }

                        return referenceTool.referenceCnt().get() > 0;
                    } else {
                        return true;
                    }
                })
                .toList();


        request.parameters().put("tools", newTools);
        return request;
    }

    private boolean isNeedInject(List<Tool> tools, FunctionTool.Call call) {
        return tools.stream()
                .anyMatch(tool -> Objects.equals(tool.meta().name(), call.stub().name()));
    }

    private CompletionStage<AigcResponse<Output>> processAsync(Chain chain, AigcRequest<Input, Output> request) {
        chain.proceed(request)
                .thenApply(r -> {
                    //noinspection unchecked
                    return (AigcResponse<Output>) r;
                })
                .thenApply(response -> {

                    //noinspection unchecked
                    final var tools = (List<Tool>) request.parameters().getOrDefault("tools", List.of());
                    final var message = response.output().best().message();
                    if (message.isToolCall()) {

                        final var newTools = new ArrayList<Tool>(tools);
                        message.calls().forEach(call -> {

                            if (!(call instanceof FunctionTool.Call functionCall)) {
                                return;
                            }

                            if (!isNeedInject(tools, functionCall)) {
                                return;
                            }

                            toolbox.lookupByName(functionCall.stub().name())
                                    .ifPresent(use -> {
                                        final var referenceTool = new ReferenceTool(use.tool());
                                        referenceTool.referenceCnt().incrementAndGet();
                                        newTools.add(referenceTool);
                                    });

                        });

                        request.parameters().put("tools", newTools);

                    }

                    return response;
                })
        ;
    }

    private static class ReferenceTool implements Tool {

        private final Tool target;
        private final AtomicInteger referenceCnt = new AtomicInteger();

        private ReferenceTool(Tool target) {
            this.target = target;
        }

        @Override
        public Meta meta() {
            return target.meta();
        }

        @Override
        public Classify classify() {
            return target.classify();
        }

        @Override
        public CompletionStage<String> call(Caller caller, String argumentJson) {
            return target.call(caller, argumentJson);
        }

        public AtomicInteger referenceCnt() {
            return referenceCnt;
        }

    }

    private static class CallTable {

        private final Toolbox toolbox;
        private final List<Entry> entries = new ArrayList<>();

        private CallTable(Toolbox toolbox) {
            this.toolbox = toolbox;
        }

        public Set<String> nameSet() {
            return entries.stream()
                    .map(Entry::name)
                    .collect(Collectors.toSet());
        }

        public void updateTools(List<Tool> tools, List<Tool.Call> calls) {
            calls.forEach(call -> {

                if (!(call instanceof FunctionTool.Call functionCall)) {
                    return;
                }

                final var isExist = tools.stream()
                        .anyMatch(tool -> Objects.equals(tool.meta().name(), functionCall.stub().name()));
                if (isExist) {
                    return;
                }

                final var tool = toolbox.lookupByName(functionCall.stub().name())
                        .map(ToolUse::tool)
                        .orElse(null);

                if (null == tool) {
                    return;
                }


            });
        }


        private record Entry(String callId, String name) {

        }

    }

}
