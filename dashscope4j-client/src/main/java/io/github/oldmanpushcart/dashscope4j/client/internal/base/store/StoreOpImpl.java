package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.base.store.StoreOp;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class StoreOpImpl implements StoreOp {

    private final DashscopeClient client;
    private final Map<String, Policy> policiesCache = new ConcurrentHashMap<>();

    public StoreOpImpl(DashscopeClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> fetchPolicy(model))
                .thenCompose(policy -> upload(policy, resource));
    }

    private CompletionStage<Policy> fetchPolicy(Model model) {
        final Policy policy = policiesCache.get(model.name());
        if (nonNull(policy) && !policy.isExpired()) {
            return completedFuture(policy);
        }
        final GetPolicyRequest request = new GetPolicyRequest(model);
        return client.async(request)
                .thenApply(GetPolicyResponse::output)
                .thenApply(GetPolicyResponse.Output::policy)
                .whenComplete((v, ex) -> {
                    if (isNull(ex)) {
                        policiesCache.put(model.name(), v);
                    }
                });
    }

    private CompletionStage<URI> upload(Policy policy, URI resource) {
        final var request = new PostUploadRequest(policy, resource);
        return client.async(request)
                .thenApply(PostUploadResponse::uploaded);
    }

}
