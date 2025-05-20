package io.github.oldmanpushcart.dashscope4j.client;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 自动上传上下文
 *
 * @since 3.2.0
 */
@Data
@Accessors(fluent = true, chain = true)
public class AutoUploadContext {

    /**
     * 是否自动上传
     */
    private boolean autoUpload = false;

}
