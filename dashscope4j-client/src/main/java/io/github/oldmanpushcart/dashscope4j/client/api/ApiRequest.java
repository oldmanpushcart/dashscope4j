package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * API 请求
 * <p>
 * 封装了 API 请求的核心属性和行为，支持通过 Builder 模式构建和修改请求。
 * </p>
 *
 * <h3>核心概念：阻断传播 vs 共享传播</h3>
 * <p>
 * 当使用 {@code newBuilder(request)} 从现有请求创建新请求时，不同属性的传播行为不同：
 * </p>
 * <ul>
 *     <li><b>阻断传播（独立副本）</b>：{@code headers}、{@code interceptors}、{@code tags}
 *         <ul>
 *             <li>新请求会获得这些属性的<strong>独立副本</strong></li>
 *             <li>修改新请求的这些属性<strong>不会影响</strong>原请求</li>
 *             <li>适用于需要隔离配置的场景，如为特定请求添加临时拦截器或标签</li>
 *         </ul>
 *     </li>
 *     <li><b>共享传播（同一引用）</b>：{@code context}
 *         <ul>
 *             <li>新请求和原请求<strong>共享同一个 context 对象</strong></li>
 *             <li>在任一个请求中修改 context，另一个请求<strong>也能看到变化</strong></li>
 *             <li>适用于需要在请求链中传递状态的场景，如追踪 ID、用户信息等</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>各属性用途说明</h3>
 * <ul>
 *     <li><b>headers</b>：HTTP 请求头，用于传递认证信息、内容类型等 HTTP 协议级别的元数据</li>
 *     <li><b>interceptors</b>：拦截器链，用于在请求发送前后执行自定义逻辑（如日志、重试、监控等）</li>
 *     <li><b>tags</b>：标签集合，用于标记和分类请求，便于日志追踪、监控统计等场景</li>
 *     <li><b>context</b>：上下文 Map，用于在请求处理过程中传递自定义状态和数据</li>
 * </ul>
 *
 * @param <R> 应答类型
 */
public abstract class ApiRequest<R extends ApiResponse> {

    private final Type responseType;
    private final Map<String, String> headers;
    private final List<Interceptor> interceptors;
    private final Set<String> tags;
    private final Map<String, Object> context;

    /**
     * 构造 API 请求（默认空配置）
     * <p>
     * 创建一个基础的 API 请求，所有可选属性均为空或空集合。
     * context 初始化为空的 ConcurrentHashMap，支持后续添加上下文数据。
     * </p>
     *
     * @param responseType 应答类型
     */
    protected ApiRequest(Type responseType) {
        requireNonNull(responseType, "responseType must not be null");
        this.responseType = responseType;
        this.headers = Map.of();
        this.interceptors = List.of();
        this.tags = Set.of();
        this.context = new ConcurrentHashMap<>();
    }

    /**
     * 构造 API 请求（基于 Builder）
     * <p>
     * 从 Builder 中复制属性创建请求实例。
     * </p>
     * <p>
     * <b>注意传播行为：</b>
     * <ul>
     *     <li>{@code headers}、{@code interceptors}、{@code tags}：创建不可变副本（阻断传播）</li>
     *     <li>{@code context}：直接引用 Builder 中的 context 对象（共享传播）</li>
     * </ul>
     *
     * @param responseType 应答类型
     * @param builder      构建器
     */
    protected ApiRequest(Type responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType must not be null");
        requireNonNull(builder, "builder must not be null");
        this.responseType = responseType;
        this.headers = CommonUtils.unmodifiableCopy(builder.headers);
        this.interceptors = CommonUtils.unmodifiableCopy(builder.interceptors);
        this.tags = CommonUtils.unmodifiableCopy(builder.tags);
        this.context = null != builder.context
                ? builder.context
                : new ConcurrentHashMap<>();
    }

    /**
     * @return HTTP 请求头（不可变 Map）
     * <p>
     * 包含认证 token、内容类型等 HTTP 协议级别的元数据。
     * 此属性具有<b>阻断传播</b>特性，新请求获得独立副本。
     * </p>
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * @return 应答类型
     */
    public Type responseType() {
        return responseType;
    }

    /**
     * @return 拦截器链（不可变 List）
     * <p>
     * 按顺序执行的拦截器列表，用于在请求前后执行自定义逻辑。
     * 此属性具有<b>阻断传播</b>特性，新请求获得独立副本。
     * </p>
     */
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    /**
     * @return 标签集合（不可变 Set）
     * <p>
     * 用于标记和分类请求，便于日志追踪、监控统计等场景。
     * 此属性具有<b>阻断传播</b>特性，新请求获得独立副本。
     * </p>
     */
    public Set<String> tags() {
        return tags;
    }

