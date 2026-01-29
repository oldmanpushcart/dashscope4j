package io.github.oldmanpushcart.dashscope4j.client.aigc.omni.realtime;

public class OmniRealtimeErrorException extends RuntimeException {

    private final String code;
    private final String reason;

    public OmniRealtimeErrorException(String code, String reason) {
        super("Omni-Realtime occur error! code=%s;reason=%s;".formatted(code, reason));
        this.code = code;
        this.reason = reason;
    }

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

}
