package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.FilesOp;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor
public class FilesOpImpl implements FilesOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<FileMeta> upload(URI resource, String filename, Purpose purpose) {
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
        return null;
    }

    @Override
    public CompletionStage<Boolean> delete(String id) {
        return null;
    }

    @Override
    public CompletionStage<Boolean> delete(String id, boolean isForce) {
        return null;
    }

    @Override
    public CompletionStage<Iterator<FileMeta>> iterator() {
        return null;
    }

}