    /**
     * @return 上下文 Map（可变 ConcurrentHashMap）
     * <p>
     * 用于在请求处理过程中传递自定义状态和数据（如追踪 ID、用户信息等）。
     * 此属性具有<b>共享传播</b>特性，新请求和原请求共享同一个 context 对象。
     * </p>
     */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * 构建 HttpRequest
     * <p>
     * 允许实现者自定义实现{@code HTTP}请求。DashScope协议要求了多种方式（GET、POST）。
     * 不同的协议下采用的方式不一样，所以这里直接将{@code HTTP}请求的构造开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code API -> HTTP}</p>
     *
     * @param host 主机名
     * @return {@code HTTP}请求
     */
    abstract public okhttp3.Request toHttpRequest(String host);

    /**
     * {@code HTTP}响应节码器
     * <p>
     * 允许实现者自定义应答解码。
     * </p>
     * <p>{@code (HTTP, BODY) -> R} </p>
     *
     * @return API 应答
     */
    abstract public BiFunction<okhttp3.Response, String, R> responseDecoder();


    /**
     * 构建器
     *
     * @param <T> API请求类型
     * @param <B> 构建器类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private Map<String, String> headers;
        private List<Interceptor> interceptors;
        private Set<String> tags;
        private Map<String, Object> context;

        protected Builder() {
        }

        /**
         * 从现有请求初始化 Builder
         * <p>
         * <b>传播行为说明：</b>
         * <ul>
         *     <li>{@code headers}、{@code interceptors}：浅拷贝引用（后续通过 setters 修改时会创建新集合）</li>
         *     <li>{@code context}：<b>直接共享引用</b>，新请求和原请求将共享同一个 context 对象</li>
         *     <li>{@code tags}：未在此处复制，需通过 setter 显式设置</li>
         * </ul>
         *
         * @param request 原始请求
         */
        protected Builder(ApiRequest<?> request) {
            this.headers = request.headers;
            this.interceptors = request.interceptors;
            this.context = request.context;
        }

        /**
         * 修改拦截器列表
         *
         * @param operator 修改操作
         * @return this
         */
        public B interceptors(UnaryOperator<List<Interceptor>> operator) {
            this.interceptors = operator.apply(CommonUtils.mutableCopy(this.interceptors));
            return self();
        }

        /**
         * 设置拦截链
         * <p>
         * 拦截器将会按照集合顺序执行。
         * <ul>
         *     <li>请求拦截顺序：FIFO；{@code interceptor1 -> interceptor2 -> interceptor3}</li>
         *     <li>响应拦截顺序：LIFO；{@code interceptor3 -> interceptor2 -> interceptor1}</li>
         * </ul>
         * </p>
         * <p>
         * <b>传播特性：</b>此属性具有<b>阻断传播</b>特性，设置后会创建新的独立列表，不影响其他请求。
         * </p>
         *
         * @param interceptors 拦截链
         * @return this
         */
        public B interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return self();
        }

        /**
         * 设置标签集合
         * <p>
         * 用于标记和分类请求，便于日志追踪、监控统计等场景。
         * </p>
         * <p>
         * <b>传播特性：</b>此属性具有<b>阻断传播</b>特性，设置后会创建新的独立集合，不影响其他请求。
         * </p>
         *
         * @param tags 标签集合
         * @return this
         */
        public B tags(Set<String> tags) {
            this.tags = tags;
            return self();
        }

        /**
         * 修改标签集合
         * <p>
         * <b>传播特性：</b>此属性具有<b>阻断传播</b>特性，修改后不会影响其他请求的标签集合。
         * </p>
         *
         * @param operator 修改操作
         * @return this
         */
        public B tags(UnaryOperator<Set<String>> operator) {
            this.tags = operator.apply(CommonUtils.mutableCopy(this.tags));
            return self();
        }

        /**
         * 设置上下文 Map
         * <p>
         * 用于在请求处理过程中传递自定义状态和数据（如追踪 ID、用户信息等）。
         * </p>
         * <p>
         * <b>传播特性：</b>此属性具有<b>共享传播</b>特性，如果从现有请求构建，新请求和原请求将共享同一个 context 对象。
         * 在此处显式设置会替换为新的 context，打破共享关系。
         * </p>
         *
         * @param context 上下文 Map
         * @return this
         */
        public B context(Map<String, Object> context) {
            this.context = context;
            return self();
        }

        /**
         * 修改上下文 Map
         * <p>
         * <b>传播特性：</b>默认情况下 context 是共享的。使用此方法修改时，如果传入的 operator 返回的是新 Map，
         * 则会打破共享关系；如果直接在原 Map 上修改，则所有共享该 context 的请求都能看到变化。
         * </p>
         *
         * @param operator 修改操作
         * @return this
         */
        public B context(UnaryOperator<Map<String, Object>> operator) {
            this.context = operator.apply(CommonUtils.mutableCopy(this.context));
            return self();
        }

        /**
         * 设置 HTTP 请求头
         * <p>
         * 用于传递认证信息、内容类型等 HTTP 协议级别的元数据。
         * </p>
         * <p>
         * <b>传播特性：</b>此属性具有<b>阻断传播</b>特性，设置后会创建新的独立 Map，不影响其他请求。
         * </p>
         *
         * @param headers HTTP 请求头
         * @return this
         */
        public B headers(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        /**
         * 修改 HTTP 请求头
         * <p>
         * <b>传播特性：</b>此属性具有<b>阻断传播</b>特性，修改后不会影响其他请求的请求头。
         * </p>
         *
         * @param operator 修改操作
         * @return this
         */
        public B headers(UnaryOperator<Map<String, String>> operator) {
            this.headers = operator.apply(CommonUtils.mutableCopy(this.headers));
            return self();
        }

    }

}
