package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.OkHttpClient;

import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public class DashscopeClientBuilderImpl implements DashscopeClient.Builder {

    private String ak;
    private final OkHttpClient.Builder okHttpClientBuilder
            = new OkHttpClient.Builder();

    @Override
    public DashscopeClient.Builder ak(String ak) {
        this.ak = ak;
        return this;
    }

    @Override
    public DashscopeClient.Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer) {
        consumer.accept(okHttpClientBuilder);
        return this;
    }

    @Override
    public DashscopeClient build() {
        return new DashscopeClientImpl(this, okHttpClientBuilder.build());
    }

}
