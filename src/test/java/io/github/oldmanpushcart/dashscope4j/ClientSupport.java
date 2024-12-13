package io.github.oldmanpushcart.dashscope4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ClientSupport implements LoadingEnv {

    protected static DashscopeClient client;

    @BeforeAll
    static void setup() {
        client = DashscopeClient.newBuilder()
                .ak(AK)
                .customizeOkHttpClient(builder -> {
                    builder.pingInterval(30, SECONDS);
                    builder.connectTimeout(Duration.ofSeconds(10));
                    builder.readTimeout(Duration.ofMinutes(3));
                    builder.writeTimeout(Duration.ofMinutes(3));
                })
                .build();
    }

    @AfterAll
    static void cleanup() {
        if (Objects.nonNull(client)) {
            client.shutdown();
        }
    }

}
