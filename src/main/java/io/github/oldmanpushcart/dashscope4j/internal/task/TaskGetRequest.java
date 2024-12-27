package io.github.oldmanpushcart.dashscope4j.internal.task;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TaskGetRequest extends ApiRequest<TaskGetResponse> {

    String taskId;

    private TaskGetRequest(Builder builder) {
        super(TaskGetResponse.class, builder);
        this.taskId = builder.taskId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TaskGetRequest request) {
        return new Builder(request);
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/task/get/{} >>> GET", taskId);
        return new Request.Builder()
                .url(String.format("https://dashscope.aliyuncs.com/api/v1/tasks/%s", taskId))
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, TaskGetResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/task/get/{} <<< {}", taskId, bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, TaskGetResponse.class, httpResponse);
        };
    }

    public static class Builder extends ApiRequest.Builder<TaskGetRequest, Builder> {

        private String taskId;

        public Builder() {

        }

        public Builder(TaskGetRequest request) {
            super(request);
            this.taskId = request.taskId;
        }

        public Builder taskId(String taskId) {
            this.taskId = requireNonNull(taskId);
            return this;
        }

        @Override
        public TaskGetRequest build() {
            requireNonNull(taskId);
            return new TaskGetRequest(this);
        }

    }

}
