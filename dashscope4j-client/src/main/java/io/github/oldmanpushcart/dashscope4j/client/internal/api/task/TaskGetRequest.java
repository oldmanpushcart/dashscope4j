package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

class TaskGetRequest extends ApiRequest<TaskGetResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String taskId;

    public TaskGetRequest(String taskId) {
        super(TaskGetResponse.class);
        this.taskId = taskId;
    }

    @Override
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/task/{} >>> GET", taskId);
        final var endpoint = EndpointUtils.https(host, "/api/v1/tasks/%s".formatted(taskId));
        return new Request.Builder()
                .url(endpoint.toString())
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, TaskGetResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/task/{} <<< {}", taskId, responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, TaskGetResponse.class, this, httpResponse);
        };
    }
}
