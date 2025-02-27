package io.github.oldmanpushcart.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.util.ProgressListener;

import java.net.URI;
import java.util.concurrent.CompletionStage;

/**
 * 存储操作
 *
 * @link <a href="https://help.aliyun.com/zh/dashscope/developer-reference/guidance-of-temporary-storage-space">文件存储API</a>
 */
public interface StoreOp {

    /**
     * 上传资源到存储
     *
     * @param resource 资源URI
     * @param model    模型
     * @return 存储URI
     */
    CompletionStage<URI> upload(URI resource, Model model);

    /**
     * 上传资源到存储
     *
     * @param resource 资源URI
     * @param model    模型
     * @param listener 进度监听器
     * @return 存储URI
     * @since 3.1.0
     */
    CompletionStage<URI> upload(URI resource, Model model, ProgressListener listener);

}
