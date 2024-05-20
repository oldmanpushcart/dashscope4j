package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProcessContentRequestInterceptorForUpload extends ProcessContentRequestInterceptor {

    private static final Duration DEFAULT_CACHE_EXPIRE = Duration.ofHours(48);
    private final ConcurrentMap<CacheKey, CacheVal> cache = new ConcurrentHashMap<>() {


    };

    @Override
    protected CompletableFuture<Object> processContentData(InvocationContext context, AlgoRequest<?> request, Object data) {

        // 如果不是URI类型的数据，则不需要上传
        if (!(data instanceof URI resource)) {
            return CompletableFuture.completedFuture(data);
        }

        // 如果已经是oss协议的URI，则不需要上传
        if ("oss".equalsIgnoreCase(resource.getScheme())) {
            return CompletableFuture.completedFuture(data);
        }

        // 只有文件协议的URI才需要上传
        if (!"file".equalsIgnoreCase(resource.getScheme())) {
            return CompletableFuture.completedFuture(data);
        }

        final var client = context.client();
        final var model = request.model();

        // 优先从缓存中寻找，如果缓存中找到则直接使用
        final var cacheKey = new CacheKey(resource, model);
        final var cacheVal = cache.get(cacheKey);
        if (Objects.nonNull(cacheVal)) {
            if (cacheVal.isExpired()) {
                cache.remove(cacheKey);
            } else {
                return CompletableFuture.completedFuture(cacheVal.uploaded());
            }
        }

        // 上传资源
        final var uploadRequest = UploadRequest.newBuilder()
                .resource(resource)
                .model(model)
                .build();
        return client.base().upload(uploadRequest).async()
                .thenApply(response -> {
                    cache.put(cacheKey, new CacheVal(
                            response.output().uploaded(),
                            System.currentTimeMillis() + DEFAULT_CACHE_EXPIRE.toMillis()
                    ));
                    return response;
                })
                .thenApply(response -> response.output().uploaded());
    }

    private record CacheKey(URI resource, Model model) {

    }

    private record CacheVal(URI uploaded, long expireMs) {

        boolean isExpired() {
            return System.currentTimeMillis() > expireMs;
        }

    }

}
