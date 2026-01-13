package io.github.oldmanpushcart.dashscope4j.client.base.files;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface FilesOp {

    CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose);

    default CompletionStage<FileMeta> create(File file, Purpose purpose) {
        return create(file.toURI(), file.getName(), purpose);
    }

    CompletionStage<FileMeta> detail(String identity);

    CompletionStage<Boolean> delete(String identity);

    CompletionStage<List<FileMeta>> list(String after, int limit);

    Flow.Publisher<FileMeta> flow();

}
