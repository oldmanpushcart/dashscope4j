package io.github.oldmanpushcart.dashscope4j.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiAssertions {

    /**
     * ApiResponse的基础校验
     * <p>
     * 任何一个ApiResponse以及其子类都必须满足的基础校验
     * </p>
     *
     * @param response apiResponse
     */
    public static void assertApiResponseBase(AlgoResponse<?> response) {
        assertNotNull(response, "Response is null");
        assertNotNull(response.uuid(), "Response uuid is null");
        assertNotNull(response.code(), "Response code is null");
        assertNotNull(response.usage(), "Response usage is null");
    }

    /**
     * 校验ApiResponse是否成功
     * <ul>
     * <li>校验ApiResponse的基础信息</li>
     * <li>校验ApiResponse是否成功</li>
     * <li>校验成功ApiResponse所必须携带的数据</li>
     * </ul>
     *
     * @param response apiResponse
     */
    public static void assertApiResponseSuccessful(AlgoResponse<?> response) {
        assertApiResponseBase(response);
        assertTrue(response.isSuccess(), "Response is not successful");
        response.usage().items().forEach(item -> {
            assertNotNull(item, "Usage item is null");
            assertNotNull(item.name(), "Usage item name is null");
            assertTrue(item.cost() >= 0, "Usage item cost is negative");
        });
    }

}
