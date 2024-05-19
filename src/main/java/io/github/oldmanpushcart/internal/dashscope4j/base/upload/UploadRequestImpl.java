package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;

import java.net.URI;
import java.time.Duration;

/**
 * 上传请求
 */
public record UploadRequestImpl(URI resource, Model model, Duration timeout) implements UploadRequest {

}
