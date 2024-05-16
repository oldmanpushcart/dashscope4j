package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;

import java.net.URI;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class UploadRequestBuilderImpl implements UploadRequest.Builder {

    private URI resource;
    private Model model;
    private Duration timeout;

    @Override
    public UploadRequest.Builder resource(URI resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public UploadRequest.Builder model(Model model) {
        this.model = model;
        return this;
    }

    @Override
    public UploadRequest.Builder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public UploadRequest build() {
        return new UploadRequestImpl(
                requireNonNull(resource, "resource is required"),
                requireNonNull(model, "model is required"),
                timeout
        );
    }

}
