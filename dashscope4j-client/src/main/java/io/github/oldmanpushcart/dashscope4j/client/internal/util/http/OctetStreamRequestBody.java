package io.github.oldmanpushcart.dashscope4j.client.internal.util.http;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.MT_APPLICATION_OCTET_STREAM;

public class OctetStreamRequestBody extends RequestBody {

    private final URI resource;

    public OctetStreamRequestBody(URI resource) {
        this.resource = resource;
    }

    @Override
    public @Nullable MediaType contentType() {
        return MT_APPLICATION_OCTET_STREAM;
    }

    @Override
    public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
        try (final InputStream input = resource.toURL().openStream();
             final Source source = Okio.source(input)) {

            final okio.Buffer buffer = new okio.Buffer();
            long bytesRead;

            while ((bytesRead = source.read(buffer, 8192)) != -1) {
                bufferedSink.write(buffer, bytesRead);
            }

        }
    }

}
