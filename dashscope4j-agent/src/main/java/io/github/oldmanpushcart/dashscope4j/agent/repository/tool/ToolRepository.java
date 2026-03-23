package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.BaseRepository;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

public class ToolRepository extends BaseRepository<String, Tool> {

    protected ToolRepository(Builder builder) {
        super(
                builder.name,
                new ToolIndexer(builder.client),
                new ToolStorer(),
                builder.loader
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolRepository, Builder> {

        private String name;
        private DashscopeClient client;
        private Repository.Loader<String, Tool> loader;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder loader(Repository.Loader<String, Tool> loader) {
            this.loader = loader;
            return this;
        }

        @Override
        public ToolRepository build() {
            return new ToolRepository(this);
        }

    }

}
