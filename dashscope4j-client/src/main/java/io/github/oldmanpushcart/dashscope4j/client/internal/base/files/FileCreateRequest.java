package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.MultipartBodyPublisherBuilder;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.HttpHeader.HEADER_CONTENT_TYPE;

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
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/create >>> resource={};purpose={};", resource, purpose);
        final var boundary = "boundary%s".formatted(sequencer.incrementAndGet());
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host ,"/compatible-mode/v1/files"))
                .header(HEADER_CONTENT_TYPE, "multipart/form-data; boundary=%s".formatted(boundary))
                .POST(new MultipartBodyPublisherBuilder()
                        .boundary(boundary)
                        .part("purpose", JacksonJsonUtils.toJson(purpose).replaceAll("\"", ""))
                        .part("file", resource, filename)
                        .build())
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileCreateResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/create <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileCreateResponse.class, this, httpResponse);
        };
    }

}
