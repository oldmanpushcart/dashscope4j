package io.github.oldmanpushcart.dashscope4j.client.aigc.embedding;

import io.github.oldmanpushcart.dashscope4j.client.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class EmbeddingTestCase implements LoadingEnv {

    @Test
    public void test$text_embedding() {

        final var request = AigcRequest.newBuilder(TextEmbeddingModel.TEXT_EMBEDDING_V4)
                .input(TextEmbeddingModel.Input.newBuilder()
                        .texts(List.of(
                                "锄禾日当午",
                                "汗滴禾下土",
                                "谁知盘中餐",
                                "粒粒皆辛苦"
                        ))
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        Assertions.assertEquals(4, response.output().embeddings().size());

    }

    @Test
    public void test$mm_embedding$text() {
        final var request = AigcRequest.newBuilder(MmEmbeddingModel.QWEN3_VL_EMBEDDING)
                .input(MmEmbeddingModel.Input.newBuilder()
                        .contents(List.of(
                                MmContent.ofText("锄禾日当午"),
                                MmContent.ofText("汗滴禾下土"),
                                MmContent.ofText("谁知盘中餐"),
                                MmContent.ofText("粒粒皆辛苦")
                        ))
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        Assertions.assertEquals(4, response.output().embeddings().size());
    }

    @Test
    public void test$mm_embedding$image() {
        final var request = AigcRequest.newBuilder(MmEmbeddingModel.QWEN3_VL_EMBEDDING)
                .input(MmEmbeddingModel.Input.newBuilder()
                        .contents(List.of(
                                MmContent.ofImage(new File("./test-data/image/red-cup.jpeg").toURI()),
                                MmContent.ofImage(new File("./test-data/image/sketch-tree.jpg").toURI())
                        ))
                        .uploadEnabled(true)
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        Assertions.assertEquals(2, response.output().embeddings().size());
    }

    @Test
    public void test$mm_embedding$complex() {
        final var request = AigcRequest.newBuilder(MmEmbeddingModel.QWEN3_VL_EMBEDDING)
                .input(MmEmbeddingModel.Input.newBuilder()
                        .contents(List.of(
                                MmContent.Complex.newBuilder()
                                        .text("锄禾日当午，汗滴禾下土，谁知盘中餐，粒粒皆辛苦！")
                                        .image(new File("./test-data/image/red-cup.jpeg").toURI())
                                        .build()
                        ))
                        .uploadEnabled(true)
                        .build())
                .build();

        final var response = client.async(request)
                .toCompletableFuture()
                .join();

        ApiAssertions.assertApiResponseSuccessful(response);
        Assertions.assertEquals(1, response.output().embeddings().size());
    }

}
