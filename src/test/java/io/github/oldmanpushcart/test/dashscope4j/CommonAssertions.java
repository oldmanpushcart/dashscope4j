package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class CommonAssertions {

    public static void assertUsage(Usage usage) {
        Assertions.assertNotNull(usage.items());

        final var totalRef = new AtomicLong();
        usage.items().forEach(item-> {
            Assertions.assertNotNull(item.name());
            Assertions.assertTrue(item.cost()>=0);
            totalRef.addAndGet(item.cost());
        });

        Assertions.assertEquals(totalRef.get(), usage.total());
    }

    public static void assertRet(Ret ret) {
        Assertions.assertNotNull(ret.code());
        Assertions.assertNotNull(ret.message());
    }

    public static <X extends Throwable> void assertRootThrows(Class<X> exceptionClass, ThrowingRunnable runnable) {
        assertRootThrows(exceptionClass, runnable, exception -> {});
    }

    public static <X extends Throwable> void assertRootThrows(Class<X> exceptionClass, ThrowingRunnable runnable, Consumer<X> exceptionAsserter) {
        try {
            runnable.run();
            throw new AssertionError("Expected %s to be thrown, but nothing was thrown.".formatted(exceptionClass.getSimpleName()));
        } catch (Throwable ex) {
            final Throwable cause;

            // Unwrap CompletionException
            if(ex instanceof CompletionException) {
                cause = ex.getCause();
            }

            // Unwrap ExecutionException
            else if(ex instanceof ExecutionException) {
                cause = ex.getCause();
            }

            // real exception
            else {
                cause = ex;
            }

            // Check if the exception is the expected one
            if (exceptionClass.isInstance(cause)) {
                exceptionAsserter.accept(exceptionClass.cast(cause));
            } else {
                throw new AssertionError("Expected %s to be thrown, but %s was thrown.".formatted(
                        exceptionClass.getSimpleName(),
                        cause.getClass().getSimpleName()
                ));
            }

        }
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

}
