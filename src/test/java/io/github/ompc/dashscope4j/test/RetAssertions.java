package io.github.ompc.dashscope4j.test;

import io.github.ompc.dashscope4j.Ret;
import org.junit.jupiter.api.Assertions;

public class RetAssertions {

    public static void assertRet(Ret ret) {
        Assertions.assertNotNull(ret.code());
        Assertions.assertNotNull(ret.message());
    }

}
