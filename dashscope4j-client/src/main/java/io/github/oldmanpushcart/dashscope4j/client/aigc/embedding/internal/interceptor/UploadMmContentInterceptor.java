package io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.MmContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UploadMmContentInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof MmEmbeddingModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        if (!request.input().uploadEnabled()) {
            return chain.proceed();
        }

        return CompletableFutureUtils
                .sequentialMap(request.input().contents(), content -> {

                    if (content instanceof MmContent.Image imageContent
                            && IOUtils.isFileURI(imageContent.uri())) {
                        return chain.client().base().store().upload(imageContent.uri(), model)
                                .thenApply(MmContent.Image::new);
                    }

                    if (content instanceof MmContent.Video videoContent
                            && IOUtils.isFileURI(videoContent.uri())) {
                        return chain.client().base().store().upload(videoContent.uri(), model)
                                .thenApply(MmContent.Video::new);
                    }

                    if (content instanceof MmContent.ImageList imagesContent) {
                        return CompletableFutureUtils
                                .sequentialMap(imagesContent.uris(), uri -> {
                                    if (!IOUtils.isFileURI(uri)) {
                                        return CompletableFuture.completedStage(uri);
                                    }
                                    return chain.client().base().store().upload(uri, model);
                                })
                                .thenApply(MmContent.ImageList::new);
                    }

                    if (content instanceof MmContent.Complex complexContent) {
                        final var builder = MmContent.Complex.newBuilder(complexContent);
                        var stage = CompletableFuture.completedStage(builder);
                        if (complexContent.image() != null && IOUtils.isFileURI(complexContent.image())) {
                            stage = stage.thenCompose(b -> chain.client().base().store().upload(complexContent.image(), model)
                                    .thenApply(builder::image));
                        }
                        if (complexContent.video() != null && IOUtils.isFileURI(complexContent.video())) {
                            stage = stage.thenCompose(b -> chain.client().base().store().upload(complexContent.video(), model)
                                    .thenApply(builder::video));
                        }
                        return stage.thenApply(MmContent.Complex.Builder::build);
                    }

                    return CompletableFuture.completedStage(content);
                })
                .thenApply(newContents -> {
                    final var input = request.input();
                    return AigcRequest.newBuilder(request)
                            .input(MmEmbeddingModel.Input.newBuilder(input)
                                    .contents(newContents)
                                    .build())
                            .build();
                })
                .thenCompose(chain::proceed);
    }

}
