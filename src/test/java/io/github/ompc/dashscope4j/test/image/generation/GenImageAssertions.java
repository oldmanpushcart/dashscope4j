package io.github.ompc.dashscope4j.test.image.generation;

import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;
import io.github.ompc.dashscope4j.test.RetAssertions;
import io.github.ompc.dashscope4j.test.UsageAssertions;
import org.junit.jupiter.api.Assertions;

import java.net.MalformedURLException;
import java.net.URI;

public class GenImageAssertions {

    public static void assertGenImageRequest(GenImageRequest request) {
        Assertions.assertNotNull(request.model());
        Assertions.assertNotNull(request.input());
        Assertions.assertNotNull(request.option());
    }

    public static void assertGenImageResponse(GenImageResponse response) {
        Assertions.assertNotNull(response.uuid());
        Assertions.assertNotNull(response.ret());
        RetAssertions.assertRet(response.ret());
        Assertions.assertNotNull(response.usage());
        UsageAssertions.assertUsage(response.usage());
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().results());
        response.output().results().forEach(item -> {
            Assertions.assertNotNull(item.ret());
            RetAssertions.assertRet(item.ret());
            if (item.ret().isSuccess()) {
                Assertions.assertNotNull(item.image());
            }
        });
    }

}
