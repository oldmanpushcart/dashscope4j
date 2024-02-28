package io.github.oldmanpushcart.dashscope4j;

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

    /**
     * 日志
     */
    public static final String LOGGER_NAME = "dashscope4j";

    /**
     * 版本
     */
    public static final String VERSION = properties.getProperty("version");

}
