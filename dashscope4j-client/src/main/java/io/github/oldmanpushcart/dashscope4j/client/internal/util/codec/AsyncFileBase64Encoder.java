package io.github.oldmanpushcart.dashscope4j.client.internal.util.codec;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.client.util.IOUtils.closeQuietly;

/**
 * 异步文件内容 Base64 编码工具类。
 */
public class AsyncFileBase64Encoder {

    /**
     * 字节缓存块大小
     * <p>
     * 因为 BASE64 编码的基本单元是 3 字节 → 4 字符，
     * 所以读取块大小必须是 3 的整数倍，以避免跨块边界导致的编码错位。
     * <p>
     * 使用 3 × 2048 = 6144 字节（6KB）作为默认值：
     * - 足够小：减少内存占用
     * - 足够大：保证 I/O 效率
     * - 是 3 的倍数：简化边界处理
     */
    private static final int DEFAULT_CHUNK_SIZE = 3 * 2048;

    /**
     * 最大支持的文件大小（约 1.5 GB）
     * <p>
     * 计算依据：
     * - Java String 最大长度 ≈ Integer.MAX_VALUE - 安全裕度
     * - Base64 编码膨胀比为 4/3
     * - 因此最大原始文件大小 = (Integer.MAX_VALUE - 100) × 3 / 4
     * <p>
     * 此限制确保 StringBuilder 不会因容量过大而触发
     * {@code OutOfMemoryError: Requested array size exceeds VM limit}。
     */
    private static final long MAX_FILE_SIZE = (Integer.MAX_VALUE - 100L) * 3L / 4L;

    /**
     * 异步将文件内容编码为 Base64 字符串。
     *
     * @param path     待编码的文件路径（必须存在且可读）
     * @param executor 用于异步回调的线程池；若为 null，则使用系统默认线程
     * @return 异步结果，成功时返回完整 Base64 字符串
     */
    public static CompletionStage<String> encode(Path path, Executor executor) {
        final var result = new CompletableFuture<String>();

        // 使用 AtomicReference 捕获 channel 引用，确保在异常或取消时也能关闭
        final var channelRef = new AtomicReference<AsynchronousFileChannel>();

        try {
            // 打开异步文件通道（只读）
            final var channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
            channelRef.set(channel);

            // 获取文件总大小
            final var total = channel.size();

            // 检查文件是否过大（防止后续 StringBuilder 分配失败）
            if (total > MAX_FILE_SIZE) {
                closeQuietly(channel);
                result.completeExceptionally(
                        new IllegalArgumentException("File too large (> ~1.5GB), max allowed: " + MAX_FILE_SIZE + " bytes"));
                return result;
            }

            // 预分配 StringBuilder 容量，避免频繁扩容
            // 公式：Base64 长度 ≈ fileSize * 4 / 3，+10 为安全缓冲（覆盖 padding 和舍入误差）
            final var stringBuf = new StringBuilder((int) (total * 4 / 3 + 10));

            // 分配堆外直接缓冲区，提升 I/O 性能（避免内核 ↔ 堆内存拷贝）
            final var buffer = ByteBuffer.allocateDirect(DEFAULT_CHUNK_SIZE);

            // 启动递归异步读取与编码
            readAndEncode(channel, buffer, stringBuf, 0, total, executor)
                    .whenComplete((v, ex) -> {
                        // 无论成功或失败，确保关闭文件通道
                        closeQuietly(channel);
                        if (ex != null) {
                            result.completeExceptionally(ex);
                        } else {
                            result.complete(stringBuf.toString());
                        }
                    });

        } catch (Throwable t) {
            // 捕获 open() 或 size() 抛出的任何异常（如 FileNotFoundException）
            result.completeExceptionally(t);
        }

        // 额外保障：当 future 完成（包括被取消）时，尝试关闭 channel
        // 注意：channel 可能已被关闭，closeQuietly 是幂等的
        result.whenComplete((v, ex) -> {
            final var channel = channelRef.get();
            if (channel != null) {
                closeQuietly(channel);
            }
        });

        return result;
    }

    /**
     * 递归异步读取文件块并追加 Base64 编码结果到输出缓冲区。
     *
     * @param channel  异步文件通道
     * @param buffer   直接内存缓冲区（复用）
     * @param output   结果字符串构建器
     * @param position 当前读取位置（字节偏移）
     * @param total    文件总大小（字节）
     * @param executor 异步回调执行器
     * @return CompletableFuture<Void> 表示本次及后续读取完成
     */
    private static CompletableFuture<Void> readAndEncode(
            AsynchronousFileChannel channel,
            ByteBuffer buffer,
            StringBuilder output,
            long position,
            long total,
            Executor executor
    ) {
        // 递归终止条件：已读取完整个文件
        if (position >= total) {
            return CompletableFuture.completedFuture(null);
        }

        // 计算本次需读取的字节数（不超过剩余量）
        final var remaining = (int) (total - position);
        final var toRead = Math.min(buffer.capacity(), remaining);
        buffer.clear().limit(toRead); // 重置缓冲区并设置 limit

        final var readF = new CompletableFuture<Void>();

        // 异步读取文件块
        channel.read(buffer, position, null, new CompletionHandler<Integer, Void>() {

            @Override
            public void completed(Integer bytesRead, Void attachment) {
                // 处理 EOF 或读取异常（bytesRead <= 0）
                if (bytesRead <= 0) {
                    readF.complete(null);
                    return;
                }

                // 准备从 buffer 中读取数据
                buffer.flip();

                /*
                 * 关键：确定本次可安全编码的字节数
                 * - 如果是最后一块（position + bytesRead == total）：全部编码（含 padding）
                 * - 否则：仅编码 3 的整数倍部分，确保不破坏 Base64 单元边界
                 *   （虽然 DEFAULT_CHUNK_SIZE 是 3 的倍数，但防御性处理 read() 返回不足的情况）
                 */
                final int processableBytes;
                if (position + bytesRead == total) {
                    processableBytes = bytesRead; // 最后一块，全部处理
                } else {
                    processableBytes = (bytesRead / 3) * 3; // 截断到最近的 3 的倍数
                }

                // 执行 Base64 编码并追加到结果
                if (processableBytes > 0) {
                    final var bytes = new byte[processableBytes];
                    buffer.get(bytes, 0, processableBytes); // 从 buffer 复制到堆内存
                    output.append(Base64.getEncoder().encodeToString(bytes));
                }

                // 递归处理下一块
                final var nextPos = position + bytesRead;
                readAndEncode(channel, buffer, output, nextPos, total, executor)
                        .whenComplete((v, ex) -> {
                            if (ex != null) {
                                readF.completeExceptionally(ex);
                            } else {
                                readF.complete(null);
                            }
                        });
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                // I/O 错误（如磁盘故障、权限问题）
                readF.completeExceptionally(exc);
            }
        });

        return readF;
    }


    // ———————— 便捷方法 ————————
    public static CompletionStage<String> encode(Path path) {
        return encode(path, null);
    }

}
