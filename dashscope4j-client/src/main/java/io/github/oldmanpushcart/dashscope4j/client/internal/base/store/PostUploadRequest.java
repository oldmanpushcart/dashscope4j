package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.http.OctetStreamRequestBody;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonXmlUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_X_OSS_OBJECT_ACL;

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
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/store/upload/{} >>> {}", ossKey, resource);
        return new Request.Builder()
                .url(URI.create(policy.oss().host()).toString())
                .header(HTTP_HEADER_X_OSS_OBJECT_ACL, policy.oss().acl())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("OSSAccessKeyId", policy.oss().ak())
                        .addFormDataPart("policy", policy.value())
                        .addFormDataPart("Signature", policy.signature())
                        .addFormDataPart("key", ossKey)
                        .addFormDataPart("x-oss-object-acl", policy.oss().acl())
                        .addFormDataPart("x-oss-forbid-overwrite", String.valueOf(policy.oss().isForbidOverwrite()))
                        .addFormDataPart("success_action_status", String.valueOf(200))
                        .addFormDataPart("file", resource.getPath(), new OctetStreamRequestBody(resource))
                        .build()
                )
                .build();
    }

    @Override
    public BiFunction<Response, String, PostUploadResponse> responseDecoder() {
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
            //noinspection UastIncorrectHttpHeaderInspection
            final String uuid = httpResponse.header("x-oss-request-id");
            return new PostUploadResponse(this, uuid, URI.create("oss://%s".formatted(ossKey)));
        };
    }

}
