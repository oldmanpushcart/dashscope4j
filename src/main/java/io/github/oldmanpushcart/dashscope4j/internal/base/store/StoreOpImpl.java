package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.internal.InternalContents;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class StoreOpImpl implements StoreOp {

    private final Cache cache;
    private final ApiOp apiOp;
    private final Map<String, Policy> policiesCache = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {

        final String cacheKey = String.format("%s|%s", resource.toString(), model.name());
        final URI cached = cache.get(InternalContents.CACHE_NAMESPACE_STORE, cacheKey)
                .filter(e -> !e.isExpired())
                .map(e -> new String(e.payload(), UTF_8))
                .map(URI::create)
                .orElse(null);

        if (null != cached) {
            return completedFuture(cached);
        }

        return completedFuture(null)
                .thenCompose(unused -> fetchPolicy(model))
                .thenCompose(policy -> upload(policy, resource))
                .whenComplete((v, ex) -> {
                    if (null == ex) {
                        final byte[] payload = v.toString().getBytes(UTF_8);
                        final Instant expireAt = Instant.now().plus(Duration.ofHours(48));
                        cache.put(InternalContents.CACHE_NAMESPACE_STORE, cacheKey, payload, expireAt);
                    }
                });
    }

    private CompletionStage<Policy> fetchPolicy(Model model) {
        final Policy policy = policiesCache.get(model.name());
        if (nonNull(policy) && !policy.isExpired()) {
            return completedFuture(policy);
        }
        final GetPolicyRequest request = GetPolicyRequest.newBuilder()
                .model(model)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(GetPolicyResponse::output)
                .thenApply(GetPolicyResponse.Output::policy)
                .whenComplete((v, ex) -> {
                    if (isNull(ex)) {
                        policiesCache.put(model.name(), v);
                    }
                });
    }

    private CompletionStage<URI> upload(Policy policy, URI resource) {
        final PostUploadRequest request = PostUploadRequest.newBuilder()
                .policy(policy)
                .resource(resource)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(PostUploadResponse::output);
    }

}
