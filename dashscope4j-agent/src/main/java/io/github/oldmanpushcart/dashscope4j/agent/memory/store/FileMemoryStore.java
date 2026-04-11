package io.github.oldmanpushcart.dashscope4j.agent.memory.store;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.TokenizerUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileMemoryStore implements MemoryStore {

    private static final Logger logger = LoggerFactory.getLogger(FileMemoryStore.class);
    private static final String FILE_PREFIX = "memory";
    private static final int READ_BUFFER_SIZE = 8192;
    private static final int SEQUENCE_STEP = 10;

    private final Path directory;
    private final Sequencer sequencer;

    private FileMemoryStore(Builder builder) {
        this.directory = builder.directory.toAbsolutePath().normalize();

        try {
            if (!Files.exists(this.directory)) {
                Files.createDirectories(this.directory);
            }
            if (!Files.isDirectory(this.directory)) {
                throw new IllegalArgumentException("Path is not a directory: " + this.directory);
            }
            if (!Files.isWritable(this.directory)) {
                throw new IllegalArgumentException("Directory is not writable: " + this.directory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create or validate directory: " + this.directory, e);
        }

        this.sequencer = new Sequencer(this.directory, SEQUENCE_STEP);
    }

    @Override
    public Publisher<Fragment> flow(String sessionId, long after) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Path file = getSessionFile(sessionId);
        
        if (!Files.exists(file)) {
            return Flux.empty();
        }

        return Flux.generate(
            () -> {
                try {
                    return new FileCursor(file, after);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create file cursor", e);
                }
            },
            (cursor, sink) -> {
                Fragment fragment;
                try {
                    fragment = cursor.next();
                } catch (IOException e) {
                    sink.error(e);
                    return cursor;
                }
                
                if (fragment == null) {
                    sink.complete();
                } else {
                    sink.next(fragment);
                }
                return cursor;
            },
            cursor -> {
                try {
                    cursor.close();
                } catch (IOException e) {
                    logger.warn("Failed to close file cursor", e);
                }
            }
        );
    }

    @Override
    public CompletionStage<Fragment> upsert(String sessionId, List<Message> messages) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(messages, "messages must not be null");

        try {
            final int tokens = estimateTokens(messages);
            final Path file = getSessionFile(sessionId);

            try (FileChannel channel = FileChannel.open(file,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE)) {

                //noinspection unused
                try (FileLock lock = channel.lock()) {
                    final long fragmentId = sequencer.nextId();
                    final Fragment fragment = new Fragment(fragmentId, sessionId, messages, tokens, Instant.now());

                    final String json = JacksonJsonUtils.toJson(fragment);
                    final ByteBuffer buffer = UTF_8.encode(json + System.lineSeparator());
                    while (buffer.hasRemaining()) {
                        final int written = channel.write(buffer);
                        if (written <= 0) {
                            throw new IOException("Failed to write to file channel");
                        }
                    }
                    channel.force(false);

                    return CompletableFuture.completedFuture(fragment);
                }
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletionStage<Void> remove(long fragmentId) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Remove not supported. Use TTL or GC instead.")
        );
    }

    @Override
    public void close() {
        logger.info("FileMemoryStore closed");
    }

    private Path getSessionFile(String sessionId) {
        return directory.resolve(FILE_PREFIX + "-" + sessionId + ".jsonl");
    }

    private static int estimateTokens(List<Message> messages) {
        return TokenizerUtils.estimateTokens(
                messages.stream().map(Message::text).toArray(String[]::new)
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Path directory;

        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public FileMemoryStore build() {
            Objects.requireNonNull(directory, "directory must not be null");
            return new FileMemoryStore(this);
        }
    }

    /**
     * 序列号生成器（内部类）
     */
    private static class Sequencer {
        private final Path sequenceFile;
        private final int step;
        private final AtomicLong current = new AtomicLong(0);
        private volatile long end = 0;

        Sequencer(Path directory, int step) {
            this.sequenceFile = directory.resolve(".sequence");
            this.step = step;
            initialize();
        }

        private void initialize() {
            try (FileChannel channel = openChannel()) {
                try (FileLock lock = channel.lock()) {
                    final long val = read(channel);
                    current.set(val);
                    end = val + step;
                    write(channel, end);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize sequencer", e);
            }
        }

        long nextId() {
            long id = current.incrementAndGet();
            if (id > end) {
                synchronized (this) {
                    if (current.get() > end) {
                        refill();
                    }
                }
                return current.getAndIncrement();
            }
            return id;
        }

        private void refill() {
            try (FileChannel channel = openChannel()) {
                try (FileLock lock = channel.lock()) {
                    final long val = read(channel);
                    current.set(val);
                    end = val + step;
                    write(channel, end);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to refill sequencer", e);
            }
        }

        private FileChannel openChannel() throws IOException {
            return FileChannel.open(sequenceFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
        }

        private long read(FileChannel channel) throws IOException {
            if (Files.size(sequenceFile) == 0) {
                return 0;
            }
            final ByteBuffer buffer = ByteBuffer.allocate(64);
            channel.read(buffer, 0);
            buffer.flip();
            final String content = UTF_8.decode(buffer).toString().trim();
            return content.isEmpty() ? 0 : Long.parseLong(content);
        }

        private void write(FileChannel channel, long value) throws IOException {
            final ByteBuffer buffer = UTF_8.encode(String.valueOf(value));
            channel.truncate(0);
            while (buffer.hasRemaining()) {
                final int written = channel.write(buffer, 0);
                if (written <= 0) {
                    throw new IOException("Failed to write to sequence file");
                }
            }
            channel.force(false);
        }
    }

    /**
     * 文件游标（支持背压的流式读取）
     */
    private static class FileCursor implements AutoCloseable {
        private final RandomAccessFile raf;
        private final long after;
        private long filePos;
        private final Deque<String> lineBuffer;
        private String leftover = "";  // 缓存跨块的不完整行片段

        FileCursor(Path file, long after) throws IOException {
            this.raf = new RandomAccessFile(file.toFile(), "r");
            this.after = after;
            this.filePos = raf.length();
            this.lineBuffer = new ArrayDeque<>();
        }

        Fragment next() throws IOException {
            while (true) {
                if (!lineBuffer.isEmpty()) {
                    String line = lineBuffer.removeFirst();
                    try {
                        Fragment f = JacksonJsonUtils.toObject(line, Fragment.class);
                        if (f != null && f.fragmentId() < after) {
                            return f;
                        }
                    } catch (Exception e) {
                        logger.warn("Skipping corrupted line\n{}", line, e);
                    }
                    continue;
                }

                if (filePos <= 0) {
                    // 处理最后可能残留的不完整行片段
                    if (!leftover.isEmpty()) {
                        lineBuffer.addLast(leftover);
                        leftover = "";
                        continue;
                    }
                    return null;
                }
                loadNextBlock();
            }
        }

        private void loadNextBlock() throws IOException {
            int readSize = (int) Math.min(READ_BUFFER_SIZE, filePos);
            filePos -= readSize;
            raf.seek(filePos);

            byte[] buffer = new byte[readSize];
            raf.read(buffer);
            String content = new String(buffer, UTF_8);

            // 拼接上一次残留的不完整行片段
            if (!leftover.isEmpty()) {
                content = content + leftover;
                leftover = "";
            }

            // 从后往前扫描，找到每一行的起始位置
            int end = content.length();
            for (int i = content.length() - 1; i >= 0; i--) {
                if (content.charAt(i) == '\n') {
                    // 找到一行：[i+1, end)
                    String line = content.substring(i + 1, end);
                    if (!line.isBlank()) {
                        lineBuffer.addLast(line);  // 添加到尾部，保持倒序
                    }
                    end = i;  // 更新下一行的结束位置
                }
            }

            // 处理剩余部分（文件开头或跨块的片段）
            if (end > 0) {
                // 还有未处理的片段，说明这一行跨越了 block 边界
                leftover = content.substring(0, end);
            } else if (filePos == 0 && end == 0) {
                // 已经到文件开头且没有剩余片段，不需要额外处理
            } else if (filePos == 0) {
                // 已经到文件开头，处理最后一行
                String line = content.substring(0, end);
                if (!line.isBlank()) {
                    lineBuffer.addLast(line);
                }
            }
            // 如果 filePos > 0 且 end == 0，说明当前 block 正好在行边界，leftover 为空
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
}
