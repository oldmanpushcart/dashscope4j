package io.github.ompc.internal.dashscope4j;

import io.github.ompc.dashscope4j.DashScopeClient;
import io.github.ompc.dashscope4j.base.api.ApiRequest;
import io.github.ompc.dashscope4j.base.api.ApiResponse;
import io.github.ompc.dashscope4j.base.task.Task;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.embedding.EmbeddingRequest;
import io.github.ompc.dashscope4j.embedding.EmbeddingResponse;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;
import io.github.ompc.internal.dashscope4j.base.api.ApiExecutor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static io.github.ompc.internal.dashscope4j.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * DashScope客户端实现
 */
public class DashScopeClientImpl implements DashScopeClient {

    private final ApiExecutor apiExecutor;

    public DashScopeClientImpl(Builder builder) {
        this.apiExecutor = new ApiExecutor(
                requireNonBlankString(builder.sk),
                newHttpClient(builder),
                requireNonNull(builder.executor)
        );
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
                return apiExecutor.async(request);
            }

            @Override
            public CompletableFuture<Flow.Publisher<ChatResponse>> flow() {
                return apiExecutor.flow(request);
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


    /**
     * DashScope客户端构建器实现
     */
    public static class Builder implements DashScopeClient.Builder {

        private String sk;
        private Executor executor;
        private Duration connectTimeout;

        @Override
        public DashScopeClient.Builder sk(String sk) {
            this.sk = requireNonBlankString(sk);
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
        public DashScopeClient build() {
            return new DashScopeClientImpl(this);
        }

    }

}