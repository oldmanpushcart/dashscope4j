package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.util.ExceptionUtils;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class CommonAssertions {

    public static void assertUsage(Usage usage) {
        Assertions.assertNotNull(usage.items());

        final var totalRef = new AtomicLong();
        usage.items().forEach(item -> {
            Assertions.assertNotNull(item.name());
            Assertions.assertTrue(item.cost() >= 0);
            totalRef.addAndGet(item.cost());
        });

        Assertions.assertEquals(totalRef.get(), usage.total());
    }

    public static void assertRet(Ret ret) {
        Assertions.assertNotNull(ret.code());
        Assertions.assertNotNull(ret.message());
    }

    public static <X extends Throwable> void assertRootThrows(Class<X> exceptionClass, ThrowingRunnable runnable) {
        assertRootThrows(exceptionClass, runnable, exception -> {
        });
    }

    public static <X extends Throwable> void assertRootThrows(Class<X> exceptionClass, ThrowingRunnable runnable, Consumer<X> exceptionAsserter) {
        try {
            runnable.run();
            throw new AssertionError("Expected %s to be thrown, but nothing was thrown.".formatted(exceptionClass.getSimpleName()));
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {

            final var cause = ExceptionUtils.causeBy(ex, exceptionClass);
            if (cause != null) {
                exceptionAsserter.accept(exceptionClass.cast(cause));
            } else {
                throw new AssertionError("Expected %s to be thrown, but %s was thrown.".formatted(
                        exceptionClass.getSimpleName(),
                        ex.getClass().getSimpleName()
                ));
            }

        }
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

}
