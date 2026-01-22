package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor.http.MultipartBodyPublisherBuilder;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonXmlUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_X_OSS_OBJECT_ACL;
import static io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor.http.HttpHeader.HEADER_CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

public class PostUploadRequest extends ApiRequest<PostUploadResponse> {

    private static final int SUCCESS_CODE = 200;
    private static final AtomicInteger sequencer = new AtomicInteger(1000);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Policy policy;
    private final URI resource;
    private final String ossKey;

    public PostUploadRequest(Policy policy, URI resource) {
        super(PostUploadResponse.class);
        this.policy = policy;
        this.resource = resource;
        this.ossKey = computeOssKey(policy, resource);
    }

    // 计算OSS-KEY
    private static String computeOssKey(Policy policy, URI resource) {
        final String path = resource.getPath();
        final String name = path.substring(path.lastIndexOf('/') + 1);
        final int index = name.lastIndexOf('.');
        final String suffix = index == -1 ? "" : name.substring(index + 1);
        return "%s/%s.%s".formatted(
                policy.oss().directory(),
                UUID.randomUUID(),
                suffix
        );
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/store/upload/{} >>> {}", ossKey, resource);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
        return HttpRequest.newBuilder()
                .uri(URI.create(policy.oss().host()))
                .header(HEADER_CONTENT_TYPE, "multipart/form-data; boundary=%s".formatted(boundary))
                .header(HTTP_HEADER_X_OSS_OBJECT_ACL, policy.oss().acl())
                .POST(new MultipartBodyPublisherBuilder()
                        .boundary(boundary)
                        .part("OSSAccessKeyId", policy.oss().ak())
                        .part("policy", policy.value())
                        .part("Signature", policy.signature())
                        .part("key", ossKey)
                        .part("x-oss-object-acl", policy.oss().acl())
                        .part("x-oss-forbid-overwrite", String.valueOf(policy.oss().isForbidOverwrite()))
                        .part("success_action_status", String.valueOf(SUCCESS_CODE))
                        .part("file", resource)
                        .build()
                )
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, PostUploadResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/store/upload/{} <<< {}", ossKey, responseBody);

            /*
             * 当应答内容为非空时，说明上传出现了问题。
             * 报文内容即为问题原因
             */
            if (CommonUtils.isNotBlankString(responseBody)) {
                return JacksonXmlUtils.toApiResponse(responseBody, PostUploadResponse.class, this, httpResponse);
            }

            final var httpHeaders = httpResponse.headers();

            /*
             * 请求编号被藏在了 HTTP-HEADER 中
             */
            final String uuid = httpHeaders
                    .firstValue("x-oss-request-id")
                    .orElse("");

            return new PostUploadResponse(this, uuid, URI.create("oss://%s".formatted(ossKey)));
        };
    }

}
