package io.github.oldmanpushcart.dashscope4j.api;

import org.junit.jupiter.api.Assertions;

import java.util.function.BiConsumer;

public class ApiResponseAssertions {

    public static void assertApiResponseSuccess(ApiResponse<?> response) {
        Assertions.assertTrue(response.ret().isSuccess(), "Response is not success");
        Assertions.assertNotNull(response.uuid(), "Response UUID is null");
        response.usage().items().forEach(item -> {
            final int cost = item.cost();
            Assertions.assertTrue(cost >= 0, String.format("Usage.%s cost is not >= 0", item.name()));
        });
    }

    public static void assertApiResponseFailure(ApiResponse<?> response) {
        Assertions.assertFalse(response.ret().isSuccess(), "Response is not failure");
        Assertions.assertNotNull(response.uuid(), "Response UUID is null");
    }

    public static <R extends ApiResponse<?>> BiConsumer<R, Throwable> assertApiResponseSuccessHandler() {
        return (r, ex) -> {
            Assertions.assertNull(ex, "Exception is not null");
            assertApiResponseSuccess(r);
        };
    }

    public static <R extends ApiResponse<?>> BiConsumer<R, Throwable> assertApiResponseFailureHandler() {
        return (r, ex) -> {
            Assertions.assertNull(ex, "Exception is not null");
            assertApiResponseFailure(r);
        };
    }

    public static <R extends ApiResponse<?>, T extends Throwable> BiConsumer<R, Throwable> assertApiResponseThrowsHandler(Class<T> exType) {
        return (r, ex) -> {
            Assertions.assertNotNull(ex, "Exception is null");
            Assertions.assertThrows(exType, () -> {
                throw ex;
            });
        };
    }

}
