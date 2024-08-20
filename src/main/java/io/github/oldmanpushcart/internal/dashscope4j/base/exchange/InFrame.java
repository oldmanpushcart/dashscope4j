package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

record InFrame(

        @JsonProperty("header")
        Header header,

        @JsonProperty("payload")
        @JsonRawValue
        String payload

) {

    public record Header(

            @JsonProperty("task_id")
            String uuid,

            @JsonProperty("action")
            Type type,

            @JsonProperty("streaming")
            Exchange.Mode mode

    ) {

    }

    public enum Type {

        @JsonProperty("run-task")
        RUN,

        @JsonProperty("continue-task")
        CONTINUE,

        @JsonProperty("finish-task")
        FINISH

    }

    public static InFrame of(String uuid, Type type, Exchange.Mode mode, String payload) {
        return new InFrame(new Header(uuid, type, mode), payload);
    }


}
