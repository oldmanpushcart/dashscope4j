package io.github.oldmanpushcart.internal.dashscope4j.base.api.http;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.internal.dashscope4j.util.FeatureDetection;
import io.github.oldmanpushcart.internal.dashscope4j.util.MapFlowProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * HTTP-SSE事件发布器
 */
public class HttpSsEventFlowPublisher implements Flow.Publisher<HttpSsEvent> {

    private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_NAME);
    private static final int DEFAULT_BUFFER_SIZE = 10240;
    private final Flow.Publisher<HttpSsEvent> delegate;

    private HttpSsEventFlowPublisher(Flow.Publisher<HttpSsEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super HttpSsEvent> subscriber) {
        delegate.subscribe(subscriber);
    }


    /**
     * 从{@code List<ByteBuffer>}转换为SSE事件发布器
     *
     * @param source  源
     * @param charset 字符集
     * @param size    缓冲区大小
     * @return SSE事件发布器
     */
    public static HttpSsEventFlowPublisher ofByteBufferListFlowPublisher(Flow.Publisher<List<ByteBuffer>> source, Charset charset, int size) {

        final var detection = new FeatureDetection(new byte[]{'\n', '\n'});
        final var output = new ByteArrayOutputStream();
        final var bytes = new byte[size];

        final var publisher = MapFlowProcessor.syncOneToMany(source, buffers -> {
            final var events = new LinkedList<HttpSsEvent>();
            for (ByteBuffer buffer : buffers) {

                // 读取缓冲区
                while (buffer.hasRemaining()) {
                    final var length = Math.min(buffer.remaining(), bytes.length);
                    buffer.get(bytes, 0, length);
                    var offset = 0;
                    while (true) {

                        // 检测SSE边界，通常是两个换行符
                        final var position = detection.screening(bytes, offset, length - offset);

                        // 未找到SSE边界，直接写入
                        if (position == -1) {
                            output.write(bytes, offset, length - offset);
                            break;
                        }

                        // 找到SSE边界，写入并转换SSE
                        else {

                            output.write(bytes, offset, position - offset);
                            offset = position + 1;

                            // 转换SSE
                            try {
                                final var body = output.toString(charset).trim();

                                if(logger.isTraceEnabled()) {
                                    logger.trace("HTTP-SSE: << {}", String.join("|", body.split("\n")));
                                }

                                final var event = HttpSsEvent.parse(body);
                                events.add(event);
                            } finally {
                                output.reset();
                            }

                        }
                    }
                }
            }
            return events;
        });

        return new HttpSsEventFlowPublisher(publisher);
    }


    /**
     * 从{@code List<ByteBuffer>}转换为SSE事件发布器
     *
     * @param source  源
     * @param charset 字符集
     * @return SSE事件发布器
     */
    public static HttpSsEventFlowPublisher ofByteBufferListFlowPublisher(Flow.Publisher<List<ByteBuffer>> source, Charset charset) {
        return ofByteBufferListFlowPublisher(
                source,
                charset,
                DEFAULT_BUFFER_SIZE
        );
    }

}
