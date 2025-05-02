package io.github.oldmanpushcart.dashscope4j.agent.memory;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.LocalTokenizerUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.MessageCodec;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel.Mode.MULTIMODAL;

/**
 * TreeSet 记忆体
 */
public class TreeSetMemory implements Memory {

    private final AtomicLong fragmentIdGen = new AtomicLong(1000);
    private final Set<FragmentDO> fragments = new TreeSet<>();
    private final Integer maxTokens;
    private final Integer maxCount;
    private final Duration maxDuration;

    private TreeSetMemory(Builder builder) {
        this.maxTokens = builder.maxTokens;
        this.maxCount = builder.maxCount;
        this.maxDuration = builder.maxDuration;
    }

    @Override
    public Long newestFragmentId(String sessionId) {
        return fragments.stream()
                .filter(fragmentDO -> Objects.equals(sessionId, fragmentDO.getSessionId()))
                .map(FragmentDO::getFragmentId)
                .findFirst()
                .orElse(-1L);
    }

    @Override
    public List<Fragment> recall(String sessionId, long olderThenFragmentId, long newerThenFragmentId) {
        return fragments.stream()
                .filter(fragmentDO -> Objects.equals(sessionId, fragmentDO.getSessionId()))
                .filter(fragmentDO -> filterByFragmentIdRange(fragmentDO, olderThenFragmentId, newerThenFragmentId))
                .filter(this::filterByCondition)
                .map(FragmentDO::toFragment)
                .collect(Collectors.toList());
    }

    private boolean filterByFragmentIdRange(FragmentDO fragmentDO, long older, long newer) {
        return ((Predicate<FragmentDO>) v -> true)
                .and(fragment -> fragment.getFragmentId() > older)
                .and(fragment -> fragment.getFragmentId() < newer)
                .test(fragmentDO);
    }

    private boolean filterByCondition(FragmentDO fragmentDO) {
        final AtomicInteger hitTokensRef = new AtomicInteger();
        final AtomicInteger hitCountRef = new AtomicInteger();
        return ((Predicate<FragmentDO>) v -> true)
                .and(fragment -> null == maxDuration || fragment.getCreatedAt().isAfter(Instant.now().minus(maxDuration)))
                .and(fragment -> null == maxTokens || hitTokensRef.addAndGet(fragment.getTokens()) <= maxTokens)
                .and(fragment -> null == maxCount || hitCountRef.incrementAndGet() <= maxCount)
                .test(fragmentDO);
    }

    @Override
    public long persist(Fragment fragment) {

        // update
        if (fragment.fragmentId() != null) {
            fragments.add(FragmentDO.fromFragment(fragment));
            return fragment.fragmentId();
        }

        // create
        else {
            final FragmentDO fragmentDO = FragmentDO.fromFragment(fragment)
                    .setFragmentId(fragmentIdGen.incrementAndGet())
                    .setCreatedAt(Instant.now())
                    .setUpdatedAt(Instant.now());
            fragments.add(fragmentDO);
            return fragmentDO.getFragmentId();
        }

    }

    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Accessors(chain = true)
    private static class FragmentDO implements Comparable<FragmentDO> {

        @EqualsAndHashCode.Include
        private Long fragmentId;

        private String sessionId;
        private Integer tokens;
        private String requestMessageJson;
        private String responseMessageJson;
        private Instant createdAt;
        private Instant updatedAt;

        @Override
        public int compareTo(@NotNull FragmentDO o) {
            return Long.compare(this.fragmentId, o.fragmentId);
        }

        public Memory.Fragment toFragment() {
            return new Memory.Fragment()
                    .fragmentId(this.fragmentId)
                    .sessionId(this.sessionId)
                    .requestMessage(MessageCodec.decode(this.requestMessageJson))
                    .responseMessage(MessageCodec.decode(this.responseMessageJson))
                    .createdAt(this.createdAt)
                    .updatedAt(this.updatedAt);
        }

        public static FragmentDO fromFragment(Memory.Fragment fragment) {
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
                    .setSessionId(fragment.sessionId())
                    .setTokens(tokens)
                    .setRequestMessageJson(requestMessageJson)
                    .setResponseMessageJson(responseMessageJson)
                    .setCreatedAt(fragment.createdAt())
                    .setUpdatedAt(fragment.updatedAt());
        }

    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<TreeSetMemory, Builder> {

        private Integer maxTokens = 50000;
        private Integer maxCount = 1024;
        private Duration maxDuration = Duration.ofHours(1);

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder maxCount(Integer maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }

        @Override
        public TreeSetMemory build() {
            return new TreeSetMemory(this);
        }

    }

}
