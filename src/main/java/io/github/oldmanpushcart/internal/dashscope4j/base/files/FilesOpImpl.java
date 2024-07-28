package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CacheUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_FILES;
import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_IDX_CACHE_FILES_FILEID_CACHE_KEY;

public class FilesOpImpl implements FilesOp {

    private final ApiExecutor executor;
    private final Cache cache;
    private final Cache index;

    public FilesOpImpl(ApiExecutor executor, CacheFactory cacheFactory) {
        this.executor = executor;
        this.cache = cacheFactory.make(CACHE_NAMESPACE_FOR_FILES);
        this.index = cacheFactory.make(CACHE_NAMESPACE_FOR_IDX_CACHE_FILES_FILEID_CACHE_KEY);
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
                .<FileMeta>thenApply(json -> JacksonUtils.toObject(json, FileMetaImpl.class))

                /*
                 * 添加[fileid-key]倒排索引，在其他场景可以通过fileid找回key
                 * fix: 2.1.1
                 */
                .whenComplete((r, ex) -> {
                    if (null == ex) {
                        index.put(r.id(), key);
                    }
                });
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

                    /*
                     * 根据[fileid-key]倒排索引找回key，根据key完成对cache数据的清理
                     * fix: 2.1.1
                     */
                    final var key = index.remove(id);
                    if (null != key) {
                        cache.remove(key);
                    }

                    return response.output().deleted();
                })
                .exceptionallyCompose(ex -> isForce
                        ? CompletableFuture.completedFuture(false)
                        : CompletableFuture.failedFuture(ex)
                );
    }

    @Override
    public CompletableFuture<Iterator<FileMeta>> iterator() {
        final var request = FileListRequest.newBuilder()
                .build();
        return executor.async(request)
                .thenApply(response -> response.output().data().iterator());
    }

}
