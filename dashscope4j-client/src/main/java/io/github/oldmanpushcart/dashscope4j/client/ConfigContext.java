package io.github.oldmanpushcart.dashscope4j.client;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 配置上下文
 *
 * @since 3.2.0
 */
@Data
@Accessors(fluent = true, chain = true)
public class ConfigContext {

    /**
     * 是否自动上传
     */
    private boolean autoUpload = false;

}
