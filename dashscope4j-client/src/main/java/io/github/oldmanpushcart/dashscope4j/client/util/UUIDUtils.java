package io.github.oldmanpushcart.dashscope4j.client.util;

import java.util.UUID;

public class UUIDUtils {

    public static String genUUID32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String genUUID22() {
        return Base62.encode(UUID.randomUUID());
    }

}
