package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.component.Component;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.reactivex.rxjava3.core.Flowable;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.isBlankString;
import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.isNotBlankString;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

/**
 * 抽象的智能体实现
 */
@Slf4j
@Accessors(fluent = true)
public abstract class BaseChatAgent implements ChatAgent {

    private static final AtomicInteger identityGen = new AtomicInteger(100);

    @Getter
    private final String name;

    @Getter
    private final String description;

    @Getter
    private final DashscopeClient client;

    @Getter(PROTECTED)
    private final String prompt;

    @Getter(PROTECTED)
    private final ChatModel model;

    @Getter(PROTECTED)
    private final List<Interceptor> interceptors;

    @Getter(PROTECTED)
    private final List<FunctionTool> functionTools;

    private final boolean flowBridge;
    private final ChatOp chatOp;
    private final String _toString;

    protected BaseChatAgent(Builder<?, ?> builder) {

        requireNonNull(builder.client, "client is required!");

        /*
         * 创建新的对话操作，在这个新的对话操作中
         * 1. 对话原有的async/flow将会被baseAsync/baseFlow所取代
         * 2. 代理智能体的Component
         */
        this.chatOp = BaseChatOp.of(this, builder.components);

        /*
         * 智能体的名称
         *
         * 如果没有设置则采用默认命名格式
         * 如果有指定则用指定的
         */
        this.name = isBlankString(builder.name)
                ? "%s-%s".formatted(getClass().getSimpleName(), identityGen.incrementAndGet())
                : builder.name;

        this.description = builder.description;
        this.prompt = builder.prompt;
        this.client = builder.client;
        this.model = builder.model;
        this.flowBridge = builder.flowBridge;
        this.interceptors = unmodifiableList(builder.interceptors);
        this.functionTools = unmodifiableList(builder.functionTools);
        this._toString = "dashscope-agent://%s".formatted(name);

    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedFuture(request)

                // 创建新的对话请求
                .thenApply(this::newChatRequest)

                // 根据流控开关选择不同的执行方式
                .thenCompose(newRequest -> flowBridge
                        ? bridgeFlow(newRequest)
                        : chatOp.async(newRequest))

                // 记录日志
                .whenComplete((r, ex) -> log.debug("{}/async completed.", this, ex));
    }

