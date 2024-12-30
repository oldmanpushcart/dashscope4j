package io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 热词表管理
 *
 * @since 3.1.0
 */
public interface VocabularyOp {

    /**
     * 创建热词表
     *
     * @param group  分组
     * @param target 目标模型
     * @param items  热词列表
     * @return 创建热词表操作
     */
    CompletionStage<Vocabulary> create(String group, Model target, Collection<Vocabulary.Item> items);

    /**
     * 查看热词表详情
     *
     * @param vocabularyId 热词表ID
     * @return 查看详情表操作
     */
    CompletionStage<Vocabulary> detail(String vocabularyId);

    /**
     * 更新热词表
     *
     * @param vocabularyId 热词表ID
     * @param items        热词列表
     * @return 更新热词表操作
     */
    CompletionStage<?> update(String vocabularyId, Collection<Vocabulary.Item> items);

    /**
     * 删除热词表
     *
     * @param vocabularyId 热词表ID
     * @return 删除热词表操作
     */
    CompletionStage<Boolean> delete(String vocabularyId);

    /**
     * 分页查询热词表
     *
     * @param group 热词表分组
     * @param index 第几页（从0开始）
     * @param size  页大小
     * @return 分页查询操作
     */
    CompletionStage<List<String>> page(String group, int index, int size);

    /**
     * 遍历热词表
     *
     * @param group 热词表分组
     * @return 遍历操作
     */
    Flowable<Vocabulary> flow(String group);

}
