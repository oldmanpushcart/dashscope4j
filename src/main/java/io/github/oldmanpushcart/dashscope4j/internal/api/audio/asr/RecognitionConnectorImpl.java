package io.github.oldmanpushcart.dashscope4j.internal.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionConnector;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.internal.AbstractExchangeConnector;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

@Slf4j
public class RecognitionConnectorImpl extends AbstractExchangeConnector implements RecognitionConnector {

    private final Listener listener;
    private final ReadableByteChannel channel;
    private final int channelBufferSize;
    private final RecognitionRequest request;
    private final OpExchange<RecognitionRequest, RecognitionResponse> opExchange;

    public RecognitionConnectorImpl(Builder builder) {
        super(builder);

        // 检查参数
        requireNonNull(builder.listener, "listener is required!");
        requireNonNull(builder.channel, "channel is required!");
        requireNonNull(builder.request, "request is required!");
        requireNonNull(builder.opExchange, "opExchange is required!");
        check(builder.channel, ReadableByteChannel::isOpen, "channel must be opened!");

        // 属性赋值
        this.listener = builder.listener;
        this.request = builder.request;
        this.channel = builder.channel;
        this.channelBufferSize = builder.channelBufferSize;
        this.opExchange = builder.opExchange;

    }

    @Override
    public String toString() {
        return "dashscope://audio/asr/recognizer";
    }

    @Override
    protected CompletionStage<Exchange<?>> doConnect() {
        return opExchange.exchange(request, Exchange.Mode.DUPLEX, new ExchangeListenerImpl())
                .thenApply(v -> v);
    }

    /**
     * 交换监听器
     * <p>
     * 在此通过IO调度完成数据传输
     * </p>
     */
    private class ExchangeListenerImpl implements Exchange.Listener<RecognitionRequest, RecognitionResponse> {

        @Override
        public void onOpen(Exchange<RecognitionRequest> exchange) {

            // 启动IO调度线程
            new Thread(() -> scheduleIo(exchange)) {{
                setName(String.format("%s/io-scheduler/%s", RecognitionConnectorImpl.this, exchange.uuid()));
                setDaemon(true);
            }}.start();

        }

        private void scheduleIo(Exchange<?> exchange) {

            log.debug("{}/io-scheduler started! exchange={};", RecognitionConnectorImpl.this, exchange.uuid());

            final ByteBuffer buf = ByteBuffer.allocate(channelBufferSize);
            try {
                while (!exchange.isClosed()) {

                    // 如果数据通道读到了尽头，则向服务端发起主动结束
                    buf.clear();
                    if (channel.read(buf) == -1) {
                        exchange.finishing();
                        break;
                    }

                    // 写入数据到服务端
                    buf.flip();
                    exchange.writeByteBuffer(buf);

                }
            } catch (Throwable ex) {
                log.debug("{}/io-scheduler error! exchange={};", RecognitionConnectorImpl.this, exchange.uuid(), ex);
                exchange.closing(ex);
            } finally {
                log.debug("{}/io-scheduler stopped! exchange={};", RecognitionConnectorImpl.this, exchange.uuid());
            }

        }

        @Override
        public void onData(RecognitionResponse response) {
            listener.onResponse(response);
        }

    }

    public static class Builder extends AbstractExchangeConnector.Builder<RecognitionConnector, RecognitionConnector.Builder> implements RecognitionConnector.Builder {

        private Listener listener;
        private ReadableByteChannel channel;
        private int channelBufferSize = 1024 * 8;
        private RecognitionRequest request;
        private OpExchange<RecognitionRequest, RecognitionResponse> opExchange;

        @Override
        public RecognitionConnector.Builder listener(Listener listener) {
            requireNonNull(listener);
            this.listener = listener;
            return this;
        }

        @Override
        public RecognitionConnector.Builder request(RecognitionRequest request) {
            requireNonNull(request);
            this.request = request;
            return this;
        }

        @Override
        public RecognitionConnector.Builder opExchange(OpExchange<RecognitionRequest, RecognitionResponse> opExchange) {
            requireNonNull(opExchange);
            this.opExchange = opExchange;
            return this;
        }

        @Override
        public RecognitionConnector.Builder channel(ReadableByteChannel channel) {
            requireNonNull(channel);
            this.channel = channel;
            return this;
        }

        @Override
        public RecognitionConnector.Builder channelBufferSize(int bufferSize) {
            check(bufferSize, v -> v > 0, "channelBufferSize must be positive!");
            this.channelBufferSize = bufferSize;
            return this;
        }

        @Override
        public RecognitionConnector build() {
            return new RecognitionConnectorImpl(this);
        }

    }

}
