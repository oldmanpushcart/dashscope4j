package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.MultipartBodyPublisherBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;

/**
 * 上传数据请求
 */
public final class StoreUploadRequest implements HttpApiRequest<StoreUploadResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private static final int SUCCESS_CODE = 200;
    private static final AtomicInteger sequencer = new AtomicInteger(1000);
    private final URI resource;
    private final Model model;
    private final StorePolicy policy;
    private final Duration timeout;
    private final String ossKey;

    public StoreUploadRequest(URI resource, Model model, StorePolicy policy) {
        this(resource, model, policy, null);
    }

    @Override
    public String suite() {
        return "dashscope://base/store";
    }

    @Override
    public String type() {
        return "upload";
    }

    /**
     * 构造上传请求
     *
     * @param resource 上传资源
     * @param model    指定模型
     * @param policy   上传凭证
     * @param timeout  上传超时
     */
    public StoreUploadRequest(URI resource, Model model, StorePolicy policy, Duration timeout) {
        this.resource = resource;
        this.model = model;
        this.policy = policy;
        this.timeout = timeout;
        this.ossKey = computeOssKey(resource, policy);
    }

    // 计算OSS-KEY
    private static String computeOssKey(URI resource, StorePolicy policy) {
        final var path = resource.getPath();
        final var name = path.substring(path.lastIndexOf('/') + 1);
        final var index = name.lastIndexOf('.');
        final var suffix = index == -1 ? "" : name.substring(index + 1);
        return String.format("%s/%s.%s",
                policy.oss().directory(),
                UUID.randomUUID(),
                suffix
        );
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("{}/{} => {}", protocol(), ossKey, resource);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
        return HttpRequest.newBuilder()
                .uri(URI.create(policy.oss().host()))
                .header(HEADER_CONTENT_TYPE, "multipart/form-data; boundary=%s".formatted(boundary))
                .header("x-oss-object-acl", policy.oss().acl())
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
    public <T> Function<HttpResponse<T>, HttpResponse<T>> newHttpResponseHandler() {
        return response -> {
            final var code = response.statusCode();
            logger.debug("{}/{} <= {}", protocol(), ossKey, code);
            if (code != SUCCESS_CODE) {
                throw new RuntimeException("upload failed! code=%s;body=%s;".formatted(
                        code,
                        response.body()
                ));
            }
            return response;
        };
    }

    @Override
    public Function<String, StoreUploadResponse> newResponseDecoder() {
        return unused ->
                new StoreUploadResponse(
                        ApiResponse.EMPTY_UUID,
                        Ret.ofSuccess("success"),
                        Usage.empty(),
                        new StoreUploadResponse.Output(
                                resource,
                                model,
                                URI.create("oss://%s".formatted(ossKey))
                        )
                );
    }

    @Override
    public Duration timeout() {
        return timeout;
    }


}
