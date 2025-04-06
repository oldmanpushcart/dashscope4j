package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.Vocabulary;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.VocabularyOp;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.failedStage;
import static io.github.oldmanpushcart.dashscope4j.internal.util.DashscopeApiUtils.isCauseByResourceNotExisted;
import static java.util.Objects.isNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class VocabularyOpImpl implements VocabularyOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<Vocabulary> create(String group, Model targetModel, Collection<Vocabulary.Item> items) {

        final VocabularyCreateRequest request = VocabularyCreateRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .group(group)
                .targetModel(targetModel)
                .items(items)
                .build();

        return apiOp.executeAsync(request)
                .thenCompose(response ->
                        implDetail(response.output().vocabularyId()));

    }

    @Override
    public CompletionStage<Vocabulary> detail(String vocabularyId) {
        return implDetail(vocabularyId)

                /*
                 * 如果查询错误的原因是热词表不存在，则返回null
                 */
                .<CompletionStage<Vocabulary>>handle((v, ex) ->
                        isNull(ex) || isCauseByResourceNotExisted(ex)
                                ? completedFuture(v)
                                : failedStage(ex))
                .thenCompose(v -> v);

    }

    private CompletionStage<Vocabulary> implDetail(String vocabularyId) {
        final VocabularyDetailRequest request = VocabularyDetailRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .vocabularyId(vocabularyId)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response ->
                        new Vocabulary(
                                vocabularyId,
                                response.output().target(),
                                response.output().createdAt(),
                                response.output().updatedAt(),
                                response.output().items()
                        ));
    }

    @Override
    public CompletionStage<?> update(String vocabularyId, Collection<Vocabulary.Item> items) {
        final VocabularyUpdateRequest request = VocabularyUpdateRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .vocabularyId(vocabularyId)
                .items(items)
                .build();
        return apiOp.executeAsync(request);
    }


    @Override
    public CompletionStage<Boolean> delete(String vocabularyId) {
        final VocabularyDeleteRequest request = VocabularyDeleteRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .vocabularyId(vocabularyId)
                .build();
        return apiOp.executeAsync(request)

                /*
                 * 如果删除异常的原因是数据不存在，则幂等返回false
                 */
                .<CompletionStage<Boolean>>handle((unused, ex) -> {
                    if(isNull(ex)) {
                        return completedFuture(true);
                    }
                    return isCauseByResourceNotExisted(ex)
                            ? completedFuture(false)
                            : failedStage(ex);
                })
                .thenCompose(v -> v);

    }

    @Override
    public CompletionStage<List<String>> page(String group, int pageIndex, int pageSize) {
        final VocabularyPageQueryRequest request = VocabularyPageQueryRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .group(group)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response ->
                        response.output().items().stream()
                                .map(VocabularyPageQueryResponse.Item::vocabularyId)
                                .collect(Collectors.toList()))
                .thenApply(Collections::unmodifiableList);
    }

    @Override
    public Flowable<Vocabulary> flow(String group) {
        return fetchPage(group, 0, 10)
                .flatMap(vocabularyId -> Flowable.fromCompletionStage(implDetail(vocabularyId)));
    }

    private Flowable<String> fetchPage(String group, int pageIndex, int pageSize) {
        return Flowable.fromCompletionStage(page(group, pageIndex, pageSize))
                .flatMap(vocabularyIds -> {

                    if (vocabularyIds.isEmpty() || vocabularyIds.size() < pageSize) {
                        return Flowable.fromIterable(vocabularyIds);
                    }

                    return Flowable.concat(
                            Flowable.fromIterable(vocabularyIds),
                            Flowable.defer(() -> fetchPage(group, pageIndex + 1, pageSize))
                    );

                });
    }

}
