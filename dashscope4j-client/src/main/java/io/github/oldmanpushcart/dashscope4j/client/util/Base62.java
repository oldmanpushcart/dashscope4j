package io.github.oldmanpushcart.dashscope4j.client.util;

import java.math.BigInteger;
import java.util.UUID;

public class Base62 {

    private static final char[] BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int BASE = 62;

    /**
     * 将 UUID 压缩为 22 字符的 Base62 字符串。
     *
     * @param uuid 非 null 的 UUID
     * @return 长度 <= 22 的 Base62 字符串（无前导零）
     */
    public static String encode(UUID uuid) {
        // 获取 UUID 的 128 位值（带符号，需转为无符号）
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // 手动填充字节数组（big-endian）
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (56 - i * 8));
        }
        for (int i = 0; i < 8; i++) {
            bytes[i + 8] = (byte) (lsb >>> (56 - i * 8));
        }

        // 转为 BigInteger（无符号）
        BigInteger value = new BigInteger(1, bytes); // 1 = 正数

        // Base62 编码
        StringBuilder sb = new StringBuilder();
        if (value.equals(BigInteger.ZERO)) {
            return "0";
        }
        while (value.compareTo(BigInteger.ZERO) > 0) {
            int remainder = value.mod(BigInteger.valueOf(BASE)).intValue();
            sb.append(BASE62_CHARS[remainder]);
            value = value.divide(BigInteger.valueOf(BASE));
        }

        // 反转（因为是从低位到高位）
        return sb.reverse().toString();
    }

    /**
     * 从 Base62 字符串还原为 UUID。
     *
     * @param base62 非 null、非空的 Base62 字符串
     * @return 还原的 UUID
     * @throws IllegalArgumentException 如果输入无效
     */
    public static UUID decode(String base62) {
        final byte[] bytes = getBytes(base62);
        final byte[] padded = new byte[16];
        if (bytes.length > 16) {
            throw new IllegalArgumentException("Value too large for UUID");
        }
        System.arraycopy(bytes, 0, padded, 16 - bytes.length, bytes.length);

        // 从字节数组重建 UUID
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (padded[i] & 0xFF);
        }
        for (int i = 0; i < 8; i++) {
            lsb = (lsb << 8) | (padded[i + 8] & 0xFF);
        }
        return new UUID(msb, lsb);
    }

    private static byte[] getBytes(String base62) {
        if (!CommonUtils.isEmptyString(base62)) {
            throw new IllegalArgumentException("Base62 string must not be null or empty");
        }

        BigInteger value = BigInteger.ZERO;
        for (char c : base62.toCharArray()) {
            int digit = getBase62Index(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            value = value.multiply(BigInteger.valueOf(BASE)).add(BigInteger.valueOf(digit));
        }

        // 转回 16 字节数组（补充前导零到 16 字节）
        return value.toByteArray();
    }

    private static int getBase62Index(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        if (c >= 'a' && c <= 'z') return c - 'a' + 36;
        return -1;
    }

}
