package io.github.oldmanpushcart.dashscope4j.internal.task;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.function.BiFunction;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.MT_APPLICATION_JSON;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TaskCancelRequest extends ApiRequest<TaskCancelResponse> {

    String taskId;

    private TaskCancelRequest(Builder builder) {
        super(TaskCancelResponse.class, builder);
        this.taskId = builder.taskId;
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/task/cancel/{} >>> POST", taskId);
        return new Request.Builder()
                .url(String.format("https://dashscope.aliyuncs.com/api/v1/tasks/%s/cancel", taskId))
                .post(RequestBody.create("", MT_APPLICATION_JSON))
                .build();
    }

    @Override
    public BiFunction<Response, String, TaskCancelResponse> newResponseDecoder() {
        return (httpResponse, bodyJson)-> {
            log.debug("dashscope://base/task/cancel/{} <<< {}", taskId, bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, TaskCancelResponse.class, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TaskCancelRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<TaskCancelRequest, Builder> {

        private String taskId;

        public Builder() {

        }

        public Builder(TaskCancelRequest request) {
            super(request);
            this.taskId = request.taskId;
        }

        public Builder taskId(String taskId) {
            this.taskId = requireNonNull(taskId);
            return this;
        }

        @Override
        public TaskCancelRequest build() {
            requireNonNull(taskId);
            return new TaskCancelRequest(this);
        }

    }

}
