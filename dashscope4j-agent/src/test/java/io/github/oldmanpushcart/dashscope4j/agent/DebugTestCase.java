package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeGenImageByImageFunction;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {
        DashscopeGenImageByImageFunction.newBuilder()
                .build();
    }

}
