package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CacheUtils;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_STORE;

public class StoreOpImpl implements StoreOp {

    private final ApiExecutor apiExecutor;
    private final Cache cache;

    public StoreOpImpl(ApiExecutor apiExecutor, CacheFactory cacheFactory) {
        this.apiExecutor = apiExecutor;
        this.cache = cacheFactory.make(CACHE_NAMESPACE_FOR_STORE);
    }

    private static String toCacheKey(URI resource, Model model) {
        return "%s|%s".formatted(
                resource.toString(),
                model.name()
        );
    }

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {
        final var key = toCacheKey(resource, model);
        return CacheUtils
                .asyncGetOrPut(cache, key, () -> _upload(resource, model).thenApply(Objects::toString))
                .thenApply(URI::create);
    }

    private CompletionStage<URI> _upload(URI resource, Model model) {
        return CompletableFuture.completedFuture(null)

                // 获取存储凭证
                .thenCompose(unused -> apiExecutor.async(new StoreGetPolicyRequest(model)))

                // 上传资源到存储空间
                .thenCompose(response -> apiExecutor.async(new StoreUploadRequest(
                        resource,
                        model,
                        response.output().policy()
                )))

                // 转换应答为存储URI
                .thenApply(response -> response.output().uploaded());
    }

}
