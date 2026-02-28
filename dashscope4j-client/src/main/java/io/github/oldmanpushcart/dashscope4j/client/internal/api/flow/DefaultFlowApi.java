package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;

public class DefaultFlowApi implements FlowApi, InternalContents {

    private final String host;
    private final String ak;
    private final OkHttpClient http;

    public DefaultFlowApi(String host, String ak, OkHttpClient http) {
        this.host = host;
        this.ak = ak;
        this.http = http;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> execute(T request) {

        return Flux.create(sink -> {

            final var httpRequest = new Request.Builder(request.toHttpRequest(host))
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();

            //noinspection resource
            final var scope = Tracer.getDefault().enter("http-sse");
            scope.span()
                    .property("method", httpRequest.method())
                    .property("url", httpRequest.url().toString());

            final var listener = new EventSourceListener() {

                private volatile Response httpResponse;

                @Override
                public void onOpen(@NonNull EventSource source, @NonNull Response httpResponse) {

                    if (sink.isCancelled()) {
                        source.cancel();
                        return;
                    }
                    sink.onCancel(source::cancel);
                    this.httpResponse = httpResponse;
                }

                @Override
                public void onEvent(@NonNull EventSource source, @Nullable String id, @Nullable String type, @NonNull String data) {

                    if (!"result".equals(type) && !"error".equals(type)) {
                        throw new IllegalArgumentException("Unexpected Event-Type: " + type);
                    }

                    final var response = request.responseDecoder().apply(httpResponse, data);
                    if (!response.isSuccess()) {
                        throw new ApiException(response);
                    }

                    if (!sink.isCancelled()) {
                        sink.next(response);
                    }

                }

                @Override
                public void onClosed(@NonNull EventSource source) {
                    if (!sink.isCancelled()) {
                        sink.complete();
                    }
                }

                @Override
                public void onFailure(@NonNull EventSource source, @Nullable Throwable t, @Nullable Response httpResponse) {

                    if (sink.isCancelled()) {
                        return;
                    }

                    if (null != t) {
                        sink.error(t);
                        return;
                    }

                    if (null != httpResponse
                            && !httpResponse.isSuccessful()
                            && "application/json".equals(httpResponse.header(HTTP_HEADER_CONTENT_TYPE))) {

                        try {
                            final var stringResponseBody = httpResponse.body().string();
                            final var response = request.responseDecoder().apply(httpResponse, stringResponseBody);
                            if (!response.isSuccess()) {
                                throw new ApiException(response);
                            }
                        } catch (Throwable ex) {
                            sink.error(ex);
                        }

                    }

                    sink.error(new IllegalStateException("SSE failed!"));

                }

            };

            EventSources.createFactory(http)
                    .newEventSource(httpRequest, listener);

        }, FluxSink.OverflowStrategy.BUFFER);



    }

}