    // 流式桥接异步
    private CompletionStage<ChatResponse> bridgeFlow(ChatRequest request) {

        // 强制开启增量输出模式
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        // 将增量流式输出的ChatResponse合并为一个ChatResponse，并返回
        return chatOp.directFlow(newRequest)
                .reduce(ChatResponse::accumulate)
                .toCompletionStage();

    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::newChatRequest)
                .thenCompose(chatOp::flow)
                .whenComplete((r, ex) -> log.debug("{}/flow completed.", this, ex));
    }

    /*
     * 重写对话请求
     * 1. 如果智能体设置了对话模型，则将只使用智能体指定的对话模型
     * 2. 如果智能体设置了拦截器，则将智能体拦截器添加到本次对话中
     * 3. 取消传入对话请求的所有工具，只能使用智能体提供的工具
     * 4. 将智能体的提示词替换原有的System消息
     */
    private ChatRequest newChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)

                // 设置对话模型
                .building(builder -> {
                    final var model = model();
                    if (nonNull(model)) {
                        builder.model(model);
                    }
                })

                // 设置提示词
                .building(builder -> {
                    final var prompt = prompt();
                    if (isNotBlankString(prompt)) {
                        builder.self()
                                .messages(emptyList())
                                .addMessage(Message.ofSystem(prompt))
                                .addMessages(request.messages());
                    }
                })

                /*
                 * 设置智能体拦截器、工具生效边界
                 * 1. 阻断上游拦截器，仅生效全局+智能体声明的拦截器
                 * 2. 阻断工具，仅生效全局+智能体声明的工具
                 */
                .building(builder -> {

                    // 寻找或初始化基础对话上下文
                    final var context = Optional
                            .ofNullable(request.context(BaseChatContext.class))
                            .orElseGet(() -> new BaseChatContext().originalRequest(request));

                    /*
                     * 设置函数工具、拦截器
                     * 将请求和智能体的函数工具、拦截器进行合并
                     */
                    builder.self()
                            .context(BaseChatContext.class, context)
                            .interceptors(emptyList())
                            .addInterceptors(context.originalRequest().interceptors())
                            .addInterceptors(interceptors())
                            .tools(emptyList())
                            .addTools(context.originalRequest().tools())
                            .addTools(functionTools());

                })

                /*
                 * 将消息重写为用户的输入
                 *
                 * 这里之所以需要这样做，主要是消息的多媒体部分是藏在 Message#contents() 中的，
                 * 这种情况下并不利于基于文本构建的智能体进行处理，比如ReAct。
                 *
                 * 所以这里得想办法将消息格式转变为文本的信息，以便于智能体后续的处理
                 */
                .building(builder -> {

                    final Message message = request.requireLastMessageFromUser();
                    final String prompt = PromptTemplate.newBuilder()
                            .template("""
                                    用户问题
                                    --------------------
                                    ${question}
                                    --------------------
                                    
                                    请注意，在分析和回答上述问题时，您可以使用以下资源。当需要引用或调用这些资源时，请务必使用我直接提供的链接，不要自行修改或构造链接。
                                    
                                    可用资源
                                    --------------------
                                    ${resources}
                                    --------------------
                                    """
                            )
                            .variable("question", message::text)
                            .variable("resources", message.mediaContents()
                                    .stream()
                                    .map(content -> "- **%s**: %s".formatted(content.type(), content.data()))
                                    .collect(Collectors.joining("\n")))
                            .build()
                            .render();

                    /*
                     * 重组对话请求消息
                     * 将重写的消息替换最后一个用户消息
                     */
                    builder.self()
                            .messages(request.historyMessages())
                            .addMessage(Message.ofUser(prompt));

                })

                // 构造对话请求
                .build();
    }

    /**
     * 异步对话
     *
     * @param request 对话请求
     * @return 对话结果
     */
    abstract protected CompletionStage<ChatResponse> baseAsync(ChatRequest request);

    /**
     * 流式对话
     *
     * @param request 对话请求
     * @return 对话结果
     */
    abstract protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request);

    @Override
    public FunctionToolBuilder newFunctionToolBuilder() {
        return new BaseChatAgentFunctionToolBuilder(this);
    }


    /**
     * 抽象智能体功能工具构建器
     *
     * @param <T> 智能体类型
     * @param <B> 构造器类型
     */
    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String description;
        private String prompt;
        private DashscopeClient client;
        private ChatModel model;
        private boolean flowBridge = true;
        private final List<Component> components = new ArrayList<>();
        private final List<Interceptor> interceptors = new ArrayList<>();
        private final List<FunctionTool> functionTools = new ArrayList<>();

        /**
         * 设置智能体名称
         *
         * @param name 智能体名称
         * @return this
         */
        public B name(String name) {
            this.name = name;
            return self();
        }

        /**
         * 设置智能体描述
         *
         * @param description 智能体描述
         * @return this
         */
        public B description(String description) {
            this.description = description;
            return self();
        }

        /**
         * 设置智能体提示词
         *
         * @param prompt 智能体提示词
         * @return this
         */
        public B prompt(String prompt) {
            this.prompt = prompt;
            return self();
        }

        /**
         * 设置 Dashscope4j 客户端
         *
         * @param client Dashscope4j 客户端
         * @return this
         */
        public B client(DashscopeClient client) {
            this.client = requireNonNull(client, "client is required!");
            return self();
        }

        /**
         * 设置对话模型
         *
         * @param model 对话模型
         * @return this
         */
        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        /**
         * 设置是否启用流式对话桥接
         *
         * @param enabled 是否启用流式对话桥接
         * @return this
         */
        public B flowBridge(boolean enabled) {
            this.flowBridge = enabled;
            return self();
        }

        /**
         * 设置拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B interceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加拦截器
         *
         * @param interceptor 拦截器
         * @return this
         */
        public B addInterceptor(Interceptor interceptor) {
            requireNonNull(interceptor);
            this.interceptors.add(interceptor);
            return self();
        }

        /**
         * 添加拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B addInterceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加函数工具
         *
         * @param functionTool 函数工具
         * @return this
         */
        public B addFunctionTool(FunctionTool functionTool) {
            this.functionTools.add(functionTool);
            return self();
        }

        /**
         * 批量添加函数工具
         *
         * @param functionTools 函数工具集合
         * @return this
         */
        public B addFunctionTools(Collection<? extends FunctionTool> functionTools) {
            this.functionTools.addAll(functionTools);
            return self();
        }

        /**
         * 设置函数工具列表
         *
         * @param functionTools 函数工具列表
         * @return this
         */
        public B functionTools(Collection<? extends FunctionTool> functionTools) {
            this.functionTools.clear();
            this.functionTools.addAll(functionTools);
            return self();
        }

        /**
         * 添加函数
         *
         * @param function 函数
         * @return this
         */
        public B addFunction(ChatFunction<?, ?> function) {
            return addFunctionTool(ChatFunctionTool.of(function));
        }

        /**
         * 批量添加函数
         *
         * @param functions 函数列表
         * @return this
         */
        public B addFunctions(Collection<? extends ChatFunction<?, ?>> functions) {
            final var functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .toList();
            return addFunctionTools(functionTools);
        }

        /**
         * 设置函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public B functions(Collection<? extends ChatFunction<?, ?>> functions) {
            final var functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .toList();
            return functionTools(functionTools);
        }

        /**
         * 添加组件
         *
         * @param component 组件
         * @return this
         */
        public B addComponent(Component component) {
            this.components.add(component);
            return self();
        }

        /**
         * 批量添加组件
         *
         * @param components 组件集合
         * @return this
         */
        public B addComponents(Collection<? extends Component> components) {
            this.components.addAll(components);
            return self();
        }

        /**
         * 设置组件集合
         *
         * @param components 组件集合
         * @return this
         */
        public B components(Collection<? extends Component> components) {
            this.components.clear();
            this.components.addAll(components);
            return self();
        }

    }

}
