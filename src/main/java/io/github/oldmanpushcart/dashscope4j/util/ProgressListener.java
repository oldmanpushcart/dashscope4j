package io.github.oldmanpushcart.dashscope4j.util;

/**
 * 进度监听器
 *
 * @since 3.1.0
 */
public interface ProgressListener {

    /**
     * 空实现
     */
    ProgressListener empty = (bytesWritten, contentLength, done) -> {
    };

    /**
     * 进度回调
     *
     * @param bytesWritten  已写入字节数
     * @param contentLength 总字节数
     * @param done          是否完成
     */
    void onProgress(long bytesWritten, long contentLength, boolean done);

}
