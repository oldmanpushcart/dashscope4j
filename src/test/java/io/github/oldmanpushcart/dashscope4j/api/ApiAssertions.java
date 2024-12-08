package io.github.oldmanpushcart.dashscope4j.api;

import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Assertions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ApiAssertions {

    public static void assertApiResponseSuccessful(ApiResponse<?> response) {
        Assertions.assertNotNull(response.uuid(), "Response UUID is null");
        Assertions.assertNotNull(response.ret(), "Response ret is null");
        Assertions.assertNotNull(response.usage(), "Response usage is null");
        Assertions.assertTrue(response.ret().isSuccess(), "Response is not success");
        response.usage().items().forEach(item -> {
            final int cost = item.cost();
            Assertions.assertTrue(cost >= 0, String.format("Usage.%s cost is not >= 0", item.name()));
        });
    }

    public static void assertApiResponseFailed(ApiResponse<?> response) {
        Assertions.assertNotNull(response.uuid(), "Response UUID is null");
        Assertions.assertNotNull(response.ret(), "Response ret is null");
        Assertions.assertFalse(response.ret().isSuccess(), "Response is not failure");
    }

    public static <R extends ApiResponse<?>> BiConsumer<R, Throwable> whenCompleteAssertByAsyncForApiResponseSuccessful() {
        return (r, ex) -> {
            if (ex != null) {
                Assertions.fail("Unexpected exception", ex);
            }
            assertApiResponseSuccessful(r);
        };
    }

    public static <R extends ApiResponse<?>> BiConsumer<R, Throwable> whenCompleteAssertByAsyncForApiResponseFailed() {
        return (r, ex) -> {
            if (ex != null) {
                Assertions.fail("Unexpected exception", ex);
            }
            assertApiResponseFailed(r);
        };
    }

    public static <R extends ApiResponse<?>, T extends Throwable> BiConsumer<R, Throwable> whenCompleteAssertByAsyncForApiResponseThrows(Class<T> exType) {
        return (r, ex) -> {
            Assertions.assertNotNull(ex, "Exception is null");
            Assertions.assertThrows(exType, () -> {
                throw ex;
            });
        };
    }

    public static Consumer<Flowable<? extends ApiResponse<?>>> thenAcceptAssertByFlowForApiResponseSuccessful() {
        return flow -> flow
                .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                .doOnError(ex -> Assertions.fail("Unexpected exception", ex))
                .blockingForEach(r -> {
                });
    }

}
