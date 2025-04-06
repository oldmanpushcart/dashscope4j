package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import io.github.oldmanpushcart.dashscope4j.util.ProgressListener;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class StoreOpImpl implements StoreOp {

    private final ApiOp apiOp;
    private final Map<String, Policy> policiesCache = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {
        return upload(resource, model, ProgressListener.empty);
    }

    @Override
    public CompletionStage<URI> upload(URI resource, Model model, ProgressListener listener) {
        return completedFuture(null)
                .thenCompose(unused -> fetchPolicy(model))
                .thenCompose(policy -> upload(policy, resource, listener));
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

    private CompletionStage<URI> upload(Policy policy, URI resource, ProgressListener listener) {
        final PostUploadRequest request = PostUploadRequest.newBuilder()
                .policy(policy)
                .resource(resource)
                .listener(listener)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(PostUploadResponse::output);
    }

}
