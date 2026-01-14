package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class FilesOpImpl implements FilesOp {

    private final AsyncApi asyncApi;

    public FilesOpImpl(AsyncApi asyncApi) {
        this.asyncApi = asyncApi;
    }

    @Override
    public CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose) {
        final var request = FileCreateRequest.newBuilder()
                .resource(resource)
                .filename(filename)
                .purpose(purpose)
                .build();
        return asyncApi.execute(request)
                .thenApply(FileCreateResponse::meta);
    }

    @Override
    public CompletionStage<FileMeta> detail(String identity) {
        final var request = FileDetailRequest.newBuilder()
                .identity(identity)
                .build();
        return asyncApi.execute(request)
                .thenApply(FileDetailResponse::meta);
    }

    @Override
    public CompletionStage<Boolean> delete(String identity) {
        final var request = FileDeleteRequest.newBuilder()
                .identity(identity)
                .build();
        return asyncApi.execute(request)
                .thenApply(FileDeleteResponse::deleted);
    }

    private CompletionStage<FileListResponse> list(String after, int limit) {
        final var request = FileListRequest.newBuilder()
                .after(after)
                .limit(limit)
                .build();
        return asyncApi.execute(request);
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
