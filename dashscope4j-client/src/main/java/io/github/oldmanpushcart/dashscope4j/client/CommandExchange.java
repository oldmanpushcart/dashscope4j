package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface CommandExchange<T> extends Exchange<CommandExchange.Command<T>> {

    interface Handler<T, R> extends Exchange.Handler<Command<T>, Event<R>> {

        @Override
        void onOpen(Exchange<Command<T>> exchange);

    }

    record Command<T>(
            @JsonProperty("header") Header header,
            @JsonProperty("payload") T payload
    ) {

        record Header(
                @JsonProperty("action") Action action,
                @JsonProperty("task_id") String id,
                @JsonProperty("streaming") Mode mode
        ) {

        }

        public enum Action {
            @JsonProperty("run-task") RUN,
            @JsonProperty("continue-task") CONTINUE,
            @JsonProperty("finish-task") FINISH,
        }

    }

    record Event<R>(
            @JsonProperty("header") Header header,
            @JsonProperty("payload") R payload
    ) {

        public record Header(
                @JsonProperty("task_id") String id,
                @JsonProperty("event") Type type
        ) {

        }

        public enum Type {
            @JsonProperty("task-started") STARTED,
            @JsonProperty("result-generated") GENERATED,
            @JsonProperty("task-finished") FINISHED,
            @JsonProperty("task-failed") FAILED
        }

    }

    enum Mode {
        @JsonProperty("none") ONCE,
        @JsonProperty("in") IN,
        @JsonProperty("out") OUT,
        @JsonProperty("duplex") DUPLEX
    }

}
