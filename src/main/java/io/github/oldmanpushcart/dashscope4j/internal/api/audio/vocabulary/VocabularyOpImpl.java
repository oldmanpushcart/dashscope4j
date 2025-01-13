package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.Vocabulary;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.VocabularyOp;
import io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor
public class VocabularyOpImpl implements VocabularyOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<Vocabulary> create(String group, Model target, Collection<Vocabulary.Item> items) {

        final VocabularyCreateRequest request = VocabularyCreateRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .group(group)
                .targetModel(target)
                .items(items)
                .build();

        return apiOp.executeAsync(request)
                .thenCompose(response ->
                        implDetail(response.output().vocabularyId()));

    }

    private static boolean isCauseByResourceNotExisted(Throwable ex) {
        final Throwable cause = CompletableFutureUtils.unwrapEx(ex);
        if (cause instanceof ApiException) {
            final ApiException apiEx = (ApiException) cause;
            return apiEx.status() == 400
                   && "BadRequest.ResourceNotExist".equals(apiEx.code());
        }
        return false;
    }

    @Override
    public CompletionStage<Vocabulary> detail(String vocabularyId) {
        return implDetail(vocabularyId)

                /*
                 * 如果查询错误的原因是热词表不存在，则返回null
                 */
                .<CompletionStage<Vocabulary>>handle((v, ex) -> {
                    if (Objects.isNull(ex)) {
                        return CompletableFuture.completedFuture(v);
                    }
                    if (isCauseByResourceNotExisted(ex)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFutureUtils.failedStage(ex);
                })
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
                 * 如果删除异常的原因是热词表不存在，则幂等返回false
                 */
                .<CompletionStage<Boolean>>handle((unused, ex) -> {
                    if (Objects.isNull(ex)) {
                        return CompletableFuture.completedFuture(true);
                    }
                    if (isCauseByResourceNotExisted(ex)) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return CompletableFutureUtils.failedStage(ex);
                })
                .thenCompose(v -> v);
    }

    @Override
    public CompletionStage<List<String>> page(String group, int index, int size) {
        final VocabularyPageQueryRequest request = VocabularyPageQueryRequest.newBuilder()
                .model(VocabularyModel.SPEECH_BIASING)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response ->
                        response.output().items().stream()
                                .map(VocabularyPageQueryResponse.Item::vocabularyId)
                                .collect(Collectors.toList()));
    }

    @Override
    public Flowable<Vocabulary> flow(String group) {
        return fetchPage(group, 0, 10)
                .flatMap(vocabularyId -> Flowable.fromCompletionStage(implDetail(vocabularyId)));
    }

    private Flowable<String> fetchPage(String group, int index, int size) {
        return Flowable.fromCompletionStage(page(group, index, size))
                .flatMap(vocabularyIds -> {

                    if (vocabularyIds.isEmpty() || vocabularyIds.size() < size) {
                        return Flowable.fromIterable(vocabularyIds);
                    }

                    return Flowable.concat(
                            Flowable.fromIterable(vocabularyIds),
                            Flowable.defer(() -> fetchPage(group, index + 1, size))
                    );

                });
    }

}
