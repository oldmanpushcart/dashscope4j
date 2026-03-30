package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.BaseRepository;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.storage.InMemoryStorage;
import io.github.oldmanpushcart.dashscope4j.agent.storage.Storage;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.mutableCopy;

/**
 * 工具仓库
 */
public class ToolRepository extends BaseRepository<String, Tool> {

    public static final String NAME = "tool";

    protected ToolRepository(Builder builder) {
        super(
                NAME,
                Objects.requireNonNullElseGet(builder.indexer, () -> new ToolIndexer(builder.client)),
                Objects.requireNonNullElseGet(builder.storage, InMemoryStorage::new),
                CommonUtils.unmodifiableCopy(builder.loaders),
                builder.blocking
        );
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolRepository, Builder> {

        private DashscopeClient client;
        private Storage<String, Tool> storage;
        private Repository.Indexer<String, Tool> indexer;
        private List<Repository.Loader<String, Tool>> loaders;
        private boolean blocking;

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

        public Builder loaders(List<Repository.Loader<String, Tool>> loaders) {
            this.loaders = loaders;
            return this;
        }

        public Builder loaders(UnaryOperator<List<Repository.Loader<String, Tool>>> operator) {
            this.loaders = operator.apply(mutableCopy(this.loaders));
            return this;
        }

        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        @Override
        public ToolRepository build() {
            Objects.requireNonNull(client, "DashscopeClient is required");
            return new ToolRepository(this);
        }

    }

}
