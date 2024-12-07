package io.github.oldmanpushcart.dashscope4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Objects;

public class ClientSupport implements LoadingEnv {

    protected static DashscopeClient client;

    @BeforeAll
    static void setup() {
        client = DashscopeClient.newBuilder()
                .ak(AK)
                .build();
    }

    @AfterAll
    static void cleanup() {
        if (Objects.nonNull(client)) {
            client.shutdown();
        }
    }

}
