package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FeatureDetection;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.http.HttpHeader;
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

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
        return FlowX.defer(() -> {

            final var httpRequest = HttpRequest.newBuilder(request.toHttpRequest(host), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();
            traceLogHttpRequest(httpRequest);

            final CompletionStage<Flow.Publisher<R>> stage = http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofPublisher())
                    .whenComplete(HttpUtils::traceLogHttpResponse)
                    .thenApply(httpResponse -> {

                        final var ct = HttpHeader.ContentType.parse(httpResponse.headers());
                        final var charset = ct.charset();

                        // sse
                        if (httpResponse.statusCode() == 200 && "text/event-stream".equalsIgnoreCase(ct.mime())) {
                            return FlowX
                                    .fromPublisher(httpResponse.body())
                                    .transform(new ByteBufferListToServerSentEventTransformer(charset))
                                    .transform(new ServerSentEventToApiResponseTransformer<>(request, httpResponse));
                        }

                        // error
                        else if (httpResponse.statusCode() != 200 && "application/json".equalsIgnoreCase(ct.mime())) {
                            return FlowX
                                    .fromPublisher(httpResponse.body())
                                    .transform(new ByteBufferListToApiResponseTransformer<>(charset, request, httpResponse));
                        }

                        // other
                        else {
                            return FlowX.error(new IllegalStateException("Unsupported HTTP response! code=%s;mime-type=%s;".formatted(
                                    ct.mime(),
                                    httpResponse.statusCode()
                            )));
                        }

                    });
            return FlowX.fromCompletionStage(stage);
        });
    }

    /**
     * {@code BYTES -> ApiResponse}流转换器
     */
    private static class ByteBufferListToApiResponseTransformer<R extends ApiResponse>
            implements Function<Flow.Publisher<List<ByteBuffer>>, Flow.Publisher<R>> {

        private final Charset charset;
        private final ApiRequest<R> request;
        private final HttpResponse<?> httpResponse;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private ByteBufferListToApiResponseTransformer(Charset charset, ApiRequest<R> request, HttpResponse<?> httpResponse) {
            this.charset = charset;
            this.request = request;
            this.httpResponse = httpResponse;
        }

        @Override
        public Flow.Publisher<R> apply(Flow.Publisher<List<ByteBuffer>> publisher) {
            return FlowX
                    .fromPublisher(publisher)
                    .flatMap(buffers -> {
                        for (final var buffer : buffers) {
                            final var bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            baos.write(bytes, 0, bytes.length);
                        }
                        return List.<R>of();
                    })
                    .concat(FlowX
                            .defer(() -> {
                                try {
                                    final var body = baos.toString(charset);
                                    final var response = JacksonJsonUtils.<R>toApiResponse(body, request.responseType(), request, httpResponse);
                                    return !response.isSuccess()
                                            ? FlowX.error(new ApiException(response))
                                            : FlowX.just(response);
                                } finally {
                                    IOUtils.closeQuietly(baos);
                                }
                            }));
        }
    }

    /**
     * {@code SSE -> ApiResponse}流转换器
     */
    private static class ServerSentEventToApiResponseTransformer<R extends ApiResponse>
            implements Function<Flow.Publisher<ServerSentEvent>, Flow.Publisher<R>> {

        private final ApiRequest<R> request;
        private final HttpResponse<?> httpResponse;

        private ServerSentEventToApiResponseTransformer(ApiRequest<R> request, HttpResponse<?> httpResponse) {
            this.request = request;
            this.httpResponse = httpResponse;
        }

        @Override
        public Flow.Publisher<R> apply(Flow.Publisher<ServerSentEvent> publisher) {
            return FlowX
                    .fromPublisher(publisher)
                    .filter(event -> !"[DONE]".equalsIgnoreCase(event.payload()))
                    .flatMap(event -> {
                        final var response = request.responseDecoder().apply(httpResponse, event.payload());
                        if (!response.isSuccess()) {
                            throw new ApiException(response);
                        }
                        return List.of(response);
                    });
        }

    }


    /**
     * {@code BYTES -> SSE}流转换器
     */
    private static class ByteBufferListToServerSentEventTransformer
            implements Function<Flow.Publisher<List<ByteBuffer>>, Flow.Publisher<ServerSentEvent>> {

        private final Charset charset;
        private final byte[] bytes = new byte[10240];
        private final FeatureDetection detection = new FeatureDetection(new byte[]{'\n', '\n'});
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private ByteBufferListToServerSentEventTransformer(Charset charset) {
            this.charset = charset;
        }

        // 将字符串转换为 SSE
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
        public Flow.Publisher<ServerSentEvent> apply(Flow.Publisher<List<ByteBuffer>> publisher) {
            return FlowX
                    .fromPublisher(publisher)

                    // 将字节流转换为 SSE 事件流
                    .flatMap(buffers -> {
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
                    })

                    // 流结束，将剩余的事件流发送走
                    .concat(FlowX.defer(() -> {
                        final var events = new ArrayList<ServerSentEvent>();
                        drainTo(events);
                        return FlowX.fromIterable(events);
                    }));
        }

    }

    /**
     * SSE
     */
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

            try (final var scanner = new Scanner(text)) {

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
