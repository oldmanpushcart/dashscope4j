package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

class TaskCancelResponse extends ApiResponse {

    @JsonCreator
    private TaskCancelResponse(

            @JacksonInject("dashscope/request")
            TaskCancelRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc

    ) {
        super(request, uuid, code, desc);
    }

}
