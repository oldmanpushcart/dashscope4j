package io.github.oldmanpushcart.internal.dashscope4j.base.api.http;

import io.github.oldmanpushcart.dashscope4j.util.TransformFlowProcessor;
import io.github.oldmanpushcart.internal.dashscope4j.util.FeatureDetection;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * HTTP-SSE事件处理器
 *
 * @param <T> 输入类型
 */
public class HttpSsEventProcessor<T> extends TransformFlowProcessor<T, HttpSsEvent> {

    public HttpSsEventProcessor(Function<T, List<HttpSsEvent>> transformer) {
        super(transformer);
    }

    /**
     * 从{@code List<ByteBuffer>}转换为SSE事件发布器
     *
     * @param charset 字符集
     * @param size    缓冲区大小
     * @return SSE事件发布器
     */
    public static HttpSsEventProcessor<List<ByteBuffer>> fromByteBuffers(Charset charset, int size) {

        final var detection = new FeatureDetection(new byte[]{'\n', '\n'});
        final var output = new ByteArrayOutputStream();
        final var bytes = new byte[size];

        return new HttpSsEventProcessor<>(buffers -> {
            final var events = new LinkedList<HttpSsEvent>();
            for (ByteBuffer buffer : buffers) {
                while (buffer.hasRemaining()) {
                    final var length = Math.min(buffer.remaining(), bytes.length);
                    buffer.get(bytes, 0, length);
                    var offset = 0;
                    while (true) {

                        // 检测SSE边界，通常是两个换行符
                        final var position = detection.screening(bytes, offset, length - offset);
                        if (position == -1) {

                            // 未找到SSE边界，直接写入
                            output.write(bytes, offset, length - offset);
                            break;

                        } else {

                            // 找到SSE边界，写入并转换SSE
                            output.write(bytes, offset, position - offset);
                            offset = position + 1;

                            // 转换SSE
                            try {
                                final var body = output.toString(charset).trim();
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

    }

}
