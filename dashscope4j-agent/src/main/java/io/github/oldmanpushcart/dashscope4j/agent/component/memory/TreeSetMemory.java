package io.github.oldmanpushcart.dashscope4j.agent.component.memory;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.MessageCodec;
import io.github.oldmanpushcart.dashscope4j.client.util.LocalTokenizerUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel.Mode.MULTIMODAL;
import static java.util.Objects.requireNonNull;

/**
 * 记忆体实现（TreeSet）
 */
public class TreeSetMemory implements Memory {

    private final AtomicLong identityGen = new AtomicLong(1000);
    private final Map<Long, FragmentDO> fragmentDOMap = new TreeMap<>(Comparator.reverseOrder());

    @Override
    public List<Fragment> recall(String conversationId, Condition condition) {

        requireNonNull(conversationId, "conversationId is required");
        requireNonNull(condition, "condition is required");

        return fragmentDOMap.entrySet().stream()
                .filter(filterByConversationId(conversationId))
                .filter(filterByCondition(condition))
                .map(Map.Entry::getValue)
                .map(FragmentDO::toFragment)
                .collect(Collectors.toList());

    }

    private Predicate<Map.Entry<Long, FragmentDO>> filterByConversationId(String conversationId) {
        return entry -> Objects.equals(conversationId, entry.getValue().getConversationId());
    }

    private Predicate<Map.Entry<Long, FragmentDO>> filterByCondition(Condition condition) {
        final AtomicInteger countRef = new AtomicInteger();
        final AtomicInteger tokensRef = new AtomicInteger();
        final Instant lowerBound = Optional.ofNullable(condition.maxDuration())
                .map(maxDuration -> Instant.now().minus(condition.maxDuration()))
                .orElse(null);
        return ((Predicate<Map.Entry<Long, FragmentDO>>) v -> true)
                .and(entry -> null == lowerBound || entry.getValue().getCreatedAt().isAfter(lowerBound))
                .and(entry -> null == condition.maxTokens() || tokensRef.addAndGet(entry.getValue().getTokens()) <= condition.maxTokens())
                .and(entry -> null == condition.maxCount() || countRef.incrementAndGet() <= condition.maxCount());
    }

    @Override
    public synchronized long persist(Fragment fragment) {

        requireNonNull(fragment, "fragment is required");
        requireNonNull(fragment.conversationId(), "fragment.conversationId is required");
        requireNonNull(fragment.requestMessage(), "fragment.requestMessage is required");
        requireNonNull(fragment.responseMessage(), "fragment.responseMessage is required");

        // update
        if (fragment.fragmentId() != null) {

            // 已存在对象
            final FragmentDO existedDO = fragmentDOMap.get(fragment.fragmentId());
            if (null == existedDO) {
                throw new NoSuchElementException("Fragment not found! fragmentId: " + fragment.fragmentId());
            }

            // 待更新对象
            final FragmentDO updatedDO = FragmentDO.fromFragment(fragment);

            // 更新已存在的对象
            existedDO
                    .setUpdatedAt(Instant.now())
                    .setTokens(updatedDO.getTokens())
                    .setRequestMessageJson(updatedDO.getRequestMessageJson())
                    .setResponseMessageJson(updatedDO.getResponseMessageJson());

            // 更新原有对象
            fragment
                    .createdAt(existedDO.getCreatedAt())
                    .updatedAt(existedDO.getUpdatedAt());

            return fragment.fragmentId();
        }

        // create
        else {

            // 创建新实体
            final FragmentDO createdDO = FragmentDO.fromFragment(fragment)
                    .setFragmentId(identityGen.incrementAndGet())
                    .setCreatedAt(Instant.now())
                    .setUpdatedAt(Instant.now());

            // 持久化实体
            fragmentDOMap.put(createdDO.getFragmentId(), createdDO);

            // 更新原有对象
            fragment
                    .createdAt(createdDO.getCreatedAt())
                    .updatedAt(createdDO.getUpdatedAt());

            return createdDO.getFragmentId();
        }

    }

    /**
     * 记忆片段实体
     */
    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Accessors(chain = true)
    private static class FragmentDO implements Comparable<FragmentDO> {

        @EqualsAndHashCode.Include
        private Long fragmentId;

        private String conversationId;
        private Integer tokens;
        private String requestMessageJson;
        private String responseMessageJson;
        private Instant createdAt;
        private Instant updatedAt;

        @Override
        public int compareTo(FragmentDO o) {
            return Long.compare(this.fragmentId, o.fragmentId);
        }

        /**
         * @return 转换为记忆片段
         */
        public Fragment toFragment() {
            return new Fragment()
                    .fragmentId(this.fragmentId)
                    .conversationId(this.conversationId)
                    .requestMessage(MessageCodec.decode(this.requestMessageJson))
                    .responseMessage(MessageCodec.decode(this.responseMessageJson))
                    .createdAt(this.createdAt)
                    .updatedAt(this.updatedAt);
        }

        /**
         * 从记忆片段转换为实体
         *
         * @param fragment 记忆片段
         * @return 记忆片段实体
         */
        public static FragmentDO fromFragment(Fragment fragment) {
            final int tokens = LocalTokenizerUtils
                    .encode(Arrays.asList(
                            fragment.requestMessage(),
                            fragment.responseMessage()
                    ))
                    .size();
            final String requestMessageJson = MessageCodec.encodeToJson(MULTIMODAL, fragment.requestMessage());
            final String responseMessageJson = MessageCodec.encodeToJson(MULTIMODAL, fragment.responseMessage());
            return new FragmentDO()
                    .setFragmentId(fragment.fragmentId())
                    .setConversationId(fragment.conversationId())
                    .setTokens(tokens)
                    .setRequestMessageJson(requestMessageJson)
                    .setResponseMessageJson(responseMessageJson)
                    .setCreatedAt(fragment.createdAt())
                    .setUpdatedAt(fragment.updatedAt());
        }

    }

}
