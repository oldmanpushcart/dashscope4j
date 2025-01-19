package io.github.oldmanpushcart.dashscope4j.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.reactivex.rxjava3.core.Flowable;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 音色操作
 * <p><a href="https://help.aliyun.com/zh/model-studio/developer-reference/cosyvoice-clone-api">API参考</a></p>
 *
 * @since 3.1.0
 */
public interface VoiceOp {

    /**
     * 创建音色
     *
     * @param group       分组
     * @param targetModel 目标模型
     * @param resource    声纹资源
     * @return 创建通知
     */
    CompletionStage<Voice> create(String group, Model targetModel, URI resource);

    /**
     * 获取音色详情
     *
     * @param voiceId 音色ID
     * @return 获取详情通知
     */
    CompletionStage<Voice> detail(String voiceId);

    /**
     * 更新音色
     *
     * @param voiceId  音色ID
     * @param resource 音色资源
     * @return 更新通知
     */
    CompletionStage<?> update(String voiceId, URI resource);

    /**
     * 删除音色
     *
     * @param voiceId 音色ID
     * @return 删除通知
     */
    CompletionStage<Boolean> delete(String voiceId);

    /**
     * 分页查询音色
     *
     * @param group     分组
     * @param pageIndex 第几页（从0开始）
     * @param pageSize  页大小
     * @return 分页查询通知
     */
    CompletionStage<List<String>> page(String group, int pageIndex, int pageSize);

    /**
     * 获取音色流
     *
     * @param group 分组
     * @return 音色流
     */
    Flowable<Voice> flow(String group);

}
