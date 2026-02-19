package io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.remote;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RemoteTokenizer implements Tokenizer {

    private final DashscopeClient client;
    private final ChatModel model;

    public RemoteTokenizer(DashscopeClient client, ChatModel model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(String text) {

        final var gaModel = GeneralAigcModel.newBuilder()
                .name(model.name())
                .path("/api/v1/tokenizer")
                .build();

        final var request = AigcRequest.newBuilder(gaModel)
                .input(Map.of("messages", List.of(
                        Map.of(
                                "role", Message.Role.USER,
                                "content", text.trim()
                        )
                )))
                .build();

        return client.async(request)
                .thenApply(response -> {

                    final var outputMap = response.output();

                    //noinspection unchecked
                    final var tokenIds = (List<Integer>) outputMap.get("token_ids");

                    //noinspection unchecked
                    final var tokens = (List<String>) outputMap.get("tokens");

                    if (tokenIds.size() != tokens.size()) {
                        throw new IllegalArgumentException("token-ids and tokens size not equal!");
                    }

                    return IntStream.range(0, tokenIds.size())
                            .mapToObj(index ->
                                    new AbstractMap.SimpleEntry<>(
                                            tokenIds.get(index),
                                            tokens.get(index)
                                    ))
                            .collect(Collectors.toList());
                });
    }

}
