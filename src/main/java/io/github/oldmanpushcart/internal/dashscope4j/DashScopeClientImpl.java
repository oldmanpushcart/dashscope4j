package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.RequestInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.ResponseInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadOp;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadResponse;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.InterceptorApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.cache.LruCacheFactoryImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.files.FilesOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.GroupRequestInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.GroupResponseInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.InterceptorHelper;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.*;
import io.github.oldmanpushcart.internal.dashscope4j.base.upload.UploadOpImpl;
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

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * DashScope客户端实现
 */
public class DashScopeClientImpl implements DashScopeClient {

    private final ApiExecutor apiExecutor;
    private final FilesOpImpl filesOpImpl;
    private final UploadOpImpl uploadOpImpl;

    public DashScopeClientImpl(Builder builder) {
        final var cacheFactory = Optional.ofNullable(builder.cacheFactory)
                .orElseGet(LruCacheFactoryImpl::new);
        final var interceptorHelper = new InterceptorHelper(
                this,
                builder.executor,
                builder.requestInterceptors,
                builder.responseInterceptors
        );
        this.apiExecutor = new InterceptorApiExecutor(
                requireNonBlankString(builder.ak, "ak is blank"),
                newHttpClient(builder),
                requireNonNull(builder.executor),
                builder.timeout,
                interceptorHelper
        );
        this.filesOpImpl  = new FilesOpImpl(apiExecutor, cacheFactory);
        this.uploadOpImpl = new UploadOpImpl(apiExecutor, cacheFactory, interceptorHelper);
    }

    // 构建HTTP客户端
    private HttpClient newHttpClient(Builder builder) {
        final var httpBuilder = HttpClient.newBuilder();
        ofNullable(builder.connectTimeout).ifPresent(httpBuilder::connectTimeout);
        ofNullable(builder.executor).ifPresent(httpBuilder::executor);
        return httpBuilder.build();
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
    public OpTask<GenImageResponse> genImage(GenImageRequest request) {
        return () -> apiExecutor.task(request);
    }

    @Override
    public OpAsync<EmbeddingResponse> embedding(EmbeddingRequest request) {
        return () -> apiExecutor.async(request);
    }

    @Override
    public OpAsync<MmEmbeddingResponse> mmEmbedding(MmEmbeddingRequest request) {
        return () -> apiExecutor.async(request);
    }

    @Override
    public <R extends ApiResponse<?>> OpAsyncOpFlowOpTask<R> api(ApiRequest<R> request) {
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
        return new BaseOpImpl();
    }

    private class BaseOpImpl implements BaseOp {

        @Override
        public OpAsync<UploadResponse> upload(UploadRequest request) {
            return uploadOpImpl.upload(request);
        }

        @Override
        public UploadOp upload() {
            return uploadOpImpl;
        }

        @Override
        public FilesOp files() {
            return filesOpImpl;
        }

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

        private final List<RequestInterceptor> requestInterceptors = new ArrayList<>() {{

            // 添加系统默认请求拦截器
            add(new GroupRequestInterceptor(List.of(
                    new ProcessContentDataRequestInterceptorForByteArrayToFileUri(),
                    new ProcessContentDataRequestInterceptorForBufferedImageToFileUri(),
                    new ProcessContentDataRequestInterceptorForFileToUri(),
                    new ProcessChatMessageRequestInterceptorForQwenLong(),
                    new ProcessContentDataRequestInterceptorForUpload()
            )));

        }};

        private final List<ResponseInterceptor> responseInterceptors = new ArrayList<>() {{

            // 添加系统默认响应拦截器
            add(new GroupResponseInterceptor(List.of(

            )));

        }};

        @Override
        public DashScopeClient.Builder ak(String ak) {
            this.ak = requireNonBlankString(ak, "ak is blank");
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
        public List<RequestInterceptor> requestInterceptors() {
            return requestInterceptors;
        }

        @Override
        public List<ResponseInterceptor> responseInterceptors() {
            return responseInterceptors;
        }

        @Override
        public DashScopeClient.Builder cacheFactory(CacheFactory factory) {
            this.cacheFactory = factory;
            return this;
        }

        @Override
        public DashScopeClient build() {
            return new DashScopeClientImpl(this);
        }

    }

}
