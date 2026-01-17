package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.AlgoModel;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class StoreOpImpl implements StoreOp {

    private final AsyncApi asyncApi;
    private final Map<String, Policy> policiesCache = new ConcurrentHashMap<>();

    public StoreOpImpl(AsyncApi asyncApi) {
        this.asyncApi = asyncApi;
    }

    @Override
    public CompletionStage<URI> upload(URI resource, AlgoModel model) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> fetchPolicy(model))
                .thenCompose(policy -> upload(policy, resource));
    }

    private CompletionStage<Policy> fetchPolicy(AlgoModel model) {
        final Policy policy = policiesCache.get(model.name());
        if (nonNull(policy) && !policy.isExpired()) {
            return completedFuture(policy);
        }
        final GetPolicyRequest request = GetPolicyRequest.newBuilder()
                .model(model)
                .build();
        return asyncApi.execute(request)
                .thenApply(GetPolicyResponse::output)
                .thenApply(GetPolicyResponse.Output::policy)
                .whenComplete((v, ex) -> {
                    if (isNull(ex)) {
                        policiesCache.put(model.name(), v);
                    }
                });
    }

    private CompletionStage<URI> upload(Policy policy, URI resource) {
        final var request = PostUploadRequest.newBuilder()
                .policy(policy)
                .resource(resource)
                .build();
        return asyncApi.execute(request)
                .thenApply(PostUploadResponse::uploaded);
    }

}
