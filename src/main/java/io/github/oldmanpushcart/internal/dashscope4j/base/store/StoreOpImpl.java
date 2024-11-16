package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CacheUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_STORE;
import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_STORE_POLICY;

public class StoreOpImpl implements StoreOp {

    private static final Duration CACHE_EXPIRE = Duration.ofHours(48);
    private final ApiExecutor apiExecutor;
    private final Cache storecache;
    private final Cache storePolicycache;

    public StoreOpImpl(ApiExecutor apiExecutor, CacheFactory cacheFactory) {
        this.apiExecutor = apiExecutor;
        this.storecache = cacheFactory.make(CACHE_NAMESPACE_FOR_STORE);
        this.storePolicycache = cacheFactory.make(CACHE_NAMESPACE_FOR_STORE_POLICY);
    }

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {
        final var key = "%s|%s".formatted(
                resource.toString(),
                model.name()
        );
        return CacheUtils
                .asyncGetOrPut(storecache, key, () ->
                        _upload(resource, model)
                                .thenApply(Objects::toString)
                                .thenApply(v -> new CacheUtils.ExpiringValue(v, CACHE_EXPIRE))
                )
                .thenApply(URI::create);
    }

    private CompletionStage<URI> _upload(URI resource, Model model) {
        return CompletableFuture.completedFuture(null)

                // 获取存储凭证
                .thenCompose(unused -> fetchStorePolicy(model))

                // 上传资源到存储空间
                .thenCompose(policy -> apiExecutor.async(new StoreUploadRequest(
                        resource,
                        model,
                        policy
                )))

                // 转换应答为存储URI
                .thenApply(response -> response.output().uploaded());
    }

    private CompletionStage<StorePolicy> fetchStorePolicy(Model model) {
        final var key = model.name();
        return CacheUtils
                .asyncGetOrPut(storePolicycache, key, () -> _fetchStorePolicy(model)
                        .thenApply(policy -> new CacheUtils.ExpiringValue(
                                JacksonUtils.toJson(policy),
                                Duration.ofSeconds(policy.expireInSeconds())
                        ))
                )
                .thenApply(json -> JacksonUtils.toObject(json, StorePolicy.class));
    }

    private CompletionStage<StorePolicy> _fetchStorePolicy(Model model) {
        return apiExecutor.async(new StoreGetPolicyRequest(model))
                .thenApply(StoreGetPolicyResponse::output)
                .thenApply(StoreGetPolicyResponse.Output::policy);
    }

}
