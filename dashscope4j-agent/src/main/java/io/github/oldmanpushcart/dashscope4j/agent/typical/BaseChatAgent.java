package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.chain.ChatChain;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;


@Accessors(fluent = true)
@Slf4j
public abstract class BaseChatAgent implements ChatAgent {

    private static final AtomicInteger identityGen = new AtomicInteger(100);

    @Getter
    private final String name;

    @Getter(AccessLevel.PROTECTED)
    private final DashscopeClient client;

    private final ChatModel model;
    private final boolean flowBridge;
    private final List<Interceptor> interceptors;

    @Getter(AccessLevel.PROTECTED)
    private final List<ChatFunctionTool> functionTools;

    private final List<ChatChain> chains;
    private final ChatOp chatOp;

    protected BaseChatAgent(Builder<?, ?> builder) {

        requireNonNull(builder.client, "client is required!");

        this.name = buildingName(builder.name);
        this.client = builder.client;
        this.model = builder.model;
        this.flowBridge = builder.flowBridge;
        this.interceptors = unmodifiableList(builder.interceptors);
        this.functionTools = unmodifiableList(builder.functionTools);
        this.chains = unmodifiableList(builder.chains);
        this.chatOp = newBaseChatOp(this, builder.chains);
    }

    private static ChatOp newBaseChatOp(BaseChatAgent agent, List<ChatChain> chains) {
        final ChatOp baseChatOp = new ChatOp() {
            @Override
            public CompletionStage<ChatResponse> async(ChatRequest request) {
                return agent.baseAsync(request);
            }

            @Override
            public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
                return agent.baseFlow(request);
            }
        };
        return ChainChatOp.group(baseChatOp, chains);
    }

    private static String buildingName(String name) {
        return StringUtils.isNotBlank(name)
                ? name
                : String.format("chat-agent-%s", identityGen.incrementAndGet());
    }


    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedFuture(request)

                // 创建新的对话请求
                .thenApply(this::newChatRequest)

                // 根据流控开关选择不同的执行方式
                .thenCompose(newRequest -> flowBridge
                        ? asyncByFlowBridge(newRequest)
                        : chatOp.async(newRequest))

                // 记录日志
                .whenComplete((r, ex) ->
                        log.debug("dashscope-agent://{}/async completed.", name(), ex))
                ;
    }

    // 流式桥接异步
    private CompletionStage<ChatResponse> asyncByFlowBridge(ChatRequest request) {

        // 强制开启增量输出模式
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        // 将增量流式输出的ChatResponse合并为一个ChatResponse，并返回
        return chatOp.flow(newRequest)
                .thenCompose(responseFlow ->
                        responseFlow
                                .reduce(ChatResponse::accumulate)
                                .toCompletionStage());
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::newChatRequest)
                .thenCompose(chatOp::flow)
                .whenComplete((r, ex) ->
                        log.debug("dashscope-agent://{}/flow completed.", name(), ex));
    }

    /**
     * 创建新的对话请求
     *
     * @param request 原始对话请求
     * @return 新的对话请求
     */
    private ChatRequest newChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .addInterceptors(interceptors)
                .tools(functionTools)
                .building(this::buildingForResetChatModel)
                .building(builder -> buildingForRewriteUserMessage(builder, request))
                .build();
    }

    // 重设对话模型
    private void buildingForResetChatModel(ChatRequest.Builder builder) {
        ofNullable(model).ifPresent(builder::model);
    }

    // 重写用户输入
    private void buildingForRewriteUserMessage(ChatRequest.Builder builder, ChatRequest request) {

        /*
         * 将消息重写为用户的输入
         *
         * 这里之所以需要这样做，主要是消息的多媒体部分是藏在 Message#contents() 中的，
         * 这种情况下并不利于基于文本构建的智能体进行处理，比如ReAct。
         *
         * 所以这里得想办法将消息格式转变为文本的信息，以便于智能体后续的处理
         */
        final Message message = request.requireLastMessageFromUser();
        final String prompt = PromptTemplate.newBuilder()
                .template("### INPUT\n" +
                          "${input}\n" +
                          "\n" +
                          "### PARTS\n" +
                          "${parts}")
                .variable("input", message::text)
                .variable("parts", message.mediaContents()
                        .stream()
                        .map(content -> String.format("- **%s**: %s", content.type(), content.data()))
                        .collect(Collectors.joining("\n")))
                .build()
                .render();

        /*
         * 重组对话请求消息
         * 将重写的消息替换最后一个用户消息
         */
        builder.self()
                .messages(emptyList())
                .addMessages(request.historyMessages())
                .addMessage(Message.ofUser(prompt));
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
    public ChatAgent.FunctionToolBuilder newFunctionToolBuilder() {
        return new BaseChatAgentFunctionToolBuilder(this);
    }


    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private DashscopeClient client;
        private ChatModel model;
        private boolean flowBridge;
        private final List<ChatChain> chains = new ArrayList<>();
        private final List<Interceptor> interceptors = new ArrayList<>();
        private final List<ChatFunctionTool> functionTools = new ArrayList<>();

        public Builder() {

        }

        public Builder(BaseChatAgent agent) {
            this.name = agent.name;
            this.client = agent.client;
            this.model = agent.model;
            this.chains.addAll(agent.chains);
            this.interceptors.addAll(agent.interceptors);
            this.functionTools.addAll(agent.functionTools);
        }

        /**
         * 设置智能体名称
         *
         * @param name 智能体名称
         * @return this
         */
        public B name(String name) {
            this.name = requireNonNull(name, "name is required!");
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
        public B addFunctionTool(ChatFunctionTool functionTool) {
            this.functionTools.add(functionTool);
            return self();
        }

        /**
         * 批量添加函数工具
         *
         * @param functionTools 函数工具集合
         * @return this
         */
        public B addFunctionTools(Collection<? extends ChatFunctionTool> functionTools) {
            this.functionTools.addAll(functionTools);
            return self();
        }

        /**
         * 设置函数工具列表
         *
         * @param functionTools 函数工具列表
         * @return this
         */
        public B functionTools(Collection<? extends ChatFunctionTool> functionTools) {
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
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return addFunctionTools(functionTools);
        }

        /**
         * 设置函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public B functions(Collection<? extends ChatFunction<?, ?>> functions) {
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return functionTools(functionTools);
        }

        public B addChain(ChatChain chain) {
            this.chains.add(chain);
            return self();
        }

        public B addChains(Collection<? extends ChatChain> chains) {
            this.chains.addAll(chains);
            return self();
        }

        public B chains(Collection<? extends ChatChain> chains) {
            this.chains.clear();
            this.chains.addAll(chains);
            return self();
        }

    }

}
