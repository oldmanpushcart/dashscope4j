package io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class VocabularyTestCase extends ClientSupport {

    private static final String GROUP = "test";

    @Test
    public void test$vocabulary$create() {
        final List<Vocabulary.Item> items = new ArrayList<>() {{
            add(Vocabulary.Item.of("测试", "zh", 5));
            add(Vocabulary.Item.of("测试", "zh", 5));
        }};
        final Vocabulary vocabulary = client.audio().vocabulary().create(GROUP, RecognitionModel.PARAFORMER_REALTIME_V2, items)
                .toCompletableFuture()
                .join();
        Assertions.assertNotNull(vocabulary);
        Assertions.assertEquals(RecognitionModel.PARAFORMER_REALTIME_V2.name(), vocabulary.target());
        Assertions.assertEquals(2, vocabulary.items().size());
        vocabulary.items().forEach(item -> {
            Assertions.assertEquals("测试", item.text());
            Assertions.assertEquals("zh", item.lang());
            Assertions.assertEquals(5, item.weight());
        });

        // cleanup
        client.audio().vocabulary().delete(vocabulary.identity())
                .toCompletableFuture()
                .join();
    }

    @Test
    public void test$vocabulary$update() {

        final Vocabulary created = client.audio().vocabulary().create(GROUP, RecognitionModel.PARAFORMER_REALTIME_V2, new ArrayList<>() {{
                    add(Vocabulary.Item.of("测试", "zh", 5));
                    add(Vocabulary.Item.of("测试", "zh", 5));
                }})
                .toCompletableFuture()
                .join();

        client.audio().vocabulary().update(created.identity(), new ArrayList<>() {{
                    add(Vocabulary.Item.of("测试-UPDATE", "zh", 5));
                    add(Vocabulary.Item.of("测试-UPDATE", "zh", 5));
                }})
                .toCompletableFuture()
                .join();

        final Vocabulary vocabulary = client.audio().vocabulary().detail(created.identity())
                .toCompletableFuture()
                .join();

        Assertions.assertEquals(2, vocabulary.items().size());
        vocabulary.items().forEach(item -> {
            Assertions.assertEquals("测试-UPDATE", item.text());
            Assertions.assertEquals("zh", item.lang());
            Assertions.assertEquals(5, item.weight());
        });

        // cleanup
        client.audio().vocabulary().delete(created.identity())
                .toCompletableFuture()
                .join();

    }

    @Test
    public void test$vocabulary$delete$not_existed() {
        client.audio().vocabulary().delete("not-existed-vocabularyId")
                .thenAccept(Assertions::assertFalse)
                .toCompletableFuture()
                .join();
    }

    @Test
    public void test$vocabulary$update$not_existed() {
        final ApiException apiEx = Assertions.assertThrows(ApiException.class, () -> {
            try {
                client.audio().vocabulary().update("not-existed-vocabularyId", new ArrayList<>() {{
                            add(Vocabulary.Item.of("测试-UPDATE", "zh", 5));
                            add(Vocabulary.Item.of("测试-UPDATE", "zh", 5));
                        }})
                        .toCompletableFuture()
                        .join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
        });

        Assertions.assertEquals(400, apiEx.status());
        Assertions.assertEquals("BadRequest.ResourceNotExist", apiEx.code());
    }

    @BeforeAll
    public static void cleanup() {
        client.audio().vocabulary().flow(GROUP)
                .filter(vocabulary -> vocabulary.createAt().isBefore(Instant.now().minus(Duration.ofDays(1))))
                .blockingSubscribe(vocabulary ->
                        client.audio().vocabulary().delete(vocabulary.identity())
                                .toCompletableFuture()
                                .join());
    }

}
