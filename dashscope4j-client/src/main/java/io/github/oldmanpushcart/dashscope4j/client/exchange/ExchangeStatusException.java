package io.github.oldmanpushcart.dashscope4j.client.exchange;

public class ExchangeStatusException extends RuntimeException {

    private final int status;
    private final String reason;

    public ExchangeStatusException(int status, String reason) {
        super("Exchange status error! code=%s;reason=%s".formatted(status, reason));
        this.status = status;
        this.reason = reason;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

}
