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

    /**
     * 缓存命名空间：临时空间
     *
     * @since 1.4.2
     */
    public static final String CACHE_NAMESPACE_FOR_UPLOAD = "upload";

    /**
     * 缓存命名空间：文件操作
     *
     * @since 1.4.2
     */
    public static final String CACHE_NAMESPACE_FOR_FILES = "files";

}
