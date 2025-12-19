package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.UUID;

public class UUIDUtils {

    public static String genUUID32() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String genUUID22() {
        return Base62.encode(UUID.randomUUID());
    }

    public static void main(String[] args) {
        System.out.println(genUUID22().length());
        System.out.println(genUUID32().length());
    }

}
