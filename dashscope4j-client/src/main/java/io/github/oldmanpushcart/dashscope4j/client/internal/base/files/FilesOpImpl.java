package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class FilesOpImpl implements FilesOp {

    private final DashscopeClient client;

    public FilesOpImpl(DashscopeClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose) {
        final var request = new FileCreateRequest(resource, filename, purpose);
        return client.async(request)
                .thenApply(FileCreateResponse::meta);
    }

    @Override
    public CompletionStage<FileMeta> detail(String identity) {
        final var request = new FileDetailRequest(identity);
        return client.async(request)
                .thenApply(FileDetailResponse::meta)
                .handle((meta, ex) -> {

                    if (null == ex) {
                        return CompletableFuture.completedStage(meta);
                    }

                    if (isCauseByFileNotExisted(ex)) {
                        return CompletableFuture.<FileMeta>completedStage(null);
                    } else {
                        return CompletableFuture.<FileMeta>failedStage(ex);
                    }

                })
                .thenCompose(v -> v);
    }

    private static boolean isCauseByFileNotExisted(Throwable ex) {
        final Throwable cause = CompletableFutureUtils.unwrapEx(ex);
        if (cause instanceof ApiException apiEx) {
            return Ret.CODE_FAILURE.equals(apiEx.code())
                    && apiEx.desc() != null
                    && apiEx.desc().startsWith("No such File object:");
        }
        return false;
    }

    @Override
    public CompletionStage<Boolean> delete(String identity) {
        final var request = new FileDeleteRequest(identity);
        return client.async(request)
                .thenApply(FileDeleteResponse::deleted)
                .handle((deleted, ex) -> {
                    if (ex == null) {
                        return CompletableFuture.completedStage(deleted);
                    }

                    if (isCauseByFileNotExisted(ex)) {
                        return CompletableFuture.completedStage(false);
                    } else {
                        return CompletableFuture.<Boolean>failedStage(ex);
                    }
                })
                .thenCompose(v -> v);
    }

    private CompletionStage<FileListResponse> list(String after, int limit) {
        final var request = new FileListRequest(after, limit);
        return client.async(request);
    }

    @Override
    public Flow.Publisher<FileMeta> flow(int batch) {
        return fetchPage(null, batch);
    }

    private Flow.Publisher<FileMeta> fetchPage(String after, int batch) {
        return FlowX.defer(() -> {
            final CompletionStage<Flow.Publisher<FileMeta>> future = list(after, batch)
                    .thenApply(listResponse -> {
                        final var metas = listResponse.metas();
                        if (metas.isEmpty()) {
                            return FlowX.empty();
                        }
                        return FlowX
                                .fromIterable(metas)
                                .concat(FlowX.defer(() ->
                                        listResponse.hasNext()
                                                ? fetchPage(metas.get(metas.size() - 1).identity(), batch)
                                                : FlowX.empty()));
                    });
            return FlowX.fromCompletionStage(future);
        });
    }

}
