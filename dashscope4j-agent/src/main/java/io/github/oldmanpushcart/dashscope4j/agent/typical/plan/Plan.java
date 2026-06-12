package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import java.util.List;
import java.util.Map;

public class Plan {

    private String id;
    private String goal;
    private List<Step> steps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public static class Step {

        private String id;
        private String name;
        private String description;
        private Status status;
        private List<String> dependencyIds;
        private Map<String, Object> arguments;
        private Map<String, Object> result;
        private String message;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public List<String> getDependencyIds() {
            return dependencyIds;
        }

        public void setDependencyIds(List<String> dependencyIds) {
            this.dependencyIds = dependencyIds;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public enum Status {
            PENDING,
            RUNNING,
            SUCCESS,
            FAILURE,
            SKIPPED
        }

    }

}
