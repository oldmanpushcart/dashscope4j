package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_CONTENT_TYPE;

class TaskGetRequest extends ApiRequest<TaskGetResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String taskId;

    public TaskGetRequest(String taskId) {
        super(TaskGetResponse.class);
        this.taskId = taskId;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/task/{} >>> GET", taskId);
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host, "/api/v1/tasks/%s".formatted(taskId)))
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, TaskGetResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/task/{} <<< {}", taskId, responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, TaskGetResponse.class, this, httpResponse);
        };
    }
}
