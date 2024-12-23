package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;

import java.net.URI;

/**
 * <pre><code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Error>
 *   <Code>InvalidArgument</Code>
 *   <Message>The bucket POST must contain the specified 'OSSAccessKeyId'. If it is specified, please check the order of the fields</Message>
 *   <RequestId>67684B970AF7903432D74FD8</RequestId>
 *   <HostId>dashscope-file-mgr.oss-cn-beijing.aliyuncs.com</HostId>
 *   <ArgumentName>OSSAccessKeyId</ArgumentName>
 *   <ArgumentValue></ArgumentValue>
 *   <EC>0002-00000706</EC>
 *   <RecommendDoc><a href="https://api.aliyun.com/troubleshoot?q=0002-00000706">...</a></RecommendDoc>
 * </Error>
 * </code></pre>
 */
public class PostUploadResponse extends ApiResponse<URI> {

    private URI output;

    public PostUploadResponse() {
        super("", null, null);
    }

    @JsonCreator
    private PostUploadResponse(

            @JsonProperty("RequestId")
            String uuid,

            @JsonProperty("Code")
            String code,

            @JsonProperty("Message")
            String message

    ) {
        super(uuid, code, message);
    }

    @Override
    public URI output() {
        return output;
    }

    PostUploadResponse output(URI output) {
        this.output = output;
        return this;
    }

}
