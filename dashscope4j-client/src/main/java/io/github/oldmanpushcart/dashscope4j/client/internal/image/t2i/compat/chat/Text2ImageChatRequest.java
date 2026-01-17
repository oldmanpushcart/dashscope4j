package io.github.oldmanpushcart.dashscope4j.client.internal.image.t2i.compat.chat;

import io.github.oldmanpushcart.dashscope4j.client.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageModel;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;

import java.util.HashMap;
import java.util.List;

public class Text2ImageChatRequest extends AlgoRequest<Text2ImageModel, Text2ImageChatResponse> {

    private final List<Message> messages;

    protected Text2ImageChatRequest(Text2ImageModel model, List<Message> messages, Parameters parameters) {
        super(Text2ImageChatResponse.class, model, parameters);
        this.messages = messages;
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{
            put("messages", messages);
        }};
    }

    public static Text2ImageChatRequest of(Text2ImageRequest request) {

        final var parameters = new Parameters()
                .merge(request.parameters());

        if (CommonUtils.isNotBlankString(request.negative())) {
            parameters.append("negative_prompt", request.negative());
        }

        final var messages = List.<Message>of(
                Message.user(request.prompt())
        );

        return new Text2ImageChatRequest(
                request.model(),
                messages,
                parameters
        );
    }

}
