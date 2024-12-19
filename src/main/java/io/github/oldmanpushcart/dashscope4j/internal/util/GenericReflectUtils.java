package io.github.oldmanpushcart.dashscope4j.internal.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 泛型反射工具
 */
public class GenericReflectUtils {

    /**
     * 查找类第一个声明的指定的泛型
     *
     * @param type   类型
     * @param target 目标类型
     * @return 泛型
     */
    public static ParameterizedType findFirst(Type type, Class<?> target) {

        // 必须是Class类型才有继续查的意义
        if (!(type instanceof Class<?>)) {
            return null;
        }

        final Class<?> clazz = (Class<?>)type;

        // 查找泛型接口
        for (final Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                final ParameterizedType pType = (ParameterizedType) genericInterface;
                if (Objects.equals(pType.getRawType(), target)) {
                    return pType;
                }
            }
        }

        // 查找泛型父类
        return findFirst(clazz.getGenericSuperclass(), target);
    }

}
