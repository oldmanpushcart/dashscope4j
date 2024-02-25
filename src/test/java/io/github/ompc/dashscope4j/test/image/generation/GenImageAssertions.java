package io.github.ompc.dashscope4j.test.image.generation;

import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;
import io.github.ompc.dashscope4j.test.CommonAssertions;
import org.junit.jupiter.api.Assertions;

public class GenImageAssertions {

    public static void assertGenImageRequest(GenImageRequest request) {
        Assertions.assertNotNull(request.model());
        Assertions.assertNotNull(request.input());
        Assertions.assertNotNull(request.option());
    }

    public static void assertGenImageResponse(GenImageResponse response) {
        Assertions.assertNotNull(response.uuid());
        Assertions.assertNotNull(response.ret());
        CommonAssertions.assertRet(response.ret());
        Assertions.assertNotNull(response.usage());
        CommonAssertions.assertUsage(response.usage());
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().results());
        response.output().results().forEach(item -> {
            Assertions.assertNotNull(item.ret());
            CommonAssertions.assertRet(item.ret());
            if (item.ret().isSuccess()) {
                Assertions.assertNotNull(item.image());
            }
        });
    }

}
