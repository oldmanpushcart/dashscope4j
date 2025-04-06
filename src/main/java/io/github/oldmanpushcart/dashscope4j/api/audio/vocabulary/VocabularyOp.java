package io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 热词表管理
 * <p><a href="https://help.aliyun.com/zh/model-studio/developer-reference/custom-hot-words">API参考</a></p>
 *
 * @since 3.1.0
 */
public interface VocabularyOp {

    /**
     * 创建热词表
     *
     * @param group       分组
     * @param targetModel 目标模型
     * @param items       热词列表
     * @return 创建通知
     */
    CompletionStage<Vocabulary> create(String group, Model targetModel, Collection<Vocabulary.Item> items);

    /**
     * 查看热词表详情
     *
     * @param vocabularyId 热词表ID
     * @return 查看详情通知
     */
    CompletionStage<Vocabulary> detail(String vocabularyId);

    /**
     * 更新热词表
     *
     * @param vocabularyId 热词表ID
     * @param items        热词列表
     * @return 更新通知
     */
    CompletionStage<?> update(String vocabularyId, Collection<Vocabulary.Item> items);

    /**
     * 删除热词表
     *
     * @param vocabularyId 热词表ID
     * @return 删除通知
     */
    CompletionStage<Boolean> delete(String vocabularyId);

    /**
     * 分页查询热词表
     *
     * @param group     热词表分组
     * @param pageIndex 第几页（从0开始）
     * @param pageSize  页大小
     * @return 分页查询通知
     */
    CompletionStage<List<String>> page(String group, int pageIndex, int pageSize);

    /**
     * 遍历热词表
     *
     * @param group 热词表分组
     * @return 热词表流
     */
    Flowable<Vocabulary> flow(String group);

}
