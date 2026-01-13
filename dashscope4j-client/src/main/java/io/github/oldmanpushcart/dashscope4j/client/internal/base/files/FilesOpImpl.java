package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.net.URI;
import java.util.List;
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

    @Override
    public CompletionStage<List<FileMeta>> list(String after, int limit) {
        final var request = FileListRequest.newBuilder()
                .after(after)
                .limit(limit)
                .build();
        return asyncApi.execute(request)
                .thenApply(FileListResponse::metas);
    }

    @Override
    public Flow.Publisher<FileMeta> flow() {
        return fetchPage(null, 10);
    }

    private Flow.Publisher<FileMeta> fetchPage(String after, int limit) {
        return FlowX.defer(() -> {
            final var future = list(after, limit)
                    .thenApply(metas -> {
                        if (!metas.isEmpty()) {
                            return FlowX
                                    .fromIterable(metas)
                                    .concat(fetchPage(metas.get(metas.size() - 1).identity(), limit));
                        } else {
                            return FlowX.<FileMeta>empty();
                        }
                    });
            return FlowX.fromCompletionStage(future);
        });
    }

}
