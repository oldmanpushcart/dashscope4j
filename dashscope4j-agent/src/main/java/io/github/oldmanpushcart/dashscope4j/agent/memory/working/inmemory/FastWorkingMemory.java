package io.github.oldmanpushcart.dashscope4j.agent.memory.working.inmemory;

import io.github.oldmanpushcart.dashscope4j.agent.memory.working.WorkingMemory;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FastWorkingMemory implements WorkingMemory {

    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    @Override
    public WorkingMemory.Session session(String sessionId) {
        return sessionMap.computeIfAbsent(sessionId, Session::new);
    }

    record Entry(
            long entryId,
            String sessionId,
            Message inbound,
            Message outbound,
            int tokens,
            Instant occurAt
    ) implements WorkingMemory.Entry {

    }

    static class Session implements WorkingMemory.Session {

        private final String sessionId;
        private final Map<Long, Entry> entryMap = new java.util.TreeMap<>(); // 使用TreeMap自动按entryId排序

        Session(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public List<WorkingMemory.Entry> recall(RecallOptions options) {
            final var now = Instant.now();
            int recordCnt = 0;
            int tokensCnt = 0;
            final var entries = new ArrayList<WorkingMemory.Entry>();
            
            // 单次循环处理所有过滤条件
            for (final var entry : entryMap.values()) {

                // 检查时间限制
                if (options.maxAge() != null 
                        && now.isAfter(entry.occurAt().plus(options.maxAge()))) {
                    break;
                }

                // 检查最大条目数限制
                if (options.maxEntries() != null 
                        && recordCnt > options.maxEntries()) {
                    break;
                }

                // 检查最大token数限制
                if (options.maxTokens() != null 
                        && tokensCnt + entry.tokens() > options.maxTokens()) {
                    break;
                }

                entries.add(entry);
                recordCnt++;
                tokensCnt += entry.tokens();
            }

            return entries;
        }

        @Override
        public long remember(Message inbound, Message outbound, int tokens) {
            final var entryId = System.nanoTime();
            final var entry = new Entry(
                    entryId,
                    sessionId,
                    inbound,
                    outbound,
                    tokens,
                    Instant.now()
            );
            entryMap.put(entryId, entry);
            return entryId;
        }

        @Override
        public boolean forgot(long entryId) {
            return entryMap.remove(entryId) != null;
        }

    }

}
