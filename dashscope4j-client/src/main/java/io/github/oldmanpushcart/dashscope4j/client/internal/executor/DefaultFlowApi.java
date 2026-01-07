package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.HttpHeader;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FeatureDetection;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.ErrorPublisher;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.ReassemblingPublisher;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils.traceLogHttpRequest;

public class DefaultFlowApi implements FlowApi {

    private final String host;
    private final String ak;
    private final HttpClient http;

    public DefaultFlowApi(String host, String ak, HttpClient http) {
        this.host = host;
        this.ak = ak;
        this.http = http;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://flow";
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(T request) {
        try {

            final var httpRequest = HttpRequest.newBuilder(request.toHttpRequest(host), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();
            traceLogHttpRequest(httpRequest);

            return new DeferredPublisher<>(() -> http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofPublisher())
                    .whenComplete(HttpUtils::traceLogHttpResponse)
                    .thenCompose(httpResponse -> {

                        final var ct = HttpHeader.ContentType.parse(httpResponse.headers());
                        final var charset = ct.charset();

                        // sse
                        if (httpResponse.statusCode() == 200 && "text/event-stream".equalsIgnoreCase(ct.mime())) {
                            return CompletableFuture.completedStage(httpResponse.body())
                                    .thenApply(bytesPublisher -> new ReassemblingPublisher<>(bytesPublisher, new ServerSentEventReassembler(charset)))
                                    .thenApply(ssePublisher -> new ReassemblingPublisher<>(ssePublisher, new SseApiResponseReassembler<>(request, httpResponse)));
                        }

                        // error
                        else if (httpResponse.statusCode() != 200 && "application/json".equalsIgnoreCase(ct.mime())) {
                            return CompletableFuture.completedStage(httpResponse.body())
                                    .thenApply(bytesPublisher -> new ReassemblingPublisher<>(bytesPublisher, new JsonApiResponseReassembler<>(charset, request, httpResponse)));
                        }

                        // other unsupported
                        else {
                            return CompletableFuture.completedStage(
                                    new ErrorPublisher<>(
                                            new IllegalStateException("Unsupported HTTP response! code=%s;mime-type=%s;".formatted(
                                                    ct.mime(),
                                                    httpResponse.statusCode()
                                            ))));
                        }


                    }));
        } catch (Throwable ex) {
            return new ErrorPublisher<>(ex);
        }
    }

    private static class JsonApiResponseReassembler<R extends ApiResponse> implements ReassemblingPublisher.Reassembler<List<ByteBuffer>, R> {

        private final Charset charset;
        private final ApiRequest<R> request;
        private final HttpResponse<?> httpResponse;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private JsonApiResponseReassembler(Charset charset, ApiRequest<R> request, HttpResponse<?> httpResponse) {
            this.charset = charset;
            this.request = request;
            this.httpResponse = httpResponse;
        }

        @Override
        public List<R> tryAssemble(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                final var bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }
            return List.of();
        }

        @Override
        public List<R> flush() {
            try {
                final var body = baos.toString(charset);
                final var response = JacksonJsonUtils.toApiResponse(body, request.responseType(), request, httpResponse);
                if (!response.isSuccess()) {
                    throw new ApiException(response);
                }
                return List.of(response);
            } finally {
                IOUtils.closeQuietly(baos);
            }
        }

    }

    private static class SseApiResponseReassembler<R extends ApiResponse> implements ReassemblingPublisher.Reassembler<ServerSentEvent, R> {

        private final ApiRequest<R> request;
        private final HttpResponse<?> httpResponse;

        private SseApiResponseReassembler(ApiRequest<R> request, HttpResponse<?> httpResponse) {
            this.request = request;
            this.httpResponse = httpResponse;
        }

        @Override
        public List<R> tryAssemble(ServerSentEvent event) {
            final var payload = event.payload();

            // TODO: 需要修复返回无效的数据
            if("[DONE]".equalsIgnoreCase(payload)) {
                return List.of();
            }

            final var response = request.responseDecoder().apply(httpResponse, payload);
            if (!response.isSuccess()) {
                throw new ApiException(response);
            }
            return List.of(response);
        }
    }

    private static class ServerSentEventReassembler implements ReassemblingPublisher.Reassembler<List<ByteBuffer>, ServerSentEvent> {

        private final Charset charset;
        private final byte[] bytes = new byte[10240];
        private final FeatureDetection detection = new FeatureDetection(new byte[]{'\n', '\n'});
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private ServerSentEventReassembler(Charset charset) {
            this.charset = charset;
        }

        private void drainTo(List<ServerSentEvent> events) {
            if (output.size() == 0) {
                return;
            }
            try {
                final var body = output.toString(charset).trim();
                final var event = ServerSentEvent.parse(body);
                events.add(event);
            } finally {
                output.reset();
            }
        }

        @Override
        public List<ServerSentEvent> tryAssemble(List<ByteBuffer> buffers) {
            final var events = new ArrayList<ServerSentEvent>();
            for (final var buffer : buffers) {
                while (buffer.hasRemaining()) {
                    final var length = Math.min(buffer.remaining(), bytes.length);
                    buffer.get(bytes, 0, length);
                    var offset = 0;
                    while (true) {
                        final var position = detection.screening(bytes, offset, length - offset);
                        if (position == -1) {
                            output.write(bytes, offset, length - offset);
                            break;
                        } else {
                            output.write(bytes, offset, position - offset);
                            offset = position + 1;
                            drainTo(events);
                        }
                    }
                }
            }
            return events;
        }

        @Override
        public List<ServerSentEvent> flush() {
            final var events = new ArrayList<ServerSentEvent>();
            drainTo(events);
            return events;
        }

    }

    private record ServerSentEvent(String id, String type, String payload) {

        /**
         * {@code TEXT -> SSE}
         *
         * @param text 文本块
         * @return SSE 事件
         */
        private static ServerSentEvent parse(String text) {

            String id = null;
            String type = null;
            final var payloadBuf = new StringBuilder();

            try (final Scanner scanner = new Scanner(text)) {

                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine();

                    // 过滤掉空行（如有）
                    if (CommonUtils.isBlankString(line)) {
                        continue;
                    }

                    // 过滤掉:开头的行，这个是注释
                    if (line.startsWith(":")) {
                        continue;
                    }

                    /*
                     * 通过":"分割field和value
                     * 如果这一行没有":"，则整行都为field，比如retry
                     */
                    String field, value = null;
                    final int colonIndex = line.indexOf(':');
                    if (colonIndex == -1) {
                        field = line;
                    } else {
                        field = line.substring(0, colonIndex).trim().toLowerCase();
                        value = line.substring(colonIndex + 1).trim();
                    }

                    switch (field) {
                        case "id" -> id = value;
                        case "event" -> type = value;
                        case "data" -> payloadBuf.append(value).append("\n");
                    }


                }// while
            }// try

            final var payload = payloadBuf.toString().trim();
            return new ServerSentEvent(id, type, payload);

        }

    }

}
