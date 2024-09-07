package io.github.oldmanpushcart.dashscope4j.base.exchange;

/**
 * 交换通道异常
 *
 * @since 2.2.0
 */
public class ExchangeException extends RuntimeException {

    private final String uuid;
    private final Exchange.Mode mode;

    /**
     * 交换通道异常
     *
     * @param uuid    通道ID
     * @param mode    通道模式
     * @param message 异常信息
     */
    public ExchangeException(String uuid, Exchange.Mode mode, String message) {
        super("exchange://%s/%s %s".formatted(
                mode,
                uuid,
                message
        ));
        this.uuid = uuid;
        this.mode = mode;
    }

    /**
     * @return 通道ID
     */
    public String uuid() {
        return uuid;
    }

    /**
     * @return 通道模式
     */
    public Exchange.Mode mode() {
        return mode;
    }

    /**
     * 通道异常：异常关闭
     */
    public static class AbnormalClosedException extends ExchangeException {

        private final int status;
        private final String reason;

        /**
         * 异常关闭
         *
         * @param uuid   通道ID
         * @param mode   通道模式
         * @param status 关闭状态
         * @param reason 关闭原因
         */
        public AbnormalClosedException(String uuid, Exchange.Mode mode, int status, String reason) {
            super(uuid, mode, "Abnormal closure! status=%s;reason=%s;".formatted(
                    status,
                    reason
            ));
            this.status = status;
            this.reason = reason;
        }

        /**
         * @return 关闭状态
         * @since 2.2.1
         */
        public int status() {
            return status;
        }

        /**
         * @return 关闭原因
         * @since 2.2.1
         */
        public String reason() {
            return reason;
        }

    }

}
