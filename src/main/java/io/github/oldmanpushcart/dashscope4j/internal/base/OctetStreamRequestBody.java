package io.github.oldmanpushcart.dashscope4j.internal.base;

import lombok.AllArgsConstructor;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.MT_APPLICATION_OCTET_STREAM;

@AllArgsConstructor
public class OctetStreamRequestBody extends RequestBody {

    private final URI resource;

    @Override
    public MediaType contentType() {
        return MT_APPLICATION_OCTET_STREAM;
    }

    @Override
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        try (final InputStream input = resource.toURL().openStream();
             final Source source = Okio.source(input)) {
            bufferedSink.writeAll(source);
        }
    }

}
