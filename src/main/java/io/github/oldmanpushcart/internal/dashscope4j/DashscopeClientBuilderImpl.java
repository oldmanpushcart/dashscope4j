package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Objects;

@Getter
@Accessors(fluent = true)
public class DashscopeClientBuilderImpl implements DashscopeClient.Builder {

    private String ak;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration writeTimeout = Duration.ofSeconds(10);

    @Override
    public DashscopeClient.Builder ak(String ak) {
        this.ak = ak;
        return this;
    }

    @Override
    public DashscopeClient.Builder connectTimeout(Duration duration) {
        this.connectTimeout = Objects.requireNonNull(duration);
        return this;
    }

    @Override
    public DashscopeClient.Builder readTimeout(Duration duration) {
        this.readTimeout = Objects.requireNonNull(duration);
        return this;
    }

    @Override
    public DashscopeClient.Builder writeTimeout(Duration duration) {
        this.writeTimeout = Objects.requireNonNull(duration);
        return this;
    }

    @Override
    public DashscopeClient build() {
        return new DashscopeClientImpl(this);
    }

}
