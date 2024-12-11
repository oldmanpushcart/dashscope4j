package io.github.oldmanpushcart.dashscope4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

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
