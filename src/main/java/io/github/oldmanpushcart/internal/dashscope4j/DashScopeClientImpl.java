package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.OpAsync;
import io.github.oldmanpushcart.dashscope4j.OpAsyncOpFlow;
import io.github.oldmanpushcart.dashscope4j.OpAsyncOpFlowOpTask;
import io.github.oldmanpushcart.dashscope4j.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessContentInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.process.ProcessMessageListInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.image.ImageOp;
import io.github.oldmanpushcart.internal.dashscope4j.audio.AudioOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.BaseOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.HttpApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.InterceptorApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.cache.LruCacheFactoryImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.ProcessTranscriptionForUpload;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.ProcessingContentForUpload;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.process.ProcessingMessageListForQwenLong;
import io.github.oldmanpushcart.internal.dashscope4j.chat.ChatResponseOpAsyncHandler;
import io.github.oldmanpushcart.internal.dashscope4j.chat.ChatResponseOpFlowHandler;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * DashScope客户端实现
 */
public class DashScopeClientImpl implements DashScopeClient {

    private final ApiExecutor apiExecutor;
    private final BaseOp baseOp;


    public DashScopeClientImpl(Builder builder) {
        this.apiExecutor = newInterceptorApiExecutor(builder);
        this.baseOp = new BaseOpImpl(apiExecutor, newCacheFactory(builder));
    }

    // 构建缓存工厂
    private CacheFactory newCacheFactory(Builder builder) {
        return Optional.ofNullable(builder.cacheFactory)
                .orElseGet(LruCacheFactoryImpl::new);
    }

    // 构建HTTP客户端
    private HttpClient newHttpClient(Builder builder) {
        final var httpBuilder = HttpClient.newBuilder();
        ofNullable(builder.connectTimeout).ifPresent(httpBuilder::connectTimeout);
        ofNullable(builder.executor).ifPresent(httpBuilder::executor);
        return httpBuilder.build();
    }

    // 构建ApiExecutor(Http)
    private ApiExecutor newHttpApiExecutor(Builder builder) {
        return new HttpApiExecutor(
                builder.ak,
                newHttpClient(builder),
                builder.executor,
                builder.timeout
        );
    }

    // 构建ApiExecutor(拦截器)
    private ApiExecutor newInterceptorApiExecutor(Builder builder) {
        var target = newHttpApiExecutor(builder);
        if (null != builder.interceptors) {

            final var interceptors = new ArrayList<>(builder.interceptors);

            // 文件内容上传
            interceptors.add(ProcessContentInterceptor.newBuilder()
                    .processor(new ProcessingContentForUpload())
                    .build());

            // QwenLong 对话模型支撑
            interceptors.add(ProcessMessageListInterceptor.newBuilder()
                    .processor(new ProcessingMessageListForQwenLong())
                    .build());

            // 语音转录文件上传
            interceptors.add(new ProcessTranscriptionForUpload());

            for (final var interceptor : interceptors) {
                target = new InterceptorApiExecutor(
                        this,
                        builder.executor,
                        interceptor,
                        target
                );
            }
        }
        return target;
    }

    @Override
    public OpAsyncOpFlow<ChatResponse> chat(ChatRequest request) {
        return new OpAsyncOpFlow<>() {
            @Override
            public CompletableFuture<ChatResponse> async() {
                return apiExecutor.async(request)
                        .thenCompose(new ChatResponseOpAsyncHandler(DashScopeClientImpl.this, request));
            }

            @Override
            public CompletableFuture<Flow.Publisher<ChatResponse>> flow() {
                return apiExecutor.flow(request)
                        .thenApply(new ChatResponseOpFlowHandler(DashScopeClientImpl.this, request));
            }
        };
    }

    @Override
    public <R extends HttpApiResponse<?>> OpAsyncOpFlowOpTask<R> http(HttpApiRequest<R> request) {
        return new OpAsyncOpFlowOpTask<>() {
            @Override
            public CompletableFuture<R> async() {
                return apiExecutor.async(request);
            }

            @Override
            public CompletableFuture<Flow.Publisher<R>> flow() {
                return apiExecutor.flow(request);
            }

            @Override
            public CompletableFuture<Task.Half<R>> task() {
                return apiExecutor.task(request);
            }
        };
    }

    @Override
    public BaseOp base() {
        return baseOp;
    }

    @Override
    public EmbeddingOp embedding() {
        return new EmbeddingOp() {
            @Override
            public OpAsync<EmbeddingResponse> text(EmbeddingRequest request) {
                return () -> apiExecutor.async(request);
            }

            @Override
            public OpAsync<MmEmbeddingResponse> mm(MmEmbeddingRequest request) {
                return () -> apiExecutor.async(request);
            }
        };
    }

    @Override
    public ImageOp image() {
        return request -> () -> apiExecutor.task(request);
    }

    @Override
    public AudioOp audio() {
        return new AudioOpImpl(apiExecutor);
    }


    /**
     * DashScope客户端构建器实现
     */
    public static class Builder implements DashScopeClient.Builder {

        private String ak;
        private Executor executor;
        private Duration connectTimeout;
        private Duration timeout;
        private CacheFactory cacheFactory;
        private List<Interceptor> interceptors = new ArrayList<>();

        @Override
        public DashScopeClient.Builder ak(String ak) {
            this.ak = requireNonNull(ak);
            return this;
        }

        @Override
        public DashScopeClient.Builder executor(Executor executor) {
            this.executor = requireNonNull(executor);
            return this;
        }

        @Override
        public DashScopeClient.Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = requireNonNull(connectTimeout);
            return this;
        }

        @Override
        public DashScopeClient.Builder timeout(Duration timeout) {
            this.timeout = requireNonNull(timeout);
            return this;
        }

        @Override
        public DashScopeClient.Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors = requireNonNull(interceptors);
            return this;
        }

        @Override
        public DashScopeClient.Builder appendInterceptors(List<Interceptor> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        @Override
        public DashScopeClient.Builder cacheFactory(CacheFactory factory) {
            this.cacheFactory = requireNonNull(factory);
            return this;
        }

        @Override
        public DashScopeClient build() {
            requireNonNull(ak, "ak is required!");
            requireNonNull(executor, "executor is required!");
            return new DashScopeClientImpl(this);
        }

    }

}
