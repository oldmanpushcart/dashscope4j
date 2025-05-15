package io.github.oldmanpushcart.dashscope4j.agent.plugin.memory;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.MessageCodec;
import io.github.oldmanpushcart.dashscope4j.client.util.LocalTokenizerUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel.Mode.MULTIMODAL;

/**
 * 记忆体实现（TreeSet）
 */
public class TreeSetMemory implements Memory {

    private final AtomicLong identityGen = new AtomicLong(1000);
    private final Set<FragmentDO> fragmentDOs = new TreeSet<>();

    @Override
    public List<Fragment> recall(String conversationId, Condition condition) {
        return fragmentDOs.stream()
                .filter(filteringByConversationId(conversationId))
                .filter(filteringByCondition(condition))
                .map(FragmentDO::toFragment)
                .collect(Collectors.toList());
    }

    private Predicate<FragmentDO> filteringByConversationId(String conversationId) {
        return fragmentDO -> Objects.equals(conversationId, fragmentDO.getConversationId());
    }

    private Predicate<FragmentDO> filteringByCondition(Condition condition) {
        final AtomicInteger countRef = new AtomicInteger();
        final AtomicInteger tokensRef = new AtomicInteger();
        final Instant lowerBound = Optional.ofNullable(condition.maxDuration())
                .map(maxDuration -> Instant.now().minus(condition.maxDuration()))
                .orElse(null);
        return ((Predicate<FragmentDO>) v -> true)
                .and(fragmentDO -> null == lowerBound || fragmentDO.getCreatedAt().isAfter(lowerBound))
                .and(fragmentDO -> null == condition.maxTokens() || tokensRef.addAndGet(fragmentDO.getTokens()) <= condition.maxTokens())
                .and(fragmentDO -> null == condition.maxCount() || countRef.incrementAndGet() <= condition.maxCount());
    }

    @Override
    public long persist(Fragment fragment) {

        // update
        if (fragment.fragmentId() != null) {
            fragmentDOs.add(FragmentDO.fromFragment(fragment));
            return fragment.fragmentId();
        }

        // create
        else {
            final FragmentDO fragmentDO = FragmentDO.fromFragment(fragment)
                    .setFragmentId(identityGen.incrementAndGet())
                    .setCreatedAt(Instant.now())
                    .setUpdatedAt(Instant.now());
            fragmentDOs.add(fragmentDO);
            return fragmentDO.getFragmentId();
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
        public int compareTo(@NotNull FragmentDO o) {
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
