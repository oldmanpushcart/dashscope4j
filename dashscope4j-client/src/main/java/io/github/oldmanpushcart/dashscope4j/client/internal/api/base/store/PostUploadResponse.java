package io.github.oldmanpushcart.dashscope4j.client.internal.api.base.store;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

public class PostUploadResponse extends ApiResponse {

    @JsonCreator
    public PostUploadResponse(

            @JacksonInject("dashscope/request")
            PostUploadRequest request,

            @JsonProperty("RequestId")
            String uuid,

            @JsonProperty("Code")
            String code,

            @JsonProperty("Message")
            String desc

    ) {
        super(request, uuid, code, desc);
    }

}
