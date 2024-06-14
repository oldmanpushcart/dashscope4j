package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCache;
import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadOp;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.InterceptorHelper;
import io.github.oldmanpushcart.internal.dashscope4j.util.CacheUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_UPLOAD;

public class UploadOpImpl implements UploadOp {

    private final ApiExecutor apiExecutor;
    private final PersistentCache cache;
    private final InterceptorHelper interceptorHelper;

    public UploadOpImpl(ApiExecutor apiExecutor, PersistentCacheFactory persistentCacheFactory, InterceptorHelper interceptorHelper) {
        this.apiExecutor = apiExecutor;
        this.cache = persistentCacheFactory.make(CACHE_NAMESPACE_FOR_UPLOAD);
        this.interceptorHelper = interceptorHelper;
    }

    private static String toCacheKey(URI resource, Model model) {
        return "%s|%s".formatted(
                resource.toString(),
                model.name()
        );
    }

    @Override
    public CompletableFuture<URI> upload(URI resource, Model model) {
        final var key = toCacheKey(resource, model);
        return CacheUtils
                .asyncGetOrPut(cache, key, () -> {
                    final var request = UploadRequest.newBuilder()
                            .resource(resource)
                            .model(model)
                            .build();
                    return upload(request)
                            .async()
                            .thenApply(response -> response.output().uploaded().toString());
                })
                .thenApply(URI::create);
    }

    public DashScopeClient.OpAsync<UploadResponse> upload(UploadRequest request) {
        final var context = interceptorHelper.newInvocationContext();
        return () -> interceptorHelper.preHandle(context, request)
                .thenCompose(req -> CompletableFuture.completedFuture(null)

                        // 获取上传凭证
                        .thenCompose(unused -> apiExecutor.async(new UploadGetRequest(
                                req.model(),
                                req.timeout()
                        )))

                        // 上传资源
                        .thenCompose(getResponse -> apiExecutor.async(new UploadPostRequest(
                                req.resource(),
                                req.model(),
                                getResponse.output().upload(),
                                req.timeout()
                        )))

                        // 构建上传响应
                        .thenApply(postResponse -> new UploadResponseImpl(
                                postResponse.uuid(),
                                postResponse.ret(),
                                postResponse.usage(),
                                new UploadResponseImpl.OutputImpl(
                                        req.resource(),
                                        req.model(),
                                        postResponse.output().uploaded()
                                )
                        ))

                )
                .thenCompose(res -> interceptorHelper.postHandle(context, res));
    }

}
