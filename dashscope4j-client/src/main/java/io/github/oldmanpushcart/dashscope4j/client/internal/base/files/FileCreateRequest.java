package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.http.OctetStreamRequestBody;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils.removeQuotes;
import static io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils.toJson;

public class FileCreateRequest extends OpenAiRequest<FileCreateResponse> {

    private static final AtomicInteger sequencer = new AtomicInteger(1000);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final URI resource;
    private final String filename;
    private final Purpose purpose;

    public FileCreateRequest(URI resource, String filename, Purpose purpose) {
        super(FileCreateResponse.class);
        this.resource = resource;
        this.filename = filename;
        this.purpose = purpose;
    }

    @Override
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/create >>> resource={};purpose={};", resource, purpose);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
        return new Request.Builder()
                .url(EndpointUtils.https(host, "/compatible-mode/v1/files").toString())
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("purpose", removeQuotes(toJson(purpose)))
                        .addFormDataPart("file", filename, new OctetStreamRequestBody(resource))
                        .build())
                .build();
    }

    @Override
    public BiFunction<Response, String, FileCreateResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/create <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileCreateResponse.class, this, httpResponse);
        };
    }

}
