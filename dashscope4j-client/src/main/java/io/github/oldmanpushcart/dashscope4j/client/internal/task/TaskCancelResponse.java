package io.github.oldmanpushcart.dashscope4j.client.internal.task;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TaskCancelResponse extends ApiResponse<Object> {

    @JsonCreator
    private TaskCancelResponse(

            @JacksonInject("dashscope/request")
            TaskCancelRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message

    ) {
        super(request, uuid, code, message);
    }

    @Override
    public Object output() {
        return new Object();
    }

}
