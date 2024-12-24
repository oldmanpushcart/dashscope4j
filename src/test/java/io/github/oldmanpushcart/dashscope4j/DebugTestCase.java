package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final FileMeta meta = client.base().files().upload(new File("./test-data/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        System.out.println(meta);
    }

    @Test
    public void test$debug1() throws JsonProcessingException {
        final String json = JacksonJsonUtils.toJson(Purpose.FILE_EXTRACT);
        System.out.println(json);
    }

}
