package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;

import java.net.URI;

public class PostUploadResponse extends ApiResponse {

    private final URI uploaded;

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
        this.uploaded = null;
    }

    public PostUploadResponse(PostUploadRequest request, String uuid, URI uploaded) {
        super(request, uuid,  "SUCCESS", "SUCCESS");
         this.uploaded = uploaded;
    }

     public URI uploaded() {
        return uploaded;
    }

}
