package io.github.oldmanpushcart.dashscope4j.client.api;

import static org.junit.jupiter.api.Assertions.*;

public class ApiAssertions {

    public static void assertApiResponse(ApiResponse response) {
        assertNotNull(response);
        assertNotNull(response.request());
        assertNotNull(response.uuid());
        assertNotNull(response.code());

    }

    public static void assertApiResponseSuccessful(ApiResponse response) {
        assertApiResponse(response);
        assertTrue(response.isSuccess());
        if (response instanceof AlgoResponse<?> algoResponse) {
            assertNotNull(algoResponse.usage());
        }
    }

    public static void assertApiResponseFailed(ApiResponse response) {
        assertApiResponse(response);
        assertFalse(response.isSuccess());
    }

}
