package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.VideoContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ContentListJsonDeserializer extends JsonDeserializer<List<Content>> {

    @Override
    public List<Content> deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        final var mapper = parser.getCodec();
        final var node = mapper.<JsonNode>readTree(parser);

        // text only
        if (null != node && node.isTextual()) {
            final var content = Content.text(node.asText());
            return List.of(content);
        }

        if (null != node && node.isArray()) {
            final var contents = new ArrayList<Content>();
            for (final var item : node) {
                final var content = deserializeContent(item, mapper);
                if (null != content) {
                    contents.add(content);
                }
            }
            return contents;
        }

        return List.of();
    }

    private Content deserializeContent(JsonNode node, ObjectCodec mapper) throws IOException {

        // TEXT
        if (node.has("text")) {
            return mapper.treeToValue(node, TextContent.class);
        }

        // IMAGE
        else if (node.has("image")) {
            return mapper.treeToValue(node, ImageContent.class);
        }

        // VIDEO
        else if (node.has("video")) {
            return mapper.treeToValue(node, VideoContent.class);
        }

        // UNKNOWN
        else {
            return null;
        }

    }

}
