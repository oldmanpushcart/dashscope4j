package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.voice.Voice;
import io.github.oldmanpushcart.dashscope4j.api.audio.voice.VoiceOp;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.failedStage;
import static io.github.oldmanpushcart.dashscope4j.internal.util.DashscopeApiUtils.isCauseByResourceNotExisted;
import static java.util.Objects.isNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
public class VoiceOpImpl implements VoiceOp {

    private final ApiOp apiOp;

    @Override
    public CompletionStage<Voice> create(String group, Model targetModel, URI resource) {
        final VoiceCreateRequest request = VoiceCreateRequest.newBuilder()
                .model(VoiceModel.VOICE_ENROLLMENT)
                .group(group)
                .targetModel(targetModel)
                .resource(resource)
                .build();
        return apiOp.executeAsync(request)
                .thenCompose(response -> detail(response.output().voiceId()));
    }

    @Override
    public CompletionStage<Voice> detail(String voiceId) {
        return implDetail(voiceId)

                /*
                 * 如果查询错误的原因是数据不存在，则返回null
                 */
                .<CompletionStage<Voice>>handle((v, ex) ->
                        isNull(ex) || isCauseByResourceNotExisted(ex)
                                ? completedFuture(v)
                                : failedStage(ex))
                .thenCompose(v -> v);
    }

    private CompletionStage<Voice> implDetail(String voiceId) {
        final VoiceDetailRequest request = VoiceDetailRequest.newBuilder()
                .model(VoiceModel.VOICE_ENROLLMENT)
                .voiceId(voiceId)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response ->
                        new Voice(
                                voiceId,
                                response.output().target(),
                                response.output().createdAt(),
                                response.output().updatedAt(),
                                response.output().resource()
                        ));
    }

    @Override
    public CompletionStage<?> update(String voiceId, URI resource) {
        final VoiceUpdateRequest request = VoiceUpdateRequest.newBuilder()
                .model(VoiceModel.VOICE_ENROLLMENT)
                .voiceId(voiceId)
                .resource(resource)
                .build();
        return apiOp.executeAsync(request);
    }

    @Override
    public CompletionStage<Boolean> delete(String voiceId) {
        final VoiceDeleteRequest request = VoiceDeleteRequest.newBuilder()
                .model(VoiceModel.VOICE_ENROLLMENT)
                .voiceId(voiceId)
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
        final VoicePageQueryRequest request = VoicePageQueryRequest.newBuilder()
                .model(VoiceModel.VOICE_ENROLLMENT)
                .group(group)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response ->
                        response.output().items().stream()
                                .map(VoicePageQueryResponse.Item::voiceId)
                                .collect(Collectors.toList()))
                .thenApply(Collections::unmodifiableList);
    }

    @Override
    public Flowable<Voice> flow(String group) {
        return fetchPage(group, 0, 10)
                .flatMap(voiceId -> Flowable.fromCompletionStage(implDetail(voiceId)));
    }

    private Flowable<String> fetchPage(String group, int pageIndex, int pageSize) {
        return Flowable.fromCompletionStage(page(group, pageIndex, pageSize))
                .flatMap(voiceIds -> {

                    if (voiceIds.isEmpty() || voiceIds.size() < pageSize) {
                        return Flowable.fromIterable(voiceIds);
                    }

                    return Flowable.concat(
                            Flowable.fromIterable(voiceIds),
                            Flowable.defer(() -> fetchPage(group, pageIndex + 1, pageSize))
                    );

                });
    }

}
