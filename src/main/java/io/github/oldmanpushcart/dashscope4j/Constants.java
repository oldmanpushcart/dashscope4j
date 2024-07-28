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
     */
    public static final String CACHE_NAMESPACE_FOR_STORE = "store";

    /**
     * 缓存命名空间：文件操作
     */
    public static final String CACHE_NAMESPACE_FOR_FILES = "files";

    /**
     * 缓存命名空间：文件操作-文件ID与缓存KEY倒排索引
     * @since 2.1.1
     */
    public static final String CACHE_NAMESPACE_FOR_IDX_CACHE_FILES_FILEID_CACHE_KEY = "idx.files.fileid_cache-key";

    /**
     * 启用
     */
    public static final String ENABLE = "enable";

    /**
     * 禁用
     */
    public static final String DISABLE = "disable";

}
