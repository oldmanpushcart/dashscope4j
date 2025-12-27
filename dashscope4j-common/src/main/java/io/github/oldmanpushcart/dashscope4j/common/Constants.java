package io.github.oldmanpushcart.dashscope4j.common;

import java.io.IOException;
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
     * 默认主机
     */
    public static final String DEFAULT_HOST = "dashscope.aliyuncs.com";

    /**
     * 实时 API 路径
     */
    public static final String DEFAULT_REALTIME_PATH = "/api-ws/v1/realtime";

    /**
     * API 路径
     */
    public static final String API_BASE_PATH = "/api/v1";

    /**
     * 兼容模式 API 路径
     */
    public static final String COMPATIBLE_API_BASE_PATH = "/compatible-mode/v1";

    /**
     * WebSocket API 路径
     */
    public static final String API_WS_BASE_PATH = "/api-ws/v1";

}
