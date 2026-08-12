package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.InteractionHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;


/**
 * Agent 基础实现类
 * <p>
 * 提供 Agent 的核心功能实现，包括：
 * <ul>
 *     <li>会话管理：支持通过 sessionId 区分不同会话</li>
 *     <li>记忆管理：自动集成 MemoryInterceptor 实现对话历史管理</li>
 *     <li>工具搜索：内置 search_tools 工具，用于动态查找可用工具</li>
 *     <li>请求构建：统一处理消息、参数和拦截器的组装</li>
 * </ul>
 * </p>
 * <p>
 * 来扩展或修改基础的异步和流式请求处理逻辑。
 * </p>
 *
 * @see Agent
 */
public abstract class BaseAgent implements Agent {

    private final String name;
    private final String description;
    private final DashscopeClient client;
    private final ChatModel model;
    private final List<Tool> tools;
    private final List<Hook> hooks;

    /**
     * 构造 BaseAgent
     *
     * @param builder 构建器
     */
    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.client = builder.client;
        this.model = builder.model;
        this.tools = CommonUtils.unmodifiableCopy(builder.tools);
        this.hooks = CommonUtils.unmodifiableCopy(builder.hooks);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public DashscopeClient client() {
        return client;
    }

    /**
     * 构建 AIGC 请求
     * <p>
     * 组装完整的请求对象，包括：
     * <ul>
     *     <li>系统提示词（如果设置了 introduction）</li>
     *     <li>用户输入消息</li>
     *     <li>搜索工具（search_tools）</li>
     *     <li>会话拦截器（InjectInterceptor）</li>
     * </ul>
     * </p>
     *
     * @param sessionId 会话ID，用于区分不同的对话会话
     * @param inbound   用户输入消息
     * @return 构建好的 AIGC 请求
     */
    private AigcRequest<Input, Output> newRequest(String sessionId, UserMessage inbound) {
        return AigcRequest.newBuilder(model())

                // 组装对话输入
                .input(Input.newBuilder()

                        // 工具调用不失败
                        .failOnToolError(false)

                        // 用户输入
                        .addMessage(inbound)

                        // 添加工具
                        .addTools(tools())

                        // 构建
                        .build())

                // 组装拦截器（每次交互）
                .interceptors(interactionInterceptors())

                // 注入会话ID
                .context(Map.of("SESSION-ID", sessionId))

                // 构建请求
                .build();
    }


    /**
     * 获取智能体可以使用的工具列表。
     * 设计为{@code protected}的原因是，这是Agent实现可以增加工具的便捷入口
     *
     * @return 工具列表
     */
    protected List<Tool> tools() {
        return tools;
    }

    /**
     * 获取智能体可以使用的钩子列表
     * 设计为{@code protected}的原因是，这是Agent实现可以增加钩子的便捷入口
     *
     * @return 钩子列表
     */
    protected List<Hook> hooks() {
        return hooks;
    }

    @Override
    public CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound) {
        return CompletableFuture.completedStage(newRequest(sessionId, inbound))
                .thenCompose(request -> client.async(request, preparationInterceptors()))
                .thenApply(response -> response.output().best().message());
    }


    @Override
    public Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound) {

        // 组装对话请求
        final var request = AigcRequest.newBuilder(newRequest(sessionId, inbound))
                .parameters(parameters -> {

                    // 如果没有制定输出模式，默认为增量输出
                    parameters.putIfAbsent("incremental_output", true);

                    return parameters;
                })
                .build();

        return Flux.from(client.flow(request, preparationInterceptors()))
                .map(response -> response.output().best().message());
    }

    private List<Interceptor> interactionInterceptors() {
        return hooks().stream()
                .map(hook -> hook instanceof InteractionHook ih ? ih.onInteraction(this) : List.of())
                .flatMap(List::stream)
                .map(Interceptor.class::cast)
                .toList();
    }

    private List<Interceptor> preparationInterceptors() {
        return hooks().stream()
                .map(hook -> hook instanceof PreparationHook ph ? ph.onPreparation(this) : List.of())
                .flatMap(List::stream)
                .map(Interceptor.class::cast)
                .toList();
    }

    /**
     * 获取对话模型
     *
     * @return 对话模型实例
     */
    protected ChatModel model() {
        return model;
    }

    /**
     * BaseAgent 构建器
     * <p>
     * 使用 Builder 模式构建 BaseAgent 实例，支持链式调用。
     * </p>
     *
     * @param <T> Agent 类型
     * @param <B> Builder 类型
     */
    public static abstract class Builder<T extends BaseAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String description;

        private DashscopeClient client;
        private ChatModel model = ChatModel.QWEN_FLASH;

        private List<Hook> hooks;
        private List<Tool> tools;


        protected Builder() {

        }

        protected Builder(BaseAgent agent) {

            this.name = agent.name;
            this.description = agent.description;

            this.client = agent.client;
            this.model = agent.model;
            this.hooks = agent.hooks;
            this.tools = agent.tools;

        }

        /**
         * 设置智能体名称
         *
         * @param name 智能体名称
         * @return 构建器
         */
        public B name(String name) {
            this.name = name;
            return self();
        }

        /**
         * 设置智能体描述
         *
         * @param description 智能体描述
         * @return 构建器
         */
        public B description(String description) {
            this.description = description;
            return self();
        }

        /**
         * 设置 Dashscope 客户端
         *
         * @param client Dashscope 客户端
         * @return 构建器
         */
        public B client(DashscopeClient client) {
            this.client = client;
            return self();
        }

        /**
         * 设置对话模型
         *
         * @param model 对话模型
         * @return 构建器
         */
        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        /**
         * 设置钩子列表
         *
         * @param hooks 钩子列表
         * @return 构建器
         */
        public B hooks(List<Hook> hooks) {
            this.hooks = hooks;
            return self();
        }

        /**
         * 修改钩子列表
         *
         * @param operator 修改操作
         * @return 构建器
         */
        public B hooks(UnaryOperator<List<Hook>> operator) {
            this.hooks = operator.apply(mutableCopy(this.hooks));
            return self();
        }

        /**
         * 添加钩子
         *
         * @param hook 钩子
         * @return 构建器
         */
        public B addHook(Hook hook) {
            return hooks(list -> {
                list.add(hook);
                return list;
            });
        }

        /**
         * 添加钩子列表
         *
         * @param it 钩子列表
         * @return 构建器
         */
        public B addHooks(Iterable<? extends Hook> it) {
            return hooks(list -> {
                it.forEach(list::add);
                return list;
            });
        }

        /**
         * 设置工具列表
         *
         * @param tools 工具列表
         * @return 构建器
         */
        public B tools(List<Tool> tools) {
            this.tools = tools;
            return self();
        }

        /**
         * 修改工具列表
         *
         * @param operator 修改操作
         * @return 构建器
         */
        public B tools(UnaryOperator<List<Tool>> operator) {
            this.tools = operator.apply(mutableCopy(this.tools));
            return self();
        }

        /**
         * 添加工具
         *
         * @param tool 工具
         * @return 构建器
         */
        public B addTool(Tool tool) {
            return tools(list -> {
                list.add(tool);
                return list;
            });
        }

        /**
         * 添加工具列表
         *
         * @param it 工具列表
         * @return 构建器
         */
        public B addTools(Iterable<? extends Tool> it) {
            return tools(list -> {
                it.forEach(list::add);
                return list;
            });
        }

    }

}
