package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.http.HttpHeader;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FeatureDetection;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils.isNotBlank;

public class HttpFlowExecutor {

    private final String ak;
    private final HttpClient http;

    public HttpFlowExecutor(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(T request) {


        return new Flow.Publisher<R>() {

            @Override
            public void subscribe(Flow.Subscriber<? super R> subscriber) {

                final var submissionPublisher = new SubmissionPublisher<R>();

                try {

                    final var encoder = request.newHttpRequestEncoder();
                    final var decoder = request.newHttpResponseDecoder();

                    final var httpRequest = HttpRequest.newBuilder(encoder.apply(request), (k, v) -> true)
                            .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                            .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                            .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                            .header(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                            .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                            .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                            .build();

                    http.sendAsync(httpRequest, new ServerSentEventBodyHandler())
                            .thenAccept(httpResponse -> {


                                // 消费HTTP连接中得SSE
                                httpResponse.body().subscribe(new Flow.Subscriber<>() {

                                    private Flow.Subscription subscription;

                                    @Override
                                    public void onSubscribe(Flow.Subscription subscription) {
                                        this.subscription = subscription;
                                        submissionPublisher.subscribe(subscriber);
                                        subscription.request(1);
                                    }

                                    @Override
                                    public void onNext(Item item) {

                                        try {

                                            final var response = decoder.apply(httpResponse, item.data());
                                            if (!response.isSuccess()) {
                                                throw new ApiException(response);
                                            }

                                            submissionPublisher.submit(response);
                                            subscription.request(1);

                                        } catch (Throwable ex) {
                                            onError(ex);
                                        }

                                    }

                                    @Override
                                    public void onError(Throwable ex) {
                                        submissionPublisher.closeExceptionally(ex);
                                    }

                                    @Override
                                    public void onComplete() {
                                        submissionPublisher.close();
                                    }

                                });

                            })
                            .exceptionally(ex -> {
                                ensureSubscribedAndCloseExceptionally(submissionPublisher, ex);
                                return null;
                            });

                } catch (Throwable ex) {
                    ensureSubscribedAndCloseExceptionally(submissionPublisher, ex);
                }

            }

            /**
             * 确保订阅者已经订阅，并关闭异常
             *
             * @param publisher 发布者
             * @param ex 异常
             * @param <T> 泛型
             */
            private static <T> void ensureSubscribedAndCloseExceptionally(SubmissionPublisher<? extends T> publisher, Throwable ex) {

                /*
                 * 这里为了符合规范，如果没有人订阅，则先创建一个订阅者，并取消订阅
                 */
                if (!publisher.hasSubscribers()) {
                    publisher.subscribe(new Flow.Subscriber<T>() {

                        @Override
                        public void onSubscribe(Flow.Subscription s) {
                            s.cancel();
                        }

                        @Override
                        public void onNext(Object item) { /* ignore */ }

                        @Override
                        public void onError(Throwable t) { /* ignore */ }

                        @Override
                        public void onComplete() { /* ignore */ }

                    });
                }

                // 正式关闭
                publisher.closeExceptionally(ex);

            }

        };
    }


    /**
     * HTTP响应处理
     */
    private static class ServerSentEventBodyHandler implements HttpResponse.BodyHandler<Flow.Publisher<Item>> {

        @Override
        public HttpResponse.BodySubscriber<Flow.Publisher<Item>> apply(HttpResponse.ResponseInfo responseInfo) {

            final var ct = HttpHeader.ContentType.parse(responseInfo.headers());
            final var charset = ct.charset();

            return new ServerSentEventBodySubscriber(charset);
        }

    }

    private static class ServerSentEventBodySubscriber implements HttpResponse.BodySubscriber<Flow.Publisher<Item>> {

        private final Charset charset;

        private final SubmissionPublisher<Item> publisher = new SubmissionPublisher<>();
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final byte[] bytes = new byte[10240];
        private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        private final FeatureDetection detection = new FeatureDetection(new byte[]{'\n', '\n'});

        private ServerSentEventBodySubscriber(Charset charset) {
            this.charset = charset;
        }

        @Override
        public CompletionStage<Flow.Publisher<Item>> getBody() {
            return CompletableFuture.completedStage(publisher);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (subscriptionRef.compareAndSet(null, subscription)) {
                subscription.request(1);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            try {
                for (ByteBuffer buffer : buffers) {
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
                                flush();
                            }
                        }
                    }
                }
                subscriptionRef.get().request(1);
            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            publisher.closeExceptionally(ex);
        }

        @Override
        public void onComplete() {
            try {
                flush();
            } catch (Throwable ex) {
                onError(ex);
                return;
            }
            publisher.close();
        }

        private void flush() {
            if (output.size() == 0) {
                return;
            }
            try {
                final var body = output.toString(charset).trim();
                final var item = Item.parse(body);
                publisher.submit(item);
            } finally {
                output.reset();
            }
        }

    }

    /**
     * SSE-Item
     */
    public record Item(String id, String event, String data) {

        public static Item parse(String text) {

            String id = null;
            String event = null;
            final StringBuilder dataBuf = new StringBuilder();

            try (final Scanner scanner = new Scanner(text)) {


                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine();

                    // 过滤掉空行（如有）
                    if (!isNotBlank(line)) {
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
                        case "event" -> event = value;
                        case "data" -> dataBuf.append(value).append("\n");
                        default -> {
                            // TODO: log this
                        }
                    }


                }// while
            }// try

            return new Item(id, event, dataBuf.toString());

        }

    }

}
