package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.BaseRepository;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.storage.InMemoryStorage;
import io.github.oldmanpushcart.dashscope4j.agent.storage.Storage;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.Objects;

public class ToolRepository extends BaseRepository<String, Tool> {

    protected ToolRepository(Builder builder) {
        super(
                builder.name,
                Objects.requireNonNullElseGet(builder.indexer, () -> new ToolIndexer(builder.client)),
                Objects.requireNonNullElseGet(builder.storage, InMemoryStorage::new),
                Objects.requireNonNullElseGet(builder.loader, Repository.Loader::empty)
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolRepository, Builder> {

        private String name = "tool";
        private DashscopeClient client;
        private Storage<String, Tool> storage;
        private Repository.Indexer<String, Tool> indexer;
        private Repository.Loader<String, Tool> loader;

        /**
         * 设置仓库名称
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 Dashscope 客户端（用于创建默认的 ToolIndexer）
         */
        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        /**
         * 设置存储实现（可选，默认为 InMemoryStorage）
         *
         * @param storage 存储实现
         * @return Builder
         */
        public Builder storage(Storage<String, Tool> storage) {
            this.storage = storage;
            return this;
        }

        /**
         * 设置索引器（可选，默认为 ToolIndexer）
         *
         * @param indexer 索引器
         * @return Builder
         */
        public Builder indexer(Repository.Indexer<String, Tool> indexer) {
            this.indexer = indexer;
            return this;
        }

        /**
         * 设置数据加载器（可选，默认为 empty loader）
         *
         * @param loader 加载器
         * @return Builder
         */
        public Builder loader(Repository.Loader<String, Tool> loader) {
            this.loader = loader;
            return this;
        }

        @Override
        public ToolRepository build() {
            Objects.requireNonNull(client, "DashscopeClient is required");
            return new ToolRepository(this);
        }

    }

}
