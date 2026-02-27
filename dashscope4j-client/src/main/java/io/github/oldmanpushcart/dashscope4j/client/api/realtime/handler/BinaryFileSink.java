package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 二进制数据写入文件
 *
 * @param <T> 输入类型
 * @param <R> 输出类型
 */
public class BinaryFileSink<T, R> implements Realtime.Handler<T, R> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File file;
    private final boolean force;
    private volatile FileChannel channel;

    /**
     * @param outputFile 输出文件
     */
    public BinaryFileSink(File outputFile) {
        this(outputFile, false);
    }

    /**
     * @param outputFile 输出文件
     * @param force      是否强制写入
     */
    public BinaryFileSink(File outputFile, boolean force) {
        this.file = outputFile;
        this.force = force;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://realtime/handler/binary-file-sink";
    }

    @Override
    public void onOpen(Realtime.Emitter<T> emitter) {
        try {
            this.channel = FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            this.channel.force(force);
            logger.debug("{} file opened file: {}", this, file.getAbsoluteFile());
        } catch (IOException ioEx) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException("Failed to open file: %s".formatted(file), ioEx);
        }
    }

    @Override
    public CompletionStage<Void> onData(R output) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        try {
            while (buffer.hasRemaining()) {
                //noinspection ResultOfMethodCallIgnored
                channel.write(buffer);
            }
            return CompletableFuture.completedFuture(null);
        } catch (IOException ioEx) {
            logger.warn("{} failed to write binary data to file: {}", this, file.getAbsoluteFile(), ioEx);
            return CompletableFuture.failedStage(ioEx);
        }

    }

    @Override
    public void onClosed(Throwable ex) {
        IOUtils.closeQuietly(channel);
        if (null == ex) {
            logger.debug("{} file closed. file={}", this, file);
        } else {
            logger.warn("{} file closed abnormally. file={}", this, file, ex);
        }
    }

}
