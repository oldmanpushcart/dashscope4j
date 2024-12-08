package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

public class ChatAssertions {

    public static void assertChatResponseMessageTextContains(ChatResponse response, String expect) {
        final String text = response.output().best().message().text();
        Assertions.assertTrue(
                text.contains(expect),
                String.format("Expected text to contain: %s, but was: %s",
                        expect,
                        text
                ));
    }

    public static <R extends ChatResponse> BiConsumer<R, Throwable> whenCompleteAssertByAsyncForChatResponseMessageTextContains(String expect) {
        return (r, ex) -> {
            if (ex != null) {
                Assertions.fail("Unexpected exception", ex);
            }
            assertChatResponseMessageTextContains(r, expect);
        };
    }

    public static UnaryOperator<Flowable<ChatResponse>> thenApplyAssertByFlowForChatResponseMessageTextContains(boolean isIncremental, String expect) {
        return flow -> {
            final AtomicReference<StringBuilder> textBuilder = new AtomicReference<>(new StringBuilder());
            return flow
                    .doOnNext(r -> {
                        if (isIncremental) {
                            textBuilder.get().append(r.output().best().message().text());
                        } else {
                            textBuilder.set(new StringBuilder(r.output().best().message().text()));
                        }
                    })
                    .doOnComplete(() -> {
                        final String text = textBuilder.get().toString();
                        Assertions.assertTrue(
                                text.contains(expect),
                                String.format("Expected text to contain: %s, but was: %s",
                                        expect,
                                        text
                                ));
                    })
                    .doOnError(ex -> Assertions.fail("Unexpected exception", ex));
        };
    }

}
