package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.GenericReflectUtils;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 模型
 */
public interface Model<I, O> {

    /**
     * @return 模型名称
     */
    @JsonValue
    String name();

    /**
     * @return 模型路径
     */
    String path();

    /**
     * @return 模型默认参数
     */
    default Map<String, Object> parameters() {
        return Map.of();
    }

    /**
     * @return 输入类型
     */
    default Type iType() {
        final var pType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        return null != pType
                ? pType.getActualTypeArguments()[0]
                : null;
    }

    /**
     * @return 输出类型
     */
    default Type oType() {
        final var pType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        return null != pType
                ? pType.getActualTypeArguments()[1]
                : null;
    }

}
