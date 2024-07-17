package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;

import java.net.URI;

public record FileMetaImpl(String id, String name, long size, long uploadedAt, String purpose) implements FileMeta {

    @Override
    public URI toURI() {
        return URI.create("fileid://%s".formatted(id));
    }

}
