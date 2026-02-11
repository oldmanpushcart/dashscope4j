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
    public static final String REALTIME_PATH = "/api-ws/v1/realtime";

    /**
     * 实时 API 路径：推理
     */
    public static final String INFERENCE_PATH = "/api-ws/v1/inference";

    /**
     * 文本生成 API 路径
     */
    public static final String TEXT_GENERATION_PATH = "/api/v1/services/aigc/text-generation/generation";

    /**
     * 多模态生成 API 路径
     */
    public static final String MULTIMODAL_GENERATION_PATH = "/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * 兼容 OpenAI API 路径
     */
    public static final String COMPAT_OPENAI_PATH = "/compatible-mode/v1/chat/completions";

}
