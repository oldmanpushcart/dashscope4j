package io.github.oldmanpushcart.dashscope4j;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {

        client.audio().vocabulary().flow("test")
                .blockingSubscribe(vocabulary -> {
                    System.out.println(vocabulary);
                });

    }

}
