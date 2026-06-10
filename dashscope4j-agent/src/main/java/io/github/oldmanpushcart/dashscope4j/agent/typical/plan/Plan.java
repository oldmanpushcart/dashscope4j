package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import java.util.List;
import java.util.Map;

public class Plan {

    private String id;
    private String goal;
    private List<Step> steps;

    public static class Step {

        private String id;
        private String name;
        private String description;
        private Status status;
        private List<String> dependencyIds;
        private Map<String, Object> arguments;
        private Map<String, Object> result;
        private String message;

        public enum Status {
            PENDING,
            RUNNING,
            SUCCESS,
            FAILURE,
            SKIPPED
        }

    }

}
