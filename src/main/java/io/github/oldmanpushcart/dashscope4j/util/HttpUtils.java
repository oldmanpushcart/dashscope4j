package io.github.oldmanpushcart.dashscope4j.util;

import okhttp3.*;
import okio.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
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
        final CompletableFuture<String> future = new CompletableFuture<>();
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new IOException(String.format("Unexpected code: %s", response.code())));
                    return;
                }
                final ResponseBody responseBody = response.body();
                if (null == responseBody) {
                    future.complete("");
                    return;
                }
                final ResponseBody progressResponseBody = new ProgressResponseBody(responseBody, listener);
                try {
                    future.complete(progressResponseBody.string());
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
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
