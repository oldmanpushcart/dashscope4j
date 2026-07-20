package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

class SettingInterceptor implements ChatInterceptor {

    private static final Message SEARCH_TOOLS_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/SEARCH_TOOLS.md")
                    .build()
                    .render())
            .withCache();

    private final List<Toolbox> toolboxes;
    private final Tool searchToolsTool;
    private final ToolLookup dynamicToolLookup;

    public SettingInterceptor(List<Toolbox> toolboxes) {
        this.toolboxes = toolboxes;
        this.searchToolsTool = new SearchToolsFunction(toolboxes).asTool();
        this.dynamicToolLookup = new DynamicToolLookup(toolboxes);
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)

                        // 添加静态工具搜索
                        .toolLookups(lookups -> {
                            lookups.add(dynamicToolLookup);
                            return lookups;
                        })

                        // 添加动态工具搜索
                        .building(inputBuilder -> {

                            final var dynamicToolboxes = toolboxes.stream()
                                    .filter(toolbox -> toolbox.mode() == Toolbox.Mode.DYNAMIC)
                                    .toList();

                            if (!dynamicToolboxes.isEmpty()) {
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

    private record DynamicToolLookup(List<Toolbox> toolboxes) implements ToolLookup {

        @Override
            public List<Tool> lookupAll() {
                return toolboxes.stream()
                        .flatMap(toolbox -> toolbox.lookupAll().stream())
                        .toList();
            }

            @Override
            public Optional<Tool> lookupByName(String name) {
                return toolboxes.stream()
                        .filter(toolbox -> toolbox.mode() == Toolbox.Mode.DYNAMIC)
                        .flatMap(toolbox -> toolbox.lookupAll().stream())
                        .filter(tool -> Objects.equals(tool.meta().name(), name))
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
     * @param toolboxes 工具箱实例
     */
    private record SearchToolsFunction(List<Toolbox> toolboxes)
            implements Function<SearchToolsFunction.Search, CompletionStage<Map<String, Tool>>> {

        /**
         * 构造工具搜索函数
         *
         * @param toolboxes 工具箱实例
         */
        private SearchToolsFunction {
        }

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

            final var merges = new CopyOnWriteArrayList<Tool>();
            final var stages = toolboxes.stream()
                    .filter(toolbox -> toolbox.mode() == Toolbox.Mode.DYNAMIC)
                    .map(toolbox -> toolbox
                            .lookupByIntent(search.intent())
                            .thenAccept(merges::addAll))
                    .toList();

            return CompletableFutureUtils.allOf(stages)
                    .thenApply(u -> merges.stream()
                            .collect(toMap(
                                    tool -> tool.meta().name(),
                                    tool -> tool
                            )));

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
