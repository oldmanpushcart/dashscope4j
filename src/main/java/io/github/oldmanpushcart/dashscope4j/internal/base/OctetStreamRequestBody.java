package io.github.oldmanpushcart.dashscope4j.internal.base;

import io.github.oldmanpushcart.dashscope4j.util.ProgressListener;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.MT_APPLICATION_OCTET_STREAM;

@AllArgsConstructor
public class OctetStreamRequestBody extends RequestBody {

    private final URI resource;
    private final ProgressListener progressListener;

    @Override
    public MediaType contentType() {
        return MT_APPLICATION_OCTET_STREAM;
    }

    @Override
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        try (final InputStream input = resource.toURL().openStream();
             final Source source = Okio.source(input)) {

            final okio.Buffer buffer = new okio.Buffer();
            final long totalBytes = input.available();
            long bytesRead, bytesWritten = 0L;

            while ((bytesRead = source.read(buffer, 8192)) != -1) {
                bufferedSink.write(buffer, bytesRead);
                bytesWritten += bytesRead;
                progressListener.onProgress(bytesWritten, totalBytes, bytesWritten == totalBytes);
            }

        }
    }

}
