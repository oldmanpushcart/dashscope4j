package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCache;
import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CacheUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.IteratorPublisher;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_FILES;

public class FilesOpImpl implements FilesOp {

    private final ApiExecutor executor;
    private final PersistentCache cache;

    public FilesOpImpl(ApiExecutor executor, PersistentCacheFactory persistentCacheFactory) {
        this.executor = executor;
        this.cache = persistentCacheFactory.make(CACHE_NAMESPACE_FOR_FILES);
    }

    private static String toCacheKey(URI resource, String filename) {
        return "%s|%s".formatted(
                resource.toString(),
                filename
        );
    }

    @Override
    public CompletableFuture<FileMeta> upload(URI resource, String filename) {
        final var key = toCacheKey(resource, filename);
        return CacheUtils
                .asyncGetOrPut(cache, key, () -> {
                    final var request = FileCreateRequest.newBuilder()
                            .uri(resource)
                            .name(filename)
                            .purpose("file-extract")
                            .build();
                    return executor.async(request)
                            .thenApply(response -> response.output().meta())
                            .thenApply(JacksonUtils::toJson);
                })
                .thenApply(json -> JacksonUtils.toObject(json, FileMetaImpl.class));
    }

    @Override
    public CompletableFuture<FileMeta> detail(String id) {
        final var request = FileDetailRequest.newBuilder()
                .id(id)
                .build();
        return executor.async(request)
                .thenApply(response -> response.output().meta());
    }

    @Override
    public CompletableFuture<Boolean> delete(String id) {
        return delete(id, false);
    }

    @Override
    public CompletableFuture<Boolean> delete(String id, boolean isForce) {
        final var request = FileDeleteRequest.newBuilder()
                .id(id)
                .build();
        return executor.async(request)
                .thenApply(response -> {
                    CacheUtils.removeIf(cache, json -> {
                        final var meta = JacksonUtils.toObject(json, FileMetaImpl.class);
                        return Objects.equals(meta.id(), id);
                    });
                    return response.output().deleted();
                })
                .exceptionallyCompose(ex -> isForce
                        ? CompletableFuture.completedFuture(false)
                        : CompletableFuture.failedFuture(ex)
                );
    }

    @Override
    public CompletableFuture<Flow.Publisher<FileMeta>> flow() {
        final var request = FileListRequest.newBuilder()
                .build();
        return executor.async(request)
                .thenApply(response -> IteratorPublisher.of(response.output().data()));
    }

}
