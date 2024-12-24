package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import org.junit.jupiter.api.Test;

import java.io.File;
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
    public void test$debug1() throws JsonProcessingException {
        final boolean deleted = client.base().files().delete("test-001")
                .toCompletableFuture()
                .join();
        System.out.println(deleted);

//        client.base().files().list()
//                .toCompletableFuture()
//                .join();
    }

}
