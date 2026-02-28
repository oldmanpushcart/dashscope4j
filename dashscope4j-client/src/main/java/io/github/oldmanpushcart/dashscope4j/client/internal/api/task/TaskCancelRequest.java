package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.MT_APPLICATION_JSON;

class TaskCancelRequest extends ApiRequest<TaskCancelResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String taskId;

    public TaskCancelRequest(String taskId) {
        super(TaskCancelResponse.class);
        this.taskId = taskId;
    }

    @Override
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/task/{}/cancel >>> POST", taskId);
        final var endpoint = EndpointUtils.https(host, "/api/v1/tasks/%s/cancel".formatted(taskId));
        return new Request.Builder()
                .url(endpoint.toString())
                .post(RequestBody.create("", MT_APPLICATION_JSON))
                .build();
    }

    @Override
    public BiFunction<Response, String, TaskCancelResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/task/{}/cancel <<< {}", taskId, responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, TaskCancelResponse.class, this, httpResponse);
        };
    }

}
