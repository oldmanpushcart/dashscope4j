package io.github.ompc.dashscope4j.internal.api.http;

import io.github.ompc.dashscope4j.internal.util.FeatureDetection;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class HttpResponseEventSubscriber implements HttpResponse.BodySubscriber<Void> {

    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final byte[] bytes = new byte[10240];
    private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
    private final FeatureDetection detection = new FeatureDetection(new byte[]{'\n', '\n'});
    private final Charset charset;
    private final Consumer<Event> consumer;

    public HttpResponseEventSubscriber(Charset charset, Consumer<Event> consumer) {
        this.charset = charset;
        this.consumer = consumer;
    }

    @Override
    public CompletionStage<Void> getBody() {
        return future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (subscriptionRef.compareAndSet(null, subscription)) {
            subscription.request(1);
        } else {
            subscription.cancel();
        }
    }

    private synchronized void flush() {
        synchronized (bytes) {
            try {

                // 防呆判空
                if (output.size() == 0) {
                    return;
                }

                // 消费SSE
                final var body = output.toString(charset).trim();
                final var event = Event.parse(body);
                consumer.accept(event);

            } finally {
                output.reset();
            }
        }
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        synchronized (bytes) {
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
        }
        subscriptionRef.get().request(1);
    }

    @Override
    public void onError(Throwable ex) {
        future.completeExceptionally(ex);
    }

    @Override
    public void onComplete() {
        try {

            // 如果管道中还有未结束的数据，则在这里处理
            flush();

            // 结束
            future.complete(null);

        } catch (Throwable ex) {
            onError(ex);
        }
    }

    /**
     * SSE
     *
     * @param id   ID
     * @param type 类型
     * @param data 数据
     * @param meta 元数据
     */
    public record Event(String id, String type, String data, Set<String> meta) {

        @Override
        public String toString() {
            return "SSE|%s|%s|%s|%s".formatted(id, type, data, String.join(",", meta));
        }

        /**
         * 解析SSE
         *
         * @param body HTTP BODY
         * @return SSE事件
         */
        public static Event parse(String body) {
            final var meta = new LinkedHashSet<String>();
            String id = null, type = null, data = null;
            try (final var scanner = new Scanner(body)) {
                while (scanner.hasNextLine()) {
                    final var line = scanner.nextLine();
                    if (line.startsWith("id:")) {
                        id = line.substring(3).trim();
                    } else if (line.startsWith("event:")) {
                        type = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        data = line.substring(5).trim();
                    } else if (line.startsWith(":")) {
                        meta.add(line.substring(1).trim());
                    }
                }
            }
            return new Event(id, type, data, Collections.unmodifiableSet(meta));
        }

    }

}
