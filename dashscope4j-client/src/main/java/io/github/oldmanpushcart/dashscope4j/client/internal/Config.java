package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.time.Duration;

public record Config(
        String host,
        String ak,
        Duration httpConnectTimeout,
        Duration httpTimeout
) {

    private Config(Builder builder) {
        this(
                builder.host,
                builder.ak,
                builder.httpConnectTimeout,
                builder.httpTimeout
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<Config, Builder> {

        private String host;
        private String ak;
        private Duration httpConnectTimeout;
        private Duration httpTimeout;

        public Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        public Builder httpConnectTimeout(Duration httpConnectTimeout) {
            this.httpConnectTimeout = httpConnectTimeout;
            return this;
        }

        public Builder httpTimeout(Duration httpTimeout) {
            this.httpTimeout = httpTimeout;
            return this;
        }

        @Override
        public Config build() {
            return new Config(this);
        }

    }

}
