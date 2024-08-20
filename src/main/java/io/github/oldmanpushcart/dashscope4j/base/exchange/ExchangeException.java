package io.github.oldmanpushcart.dashscope4j.base.exchange;

public class ExchangeException extends RuntimeException {

    private final String uuid;
    private final Exchange.Mode mode;

    public ExchangeException(String uuid, Exchange.Mode mode, String message) {
        super("exchange://%s/%s %s".formatted(
                mode,
                uuid,
                message
        ));
        this.uuid = uuid;
        this.mode = mode;
    }

    public String uuid() {
        return uuid;
    }

    public Exchange.Mode mode() {
        return mode;
    }

    public static class AbnormalClosedException extends ExchangeException {

        public AbnormalClosedException(String uuid, Exchange.Mode mode, int status, String reason) {
            super(uuid, mode, "Abnormal closure! status=%s;reason=%s;".formatted(
                    status,
                    reason
            ));
        }

    }

}
