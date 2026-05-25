package io.github.oldmanpushcart.dashscope4j.client.base.files;

import org.reactivestreams.Publisher;

import java.io.File;
import java.net.URI;
import java.util.concurrent.CompletionStage;

public interface FilesOp {

    CompletionStage<FileMeta> create(URI resource, String filename, Purpose purpose);

    default CompletionStage<FileMeta> create(File file, Purpose purpose) {
        return create(file.toURI(), file.getName(), purpose);
    }

    CompletionStage<FileMeta> detail(String identity);

    CompletionStage<Boolean> delete(String identity);

    default Publisher<FileMeta> flow() {
        return flow(10);
    }

    Publisher<FileMeta> flow(int batch);

}
