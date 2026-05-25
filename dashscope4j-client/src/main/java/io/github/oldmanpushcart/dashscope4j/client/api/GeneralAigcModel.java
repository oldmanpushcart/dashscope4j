package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.*;

public class GeneralAigcModel implements AigcModel<Map<String, Object>, Map<String, Object>> {

    private final String name;
    private final String path;
    private final Set<String> tags;
    private final List<Interceptor> interceptors;

    private final boolean uploadEnabled;
    private final boolean inlineEnabled;

    private GeneralAigcModel(Builder builder) {
        this.name = builder.name;
        this.path = builder.path;
        this.uploadEnabled = builder.uploadEnabled;
        this.inlineEnabled = builder.inlineEnabled;
        this.tags = Collections.unmodifiableSet(builder.tags);
        this.interceptors = Collections.unmodifiableList(builder.interceptors);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> tags() {
        return tags;
    }

    @Override
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    public boolean uploadEnabled() {
        return uploadEnabled;
    }

    public boolean inlineEnabled() {
        return inlineEnabled;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GeneralAigcModel model) {
        return new Builder(model);
    }

    public static class Builder implements Buildable<GeneralAigcModel, Builder> {

        private String name;
        private String path;
        private boolean uploadEnabled;
        private boolean inlineEnabled;
        private final Set<String> tags = new LinkedHashSet<>();
        private final List<Interceptor> interceptors = new ArrayList<>();

        public Builder() {

        }

        public Builder(GeneralAigcModel model) {
            this.name = model.name;
            this.path = model.path;
            this.uploadEnabled = model.uploadEnabled;
            this.inlineEnabled = model.inlineEnabled;
            this.tags.addAll(model.tags);
            this.interceptors.addAll(model.interceptors);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder addTags(Set<String> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return this;
        }

        public Builder addInterceptor(Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder addInterceptors(List<Interceptor> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        public Builder uploadEnabled(boolean uploadEnabled) {
            this.uploadEnabled = uploadEnabled;
            return this;
        }

        public Builder inlineEnabled(boolean inlineEnabled) {
            this.inlineEnabled = inlineEnabled;
            return this;
        }

        @Override
        public GeneralAigcModel build() {
            return new GeneralAigcModel(this);
        }

    }

}
