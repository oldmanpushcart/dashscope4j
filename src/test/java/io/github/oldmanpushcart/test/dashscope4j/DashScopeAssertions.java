package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import org.junit.jupiter.api.Assertions;

public class DashScopeAssertions {

    public static void assertApiRequest(ApiRequest request) {
        Assertions.assertNotNull(request);
    }

    public static void assertApiResponse(ApiResponse<?> response) {
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.uuid());
        Assertions.assertFalse(response.uuid().isBlank());

        // usage
        Assertions.assertNotNull(response.usage());
        Assertions.assertTrue(response.usage().total() > 0);
        CommonAssertions.assertUsage(response.usage());

        // ret
        Assertions.assertNotNull(response.ret());
        Assertions.assertTrue(response.ret().isSuccess());
        CommonAssertions.assertRet(response.ret());
    }

    public static void assertChatRequest(ChatRequest request) {
        assertApiRequest(request);
        Assertions.assertNotNull(request.option());
        Assertions.assertNotNull(request.model());
    }

    public static void assertChatResponse(ChatResponse response) {

        assertApiResponse(response);

        // usage
        CommonAssertions.assertUsage(response.usage());
        Assertions.assertTrue(response.usage().total() > 0);

        // choices
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().choices());
        Assertions.assertFalse(response.output().choices().isEmpty());

    }

    public static void assertEmbeddingRequest(EmbeddingRequest request) {
        assertApiRequest(request);
        Assertions.assertNotNull(request.option());
        Assertions.assertNotNull(request.model());
    }

    public static void assertEmbeddingResponse(EmbeddingResponse response) {
        assertApiResponse(response);
        Assertions.assertNotNull(response.output());

        // usage
        CommonAssertions.assertUsage(response.usage());
        Assertions.assertTrue(response.usage().total() > 0);

        // embeddings
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().embeddings());
        Assertions.assertFalse(response.output().embeddings().isEmpty());

    }

    public static void assertGenImageRequest(GenImageRequest request) {
        assertApiRequest(request);
        Assertions.assertNotNull(request.model());
        Assertions.assertNotNull(request.option());
    }

    public static void assertGenImageResponse(GenImageResponse response) {
        assertApiResponse(response);

        // usage
        CommonAssertions.assertUsage(response.usage());
        Assertions.assertTrue(response.usage().total() > 0);

        // results
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().results());
        response.output().results().forEach(item -> {
            Assertions.assertNotNull(item.ret());
            CommonAssertions.assertRet(item.ret());
            if (item.ret().isSuccess()) {
                Assertions.assertNotNull(item.image());
            }
        });
    }

}
