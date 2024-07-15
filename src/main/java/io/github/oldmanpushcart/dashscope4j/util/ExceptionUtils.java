package io.github.oldmanpushcart.dashscope4j.util;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 异常工具类
 */
public class ExceptionUtils {

    /**
     * 获取异常的根因
     *
     * @param cause 根异常
     * @param type  类型
     * @param <X>   异常类型
     * @return 根因
     */
    public static <X extends Throwable> X causeBy(Throwable cause, Class<? extends X> type) {
        return Optional.ofNullable(cause)
                .map(ex -> type.isInstance(ex) ? type.cast(ex) : causeBy(ex.getCause(), type))
                .orElse(null);
    }

    /**
     * 判断异常是否由指定类型引起
     *
     * @param cause 根异常
     * @param type  类型
     * @param <X>   异常类型
     * @return 是否由指定类型引起
     */
    public static <X extends Throwable> boolean isCauseBy(Throwable cause, Class<? extends X> type, Predicate<? super X> predicate) {
        return Optional.ofNullable(causeBy(cause, type))
                .filter(predicate)
                .isPresent();
    }

}
