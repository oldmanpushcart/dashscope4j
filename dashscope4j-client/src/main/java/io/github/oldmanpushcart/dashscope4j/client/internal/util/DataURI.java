package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

public record DataURI(

        Encoding encoding,
        String mime,
        Supplier<String> encode

) {

    public enum Encoding {
        BASE64
    }

    public URI toURI() {
        final var _mime = mime == null ? "" : mime;
        final var _encoding = (encoding == null ? Encoding.BASE64 : encoding)
                .name()
                .toLowerCase();
        final var _encode = encode.get();
        return URI.create("data:%s;%s,%s".formatted(
                _mime,
                _encoding,
                _encode
        ));
    }

    public static class Builder implements Buildable<DataURI, Builder> {

        private Encoding encoding;
        private String mime;
        private Supplier<String> encode;

        public Builder encoding(Encoding encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder mime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder encode(Supplier<String> encode) {
            this.encode = encode;
            return this;
        }

        public Builder encode(File file) throws IOException {

        }

        @Override
        public DataURI build() {
            return null;
        }
    }

}
