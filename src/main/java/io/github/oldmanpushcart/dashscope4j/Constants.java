package io.github.oldmanpushcart.dashscope4j;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/**
 * 常量
 */
public class Constants {

    private final static Properties properties = new Properties();

    static {
        try {
            properties.load(Constants.class.getResourceAsStream("/dashscope4j-meta.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Constants() {

    }

    /**
     * 版本
     */
    public static final String VERSION = properties.getProperty("version");

    /**
     * WEBSOCKET服务器地址
     */
    public static final URI WSS_REMOTE = URI.create("wss://dashscope.aliyuncs.com/api-ws/v1/inference/");

}
