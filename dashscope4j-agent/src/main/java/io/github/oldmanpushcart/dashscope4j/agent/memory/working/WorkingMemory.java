package io.github.oldmanpushcart.dashscope4j.agent.memory.working;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 短期记忆
 */
public interface WorkingMemory {

    interface Entry extends Comparable<Entry> {

        long entryId();

        String sessionId();

        Message inbound();

        Message outbound();

        int tokens();

        Instant occurAt();

        @Override
        default int compareTo(Entry o) {
            return Long.compare(this.entryId(), o.entryId());
        }

    }

    interface Session {

        String sessionId();

        List<Entry> recall(RecallOptions options);

        long remember(Message inbound, Message outbound, int tokens);

        boolean forgot(long entryId);

    }

    Session session(String sessionId);

    class RecallOptions {

        private Integer maxTokens;
        private Integer maxEntries;
        private Duration maxAge;

        public Integer maxTokens() {
            return maxTokens;
        }

        public RecallOptions maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Integer maxEntries() {
            return maxEntries;
        }

        public RecallOptions maxEntries(Integer maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Duration maxAge() {
            return maxAge;
        }

        public RecallOptions maxAge(Duration maxAge) {
            this.maxAge = maxAge;
            return this;
        }

    }

}
