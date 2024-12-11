package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class SpeechSynthesisTestCase extends ClientSupport {

    @Test
    public void test$synthesis$success() {

        final String[] strings = new String[]{
                "白日依山尽",
                "黄河入海流",
                "欲穷千里目",
                "更上一层楼"
        };

        final SpeechSynthesisRequest request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .build();

        final CompletableFuture<File> completed = new CompletableFuture<>();
        final Exchange<SpeechSynthesisRequest> exchange = client.audio().synthesis()
                .exchange(request, Exchange.Mode.DUPLEX, new Exchange.Listener<SpeechSynthesisRequest, SpeechSynthesisResponse>() {

                    private final File file = new File("./output.mp3");
                    private volatile Exchange<?> exchange;
                    private volatile FileChannel channel;

                    @Override
                    public void onOpen(Exchange<SpeechSynthesisRequest> exchange) {
                        try {
                            this.exchange = exchange;
                            this.channel = FileChannel.open(file.toPath(), CREATE, WRITE);
                        } catch (IOException ex) {
                            exchange.abort();
                            onError(ex);
                        }
                    }

                    @Override
                    public void onByteBuffer(ByteBuffer buf) {
                        try {
                            while (buf.hasRemaining()) {
                                if (channel.write(buf) == -1) {
                                    throw new EOFException();
                                }
                            }
                        } catch (IOException ex) {
                            exchange.closing(1006, ex.getMessage());
                            onError(ex);
                        }
                    }

                    @Override
                    public void onCompleted() {
                        try {
                            channel.close();
                            completed.complete(file);
                        } catch (IOException ex) {
                            onError(ex);
                        }
                    }

                    @Override
                    public void onError(Throwable ex) {
                        try {
                            if (null != channel) {
                                channel.close();
                            }
                        } catch (IOException e) {
                            // ignore...
                        }
                        completed.completeExceptionally(ex);
                    }

                })
                .toCompletableFuture()
                .join();

        for (final String string : strings) {
            exchange.write(SpeechSynthesisRequest.newBuilder(request)
                    .text(string)
                    .build());
        }
        exchange.finishing();
        completed.join();

    }

}
