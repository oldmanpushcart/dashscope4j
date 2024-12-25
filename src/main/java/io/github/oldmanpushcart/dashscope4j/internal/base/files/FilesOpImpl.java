package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.util.Iterator;
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
    public CompletionStage<Iterator<FileMeta>> iterator() {
        final FileListRequest request = FileListRequest.newBuilder()
                .build();
        return apiOp.executeAsync(request)
                .thenApply(FileListResponse::output)
                .thenApply(List::iterator);
    }

}
