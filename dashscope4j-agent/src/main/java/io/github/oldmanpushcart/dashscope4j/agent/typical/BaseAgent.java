package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin.Phases.INTERACTION;
import static io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin.Phases.PREPARATION;
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
    private final List<Plugin> plugins;
    private final List<Tool> tools;
    private final List<Plugin.Extension> extensions = new ArrayList<>();
    private final AtomicReference<State> stateRef = new AtomicReference<>(State.PENDING);

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
        this.plugins = CommonUtils.unmodifiableCopy(builder.plugins);
        this.tools = CommonUtils.unmodifiableCopy(builder.tools);
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

    // 初始化Agent
    protected void init() {



        // 安装插件
        plugins().stream()
                .map(plugin -> plugin.install(this))
                .forEach(extensions::add);

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
                        .addTools(tools)

                        // 构建
                        .build())

                // 组装拦截器
                .interceptors(interceptors(INTERACTION))

                // 注入会话ID
                .context(Map.of("SESSION-ID", sessionId))

                // 构建请求
                .build();
    }

    private List<Interceptor> interceptors(Plugin.Phases phases) {
        return extensions.stream()
                .map(extension -> extension.interceptors(phases))
                .flatMap(List::stream)
                .map(Interceptor.class::cast)
                .toList();
    }

    protected List<Plugin> plugins() {
        return plugins;
    }

    @Override
    public CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound) {
        return CompletableFuture.completedStage(newRequest(sessionId, inbound))
                .thenCompose(request -> client.async(request, interceptors(PREPARATION)))
                .thenApply(response -> response.output().best().message());
    }


    @Override
    public Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound) {
        var request = AigcRequest.newBuilder(newRequest(sessionId, inbound))
                .parameters(parameters -> {

                    // 如果没有制定输出模式，默认为增量输出
                    parameters.putIfAbsent("incremental_output", true);

                    return parameters;
                })
                .build();
        return Flux.from(client.flow(request, interceptors(PREPARATION)))
                .map(response -> response.output().best().message());
    }

    /**
     * 获取对话模型
     *
     * @return 对话模型实例
     */
    protected ChatModel model() {
        return model;
    }

    @Override
    public void close() {
        plugins().forEach(Plugin::uninstall);
    }

    private enum State {
        PENDING,
        INITIATED,
        CLOSED
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

        private List<Plugin> plugins;
        private List<Tool> tools;


        protected Builder() {

        }

        protected Builder(BaseAgent agent) {

            this.name = agent.name;
            this.description = agent.description;

            this.client = agent.client;
            this.model = agent.model;
            this.plugins = agent.plugins;
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
         * 设置插件列表
         *
         * @param plugins 插件列表
         * @return 构建器
         */
        public B plugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return self();
        }

        /**
         * 修改插件列表
         *
         * @param operator 修改操作
         * @return 构建器
         */
        public B plugins(UnaryOperator<List<Plugin>> operator) {
            this.plugins = operator.apply(mutableCopy(this.plugins));
            return self();
        }

        /**
         * 添加插件
         *
         * @param plugin 插件
         * @return 构建器
         */
        public B addPlugin(Plugin plugin) {
            return plugins(list -> {
                list.add(plugin);
                return list;
            });
        }

        /**
         * 添加插件列表
         *
         * @param it 插件列表
         * @return 构建器
         */
        public B addPlugins(Iterable<? extends Plugin> it) {
            return plugins(list -> {
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
