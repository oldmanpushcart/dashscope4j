package io.github.oldmanpushcart.dashscope4j.client.util;

import io.github.oldmanpushcart.dashscope4j.client.internal.CompletableFutureCallback;
import jakarta.validation.constraints.NotNull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.CompletionStage;

/**
 * HttpUtils
 *
 * @since 3.1.0
 */
public class HttpUtils {

    /**
     * 获取远程文件内容
     *
     * @param http   http客户端
     * @param remote 远程文件地址
     * @return 远程文件内容操作
     */
    public static CompletionStage<String> fetchAsString(OkHttpClient http, URI remote) {
        return fetchAsString(http, remote, (bytesRead, contentLength, done) -> {
        });
    }

    /**
     * 获取远程文件内容
     *
     * @param http     http客户端
     * @param remote   远程文件地址
     * @param listener 进度监听器
     * @return 远程文件内容操作
     */
    public static CompletionStage<String> fetchAsString(OkHttpClient http, URI remote, ProgressListener listener) {
        final Request request = new Request.Builder()
                .url(remote.toString())
                .get()
                .build();

        final CompletableFutureCallback<String> callback = new CompletableFutureCallback<>((call, response) -> {

            if (!response.isSuccessful()) {
                throw new IOException(String.format("Unexpected code: %s", response.code()));
            }

            final ResponseBody responseBody = response.body();
            if (null == responseBody) {
                return null;
            }

            return new ProgressResponseBody(responseBody, listener)
                    .string();
        });

        http.newCall(request).enqueue(callback);
        return callback;
    }

    /**
     * 从远程文件下载到临时文件
     *
     * @param http   http客户端
     * @param remote 文件地址
     * @return 下载操作
     * @since 3.1.0
     */
    public static CompletionStage<File> fetchAsTempFile(OkHttpClient http, URI remote) {
        return fetchAsTempFile(http, remote, (bytesRead, contentLength, done) -> {
        });
    }

    /**
     * 从远程文件下载到临时文件
     *
     * @param http     http客户端
     * @param remote   文件地址
     * @param listener 进度监听器
     * @return 下载操作
     * @since 3.1.0
     */
    public static CompletionStage<File> fetchAsTempFile(OkHttpClient http, URI remote, ProgressListener listener) {
        final Request request = new Request.Builder()
                .url(remote.toString())
                .get()
                .build();

        final CompletableFutureCallback<File> callback = new CompletableFutureCallback<>((call, response) -> {

            if (!response.isSuccessful()) {
                throw new IOException(String.format("Unexpected code: %s", response.code()));
            }

            final byte[] buffer = new byte[1024];
            final File tempFile = File.createTempFile("dashscope4j", ".download.tmp");
            final ResponseBody progressResponseBody = new ProgressResponseBody(response.body(), listener);
            try (final InputStream input = progressResponseBody.byteStream();
                 final OutputStream output = Files.newOutputStream(tempFile.toPath())) {
                int bytesRead;

                // 逐步读取并写入文件
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

            }

            return tempFile;

        });

        http.newCall(request).enqueue(callback);
        return callback;
    }

    /**
     * 实现带有进度监听的ResponseBody
     */
    private static class ProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @NotNull
        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.onProgress(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

}
