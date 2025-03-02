package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ChatLongTestCase extends ClientSupport {

    private final File file1 = new File("./test-data/yuanshen-game.docx");
    private final File file2 = new File("./test-data/P020210313315693279320.pdf");

    private static String encodingTestFilename(String filename) {
        if (isTestFilename(filename)) {
            return filename;
        }
        return String.format("%s_test_%s", System.currentTimeMillis(), filename);
    }

    private static boolean isTestFilename(String filename) {
        return filename.matches("\\d+_test_.*");
    }

    private static Instant parseInstantFromTestFilename(String filename) {
        if (!isTestFilename(filename)) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        return Instant.ofEpochMilli(Long.parseLong(filename.split("_")[0]));
    }

    @BeforeAll
    public static void cleanup() {

        client.base().files().flow()
                .blockingSubscribe(meta -> {

                    if (!isTestFilename(meta.name())) {
                        return;
                    }

                    final Instant instant = parseInstantFromTestFilename(meta.name());
                    if (instant.isBefore(Instant.now().minus(Duration.ofDays(1)))) {
                        client.base().files().delete(meta.identity())
                                .toCompletableFuture()
                                .join();
                    }

                });

    }

    @Test
    public void test$chat$long() {

        final String filename1 = encodingTestFilename(file1.getName());
        final FileMeta meta1 = client.base().files().create(file1.toURI(), filename1, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final String filename2 = encodingTestFilename(file2.getName());
        final FileMeta meta2 = client.base().files().create(file2.toURI(), filename2, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .addMessage(Message.ofUser(List.of(
                        Content.ofText("总结文章内容"),
                        Content.ofFile(meta1.toURI()),
                        Content.ofFile(meta2.toURI())
                )))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());
        final String text = response.output().best().message().text();
        Assertions.assertTrue(text.contains("规划"));
        Assertions.assertTrue(text.contains("原神"));

    }

}
