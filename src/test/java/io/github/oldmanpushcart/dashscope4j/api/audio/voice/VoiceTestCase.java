package io.github.oldmanpushcart.dashscope4j.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionException;

import static io.github.oldmanpushcart.dashscope4j.api.audio.tts.SpeechSynthesisOptions.SAMPLE_RATE;

public class VoiceTestCase extends ClientSupport {

    private static final String GROUP = "test";

    private static final SpeechSynthesisModel TARGET_MODEL = SpeechSynthesisModel.newBuilder()
            .name(SpeechSynthesisModel.MODEL_NAME_COSYVOICE_V1)
            .remote(Constants.WSS_REMOTE)
            .option(SAMPLE_RATE, Constants.SAMPLE_RATE_48K)
            .build();

    private static final URI RESOURCE = new File("./test-data/sc2-human.mp3").toURI();

    private static void assertVoice(Voice voice) {
        Assertions.assertNotNull(voice);
        Assertions.assertNotNull(voice.identity());
        Assertions.assertNotNull(voice.createdAt());
        Assertions.assertNotNull(voice.updatedAt());
        Assertions.assertNotNull(voice.resource());
    }

    @Test
    public void test$voice$create() {
        final Voice voice = client.audio().voice().create(GROUP, TARGET_MODEL, RESOURCE)
                .toCompletableFuture()
                .join();
        assertVoice(voice);
    }

    @Test
    public void test$voice$detail() {
        final Voice created = client.audio().voice().create(GROUP, TARGET_MODEL, RESOURCE)
                .toCompletableFuture()
                .join();
        final Voice voice = client.audio().voice().detail(created.identity())
                .toCompletableFuture()
                .join();
        assertVoice(voice);
    }

    @Test
    public void test$voice$update() {
        final Voice created = client.audio().voice().create(GROUP, TARGET_MODEL, RESOURCE)
                .toCompletableFuture()
                .join();
        client.audio().voice().update(created.identity(), RESOURCE)
                .toCompletableFuture()
                .join();
        final Voice voice = client.audio().voice().detail(created.identity())
                .toCompletableFuture()
                .join();
        assertVoice(voice);
    }

    @Test
    public void test$voice$delete() {
        final Voice created = client.audio().voice().create(GROUP, TARGET_MODEL, RESOURCE)
                .toCompletableFuture()
                .join();
        final boolean ret = client.audio().voice().delete(created.identity())
                .toCompletableFuture()
                .join();
        Assertions.assertTrue(ret);
        final Voice voice = client.audio().voice().detail(created.identity())
                .toCompletableFuture()
                .join();
        Assertions.assertNull(voice);
    }

    @Test
    public void test$voice$delete$not_existed() {
        final boolean ret = client.audio().voice().delete("not-existed-voiceId")
                .toCompletableFuture()
                .join();
        Assertions.assertFalse(ret);
    }

    @Test
    public void test$voice$update$not_existed() {
        final ApiException apiEx = Assertions.assertThrows(ApiException.class, ()-> {
            try {
                client.audio().voice().update("not-existed-voiceId", RESOURCE)
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
        client.audio().voice().flow(GROUP)
                .filter(voice -> voice.createdAt().isBefore(Instant.now().minus(Duration.ofDays(1))))
                .blockingSubscribe(voice ->
                        client.audio().vocabulary().delete(voice.identity())
                                .toCompletableFuture()
                                .join());
    }

}
