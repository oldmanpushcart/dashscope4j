package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.store.StoreOp;
import lombok.AllArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class StoreOpImpl implements StoreOp {

    private static final String CACHE_NAMESPACE_FOR_STORE = "store";
    private final Cache cache;
    private final OkHttpClient http;
    private final ApiOp apiOp;
    private final Map<String, Policy> policiesCache = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<URI> upload(URI resource, Model model) {

        final String cacheKey = String.format("%s|%s", resource.toString(), model.name());
        final URI cached = cache.get(CACHE_NAMESPACE_FOR_STORE, cacheKey)
                .filter(Cache.Entry::isNotExpired)
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
                        cache.put(CACHE_NAMESPACE_FOR_STORE, cacheKey, v.toString().getBytes(UTF_8));
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
        final String ossKey = computeOssKey(policy, resource);
        final Request request = new Request.Builder()
                .url(policy.oss().host())
                .addHeader("x-oss-object-acl", policy.oss().acl())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("OSSAccessKeyId", policy.oss().ak())
                        .addFormDataPart("policy", policy.value())
                        .addFormDataPart("Signature", policy.signature())
                        .addFormDataPart("key", ossKey)
                        .addFormDataPart("x-oss-object-acl", policy.oss().acl())
                        .addFormDataPart("x-oss-forbid-overwrite", String.valueOf(policy.oss().isForbidOverwrite()))
                        .addFormDataPart("success_action_status", String.valueOf(200))
                        .addFormDataPart("file", resource.getPath(), new OctetStreamRequestBody(resource))
                        .build()
                )
                .build();

        final CompletableFuture<URI> completed = new CompletableFuture<>();
        http.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                completed.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException(String.format("upload failed! code=%s;desc=%s;",
                            response.code(),
                            response.message()
                    ));
                } else {
                    completed.complete(URI.create(String.format("oss://%s", ossKey)));
                }
            }

        });

        return completed;
    }

    // 计算OSS-KEY
    private static String computeOssKey(Policy policy, URI resource) {
        final String path = resource.getPath();
        final String name = path.substring(path.lastIndexOf('/') + 1);
        final int index = name.lastIndexOf('.');
        final String suffix = index == -1 ? "" : name.substring(index + 1);
        return String.format("%s/%s.%s",
                policy.oss().directory(),
                UUID.randomUUID(),
                suffix
        );
    }

}
