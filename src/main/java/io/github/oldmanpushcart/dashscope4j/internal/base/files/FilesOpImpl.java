package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor
public class FilesOpImpl implements FilesOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose) {
        final FileCreateRequest request = FileCreateRequest.newBuilder()
                .resource(resource)
                .filename(filename)
                .purpose(purpose)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(FileCreateResponse::output);
    }

    @Override
    public CompletionStage<FileMeta> detail(String id) {
        final FileDetailRequest request = FileDetailRequest.newBuilder()
                .identity(id)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(FileDetailResponse::output);
    }

    @Override
    public CompletionStage<Boolean> delete(String id) {
        final FileDeleteRequest request = FileDeleteRequest.newBuilder()
                .identity(id)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(FileDeleteResponse::output);
    }

    @Override
    public CompletionStage<List<FileMeta>> list(String after, int limit) {
        final FileListRequest request = FileListRequest.newBuilder()
                .after(after)
                .limit(limit)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(FileListResponse::output);
    }

    @Override
    public Flowable<FileMeta> flow() {
        return fetchPage(null, 10);
    }

    private Flowable<FileMeta> fetchPage(String after, int limit) {
        return Flowable.fromCompletionStage(list(after, limit))
                .flatMap(metas -> {

                    if (metas.isEmpty() || metas.size() < limit) {
                        return Flowable.fromIterable(metas);
                    }

                    final String nextAfter = metas.get(metas.size() - 1).identity();
                    return Flowable.concat(
                            Flowable.fromIterable(metas),
                            Flowable.defer(() -> fetchPage(nextAfter, limit))
                    );

                });
    }

}
