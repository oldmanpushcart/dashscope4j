package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

class SettingInterceptor implements ChatInterceptor {

    private static final Message SEARCH_TOOLS_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/SEARCH_TOOLS.md")
                    .build()
                    .render())
            .withCache();

    private final List<ToolLookup> fixes;
    private final List<Toolbox> dynamics;
    private final Tool searchToolsTool;
    private final ToolLookup compositeToolLookup;

    public SettingInterceptor(List<ToolLookup> fixes, List<Toolbox> dynamics) {
        this.fixes = fixes;
        this.dynamics = dynamics;
        this.searchToolsTool = new SearchToolsFunction(dynamics).asTool();
        this.compositeToolLookup = new CompositeToolLookup(fixes, dynamics);
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)

                        // 添加静态工具搜索
                        .toolLookups(lookups -> {
                            lookups.add(compositeToolLookup);
                            return lookups;
                        })

                        // 添加动态工具搜索
                        .building(inputBuilder -> {
                            if (!dynamics.isEmpty()) {
                                inputBuilder
                                        .messages(messages -> {
                                            messages.add(0, SEARCH_TOOLS_MESSAGE);
                                            return messages;
                                        })
                                        .toolLookups(lookups -> {
                                            lookups.add(ToolLookup.single(searchToolsTool));
                                            return lookups;
                                        });
                            }
                        })

                        .build())
                .build();
        return chain.proceed(newRequest);
    }

    /**
     * 聚合工具查找
     *
     * @param fixes    固定工具集合
     * @param dynamics 动态工具集合
     */
    private record CompositeToolLookup(List<ToolLookup> fixes, List<Toolbox> dynamics) implements ToolLookup {

        @Override
        public List<Tool> lookupAll() {
            return fixes.stream()
                    .flatMap(toolbox -> toolbox.lookupAll().stream())
                    .toList();
        }

        @Override
        public Optional<Tool> lookupByName(String name) {
            return Stream.of(fixes, dynamics)
                    .flatMap(Collection::stream)
                    .flatMap(toolbox -> toolbox.lookupByName(name).stream())
                    .findFirst();
        }

    }

    /**
     * 工具搜索函数
     * <p>
     * 封装了从工具箱中根据用户意图搜索可用工具的功能。
     * 当 Agent 没有合适的工具完成任务时，可以调用此工具动态查找相关工具。
     * </p>
     * <p>
     * 该函数会被包装为一个 FunctionTool，名为 "search_tools"，
     * 供 LLM 在需要时发现和调用。
     * </p>
     *
     * @param dynamics 动态工具集
     */
    private record SearchToolsFunction(List<Toolbox> dynamics)
            implements Function<SearchToolsFunction.Search, CompletionStage<Map<String, Tool>>> {

        /**
         * 执行工具搜索
         * <p>
         * 根据用户提供的意图描述，从工具箱中查找匹配的工具。
         * </p>
         *
         * @param search 搜索参数，包含用户意图描述
         * @return 匹配的工具映射表（工具名 -> 工具实例）
         */
        @Override
        public CompletionStage<Map<String, Tool>> apply(Search search) {

            // 所有工具箱串并行查询
            final var stages = dynamics.stream()
                    .map(toolbox -> toolbox.lookupByIntent(search.intent()))
                    .toList();

            // 等待所有并行查询结果返回
            return CompletableFutureUtils.sequentialMap(stages, stage -> stage)
                    .thenApply(merges -> {
                        // 将并行查询的多个工具集合合并为一个MAP
                        return merges.stream()
                                .flatMap(Collection::stream)
                                .collect(toMap(
                                        tool -> tool.meta().name(),
                                        tool -> tool,
                                        (a, b) -> b
                                ));
                    });
        }

        /**
         * 工具搜索参数
         *
         * @param intent 用户意图描述，用于匹配相关工具
         */
        public record Search(

                @JsonPropertyDescription("意图")
                @JsonProperty(value = "intent", required = true)
                String intent

        ) {

        }

        /**
         * 转换为 FunctionTool
         * <p>
         * 将当前搜索函数包装为一个标准的 FunctionTool，
         * 使其可以被 LLM 发现和调用。
         * </p>
         *
         * @return 封装后的工具对象
         */
        public Tool asTool() {
            return FunctionTool.newBuilder()
                    .name("search_tools")
                    .description("根据意图搜索工具。当你没有工具可以完成任务时调用。")
                    .parameterType(Search.class)
                    .function(this)
                    .build();
        }

    }
}
