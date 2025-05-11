package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * 基础智能体
 */
@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
@Slf4j
public abstract class BaseChatAgent implements ChatAgent {

    private static final AtomicInteger identityGen = new AtomicInteger(100);
    private final String name;
    private final DashscopeClient client;
    private final Memory memory;
    private final ChatModel model;
    private final boolean flowBridge;
    private final List<Interceptor> interceptors;
    private final List<ChatFunctionTool> functionTools;

    protected BaseChatAgent(Builder<?, ?> builder) {

        requireNonNull(builder.client);

        this.name = buildingName(builder.name);
        this.client = builder.client;
        this.memory = builder.memory;
        this.model = builder.model;
        this.flowBridge = builder.flowBridge;
        this.interceptors = unmodifiableList(builder.interceptors);
        this.functionTools = unmodifiableList(builder.functionTools);

    }

    private String buildingName(String name) {
        return StringUtils.isNotBlank(name)
                ? name
                : String.format("chat-agent-%s", identityGen.incrementAndGet());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ChatAgent.FunctionToolBuilder newFunctionToolBuilder() {
        return new BaseChatAgentFunctionToolBuilder(this);
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedFuture(request)

                // 创建新的对话请求
                .thenApply(this::newChatRequest)

                // 根据流控开关选择不同的执行方式
                .thenCompose(newRequest -> flowBridge
                        ? baseAsyncByFlowBridge(newRequest)
                        : baseAsync(newRequest))

                // 结果存储到记忆体
                .thenApply(response ->
                        processPersistMemoryFragmentForAsync(request, response))

                // 记录日志
                .whenComplete((r, ex) ->
                        log.debug("dashscope-agent://{}/async completed.", name(), ex))
                ;

    }

    /*
     * 流式桥接异步
     *
     * 这里强制采用增量输出模式，减少网络传输和处理开销负担
     */
    private CompletionStage<ChatResponse> baseAsyncByFlowBridge(ChatRequest request) {

        // 强制开启增量输出模式
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        // 将增量流式输出的ChatResponse合并为一个ChatResponse，并返回
        return baseFlow(newRequest)
                .thenCompose(responseFlow ->
                        responseFlow
                                .reduce(ChatResponse::accumulate)
                                .toCompletionStage());
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::newChatRequest)
                .thenCompose(this::baseFlow)
                .thenApply(responseFlow ->
                        processPersistMemoryFragmentForFlow(request, responseFlow))
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
                .addTools(functionTools)
                .building(builder -> ofNullable(model).ifPresent(builder::model))
                .building(builder -> buildingForRewriteUserMessage(builder, request))
                .building(builder -> buildingForMemoryRecall(builder, request))
                .build();
    }

    /*
     * 重写用户输入部分
     *
     * 将多模态部分作为附件形式存放，便于智能体做更好的处理
     */
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

    /*
     * 在对话列表中添加回忆部分
     * SYSTEM
     * HISTORY
     * LAST_USER_INPUT
     */
    private void buildingForMemoryRecall(ChatRequest.Builder builder, ChatRequest request) {

        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return;
        }

        final List<Message> newMessages = new ArrayList<>();

        // 先添加SYSTEM
        request.messages()
                .stream()
                .filter(message -> message.role() == Message.Role.SYSTEM)
                .forEach(newMessages::add);

        // 然后添加回忆
        memory.recall(context.sessionId(), context.olderThenFragmentId(), context.newerThenFragmentId())
                .forEach(fragment -> {
                    newMessages.add(fragment.requestMessage());
                    newMessages.add(fragment.responseMessage());
                });

        // 最后添加请求原有的对话信息
        request.messages()
                .stream()
                .filter(message -> message.role() != Message.Role.SYSTEM)
                .forEach(newMessages::add);

        // 替换原有的消息列表
        builder.messages(newMessages);

    }

    // 处理异步请求的记忆片段存储
    private ChatResponse processPersistMemoryFragmentForAsync(ChatRequest request, ChatResponse response) {

        // 如果没有记忆体则不需要处理
        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return response;
        }

        // 持久化记忆片段
        final Message requestMessage = request.requireLastMessageFromUser();
        final Message responseMessage = response.output().best().message();
        final Memory.Fragment fragment = new Memory.Fragment()
                .fragmentId(context.newerThenFragmentId())
                .sessionId(context.sessionId())
                .requestMessage(requestMessage)
                .responseMessage(responseMessage)
                .createdAt(Instant.now())
                .updatedAt(Instant.now());
        final long fragmentId = memory.persist(fragment);
        context.newerThenFragmentId(fragmentId);

        return response;
    }

    // 处理流式请求的记忆片段存储
    private Flowable<ChatResponse> processPersistMemoryFragmentForFlow(ChatRequest request, Flowable<ChatResponse> responseFlow) {

        // 如果没有记忆体则不需要处理
        final Memory.Context context = request.context(Memory.Context.class);
        if (Objects.isNull(memory)
            || Memory.Context.isInvalid(context)) {
            return responseFlow;
        }

        /*
         * 应答流式输出内容缓存
         * 所以这里需要一个字符串缓存来存储流式输出内容
         */
        final StringBuilder stringBuf = new StringBuilder();

        /*
         * 从流式回复中截留应答文本
         * 将应答文本存储到记忆体中
         */
        return responseFlow
                .doOnNext(response -> {

                    /*
                     * 如果不是增量输出，则说明是全量输出
                     * 需要每次均清空缓冲区
                     */
                    final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
                    if (!isIncrementalOutput) {
                        stringBuf.setLength(0);
                    }

                    // 将当前输出添加到输出缓存中
                    final String text = response.output().best().message().text();
                    stringBuf.append(text);

                })

                // 成功完成时触发记忆片段刷新
                .doOnComplete(() -> {

                    final Message requestMessage = request.requireLastMessageFromUser();
                    final Message responseMessage = Message.ofAi(stringBuf.toString());
                    final Memory.Fragment fragment = new Memory.Fragment()
                            .fragmentId(context.newerThenFragmentId())
                            .sessionId(context.sessionId())
                            .requestMessage(requestMessage)
                            .responseMessage(responseMessage)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now());

                    final long fragmentId = memory.persist(fragment);
                    context.newerThenFragmentId(fragmentId);

                });
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


    // ------------------------- BUILDER : BASE_CHAT_AGENT -------------------------

    /**
     * 基础智能体构造器
     *
     * @param <T> 智能体类型
     * @param <B> 构造器类型
     */
    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private DashscopeClient client;
        private Memory memory;
        private ChatModel model;
        private boolean flowBridge;
        private final List<Interceptor> interceptors = new ArrayList<>();
        private final List<ChatFunctionTool> functionTools = new ArrayList<>();

        public Builder() {

        }

        public Builder(BaseChatAgent agent) {
            this.name = agent.name;
            this.client = agent.client;
            this.memory = agent.memory;
            this.model = agent.model;
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
         * 设置记忆体
         *
         * @param memory 记忆体
         * @return this
         */
        public B memory(Memory memory) {
            this.memory = memory;
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

    }

}