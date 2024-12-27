package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final FileMeta meta = client.base().files().create(new File("./test-data/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        System.out.println(meta);
    }

    @Test
    public void test$debug1() throws IOException, InterruptedException {
        client.base().files().flow()
                .blockingSubscribe(meta->{
                    System.out.println(meta);
                    Thread.sleep(1000);
                });
    }

}
