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

/**
 * 基于文件的内存存储实现
 * <p>
 * 使用 JSONL 格式存储会话片段，支持并发读写和流式读取。
 * </p>
 */
public class FileMemoryStore implements MemoryStore {

    private static final Logger logger = LoggerFactory.getLogger(FileMemoryStore.class);
    private static final String FILE_PREFIX = "memory";
    private static final int READ_BUFFER_SIZE = 8192;
    private static final int SEQUENCE_STEP = 10;

    private final Path directory;
    private final Sequencer sequencer;  // 全局 ID 生成器

    /**
     * 构造函数（通过 Builder 调用）
     */
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

    /**
     * 流式读取会话片段（倒序，最新的在前）
     */
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

    /**
     * 追加会话片段（线程安全）
     */
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

    /**
     * 不支持物理删除（使用 TTL 或 GC 代替）
     */
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

    /**
     * 获取会话文件路径
     */
    private Path getSessionFile(String sessionId) {
        return directory.resolve(FILE_PREFIX + "-" + sessionId + ".jsonl");
    }

    /**
     * 估算消息 Token 数量
     */
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
     * 全局序列号生成器
     * <p>
     * 使用文件锁保证多进程并发安全，AtomicLong 优化快速路径。
     * </p>
     */
    private static class Sequencer {
        private final Path sequenceFile;
        private final int step;
        private final AtomicLong current = new AtomicLong(0);
        private volatile long end = 0;

        /**
         * 初始化：读取当前值并预分配一批 ID
         */
        Sequencer(Path directory, int step) {
            this.sequenceFile = directory.resolve(".sequence");
            this.step = step;
            initialize();
        }

        /**
         * 初始化序列号文件
         */
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

        /**
         * 获取下一个 ID（无锁快速路径 + 文件锁慢速路径）
         */
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

        /**
         *  refill：从文件重新加载并预分配下一批 ID
         */
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

        /**
         * 读取序列号文件中的当前值
         */
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

        /**
         * 写入新值到序列号文件
         */
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
     * 文件游标：从后往前流式读取 JSONL 文件
     * <p>
     * 支持背压和跨块不完整行处理。
     * </p>
     */
    private static class FileCursor implements AutoCloseable {
        private final RandomAccessFile raf;
        private final long after;
        private long filePos;
        private final Deque<String> lineBuffer;
        private String leftover = "";  // 缓存跨块的不完整行片段

        /**
         * 初始化游标：定位到文件末尾
         */
        FileCursor(Path file, long after) throws IOException {
            this.raf = new RandomAccessFile(file.toFile(), "r");
            this.after = after;
            this.filePos = raf.length();
            this.lineBuffer = new ArrayDeque<>();
        }

        /**
         * 获取下一个片段（倒序）
         */
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
                        logger.warn("Skipping corrupted line", e);
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

        /**
         * 加载下一个数据块（从后往前扫描）
         */
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
            }
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
}
