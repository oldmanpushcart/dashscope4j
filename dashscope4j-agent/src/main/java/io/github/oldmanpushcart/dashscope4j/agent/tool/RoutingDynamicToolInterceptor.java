package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RoutingDynamicToolInterceptor implements ChatInterceptor {

    private final ToolRegistry registry;

    public RoutingDynamicToolInterceptor(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return CompletableFuture.completedStage(request)
                .thenApply(this::injectPrompt)
                .thenCompose(chain::proceed);
    }

    private AigcRequest<Input, Output> injectPrompt(AigcRequest<Input, Output> request) {
        return AigcRequest.newBuilder(request)
                .input(input -> {
                    return Input.newBuilder(input)
                            .messages(messages -> {
                                final var content = TextContent.newBuilder()
                                        .cacheControl(Content.CacheControl.EPHEMERAL)
                                        .text("""
                                                # Role
                                                  你是一个拥有动态工具加载能力的超级智能体。
                                    
                                                  # Constraints
                                                  1. 严禁在任务未全部完成前直接回复用户。
                                                  2. 严禁假设任何工具的存在。所有工具必须通过 `search_tools` 确认。
                                                  3. 如果当前可用工具无法完成所有子任务，必须再次调用 `search_tools`。
                                    
                                                  # Workflow (必须严格遵守)
                                                  Step 1: 【思考】分析用户请求，拆解为子任务列表 (TODO List)。
                                                  Step 2: 【检查】遍历 TODO List，检查每个任务是否有对应的已加载工具。
                                                  Step 3: 【行动】
                                                     - 如果有任务缺失工具 -> **立即** 调用 `search_tools` (即使刚才已经调用过)。
                                                     - 如果所有任务都有工具 -> 执行具体的业务工具。
                                                     - 如果 `search_tools` 返回空 -> 记录该任务失败，继续处理其他任务。
                                                  Step 4: 【循环】重复 Step 1-3，直到 TODO List 全部完成或确认无法完成。
                                    
                                                  # Example
                                                  User: 查天气并画画
                                                  Assistant (Thought): 任务1:查天气(无工具), 任务2:画画(无工具). 需搜索天气工具.
                                                  Assistant (Action): search_tools("天气")
                                                  ... (得到天气工具) ...
                                                  Assistant (Thought): 任务1:查天气(有工具), 任务2:画画(无工具). 需搜索画画工具.
                                                  Assistant (Action): search_tools("绘画")\s
                                                """)
                                        .build();
                                final var message = SystemMessage.newBuilder()
                                        .contents(List.of(content))
                                        .build();
                                messages.add(0, message);
                                return messages;
                            })
                            .build();
                })
                .parameters(parameters -> {

                    //noinspection unchecked
                    final var existed = (List<Tool>) parameters.getOrDefault("tools", List.of());
                    final var newTools = new ArrayList<>(existed);
                    newTools.add(new SearchToolFunctionTool(registry).toTool());
                    parameters.put("tools", newTools);

                    return parameters;
                })
                .interceptors(interceptors -> {
                    interceptors.add(new LoadingDynamicToolInterceptor());
                    return interceptors;
                })
                .build();
    }

}
