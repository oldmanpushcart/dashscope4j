package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final String json = "";
        final String ret = JacksonJsonUtils.toObject(json, String.class);

    }

}
