package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.TextContent;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class ContentListJsonSerializer extends JsonSerializer<List<Content>> {

    @Override
    public void serialize(List<Content> contents, JsonGenerator gen, SerializerProvider provider) throws IOException {

        final var isTextOnly = contents.stream()
                .allMatch(content ->
                        content instanceof TextContent
                                && Objects.isNull(content.cacheControl()));

        if (isTextOnly) {
            gen.writeString(contents.stream()
                    .map(TextContent.class::cast)
                    .map(TextContent::text)
                    .collect(Collectors.joining()));
            return;
        }

        gen.writeStartArray();
        for (final var content : contents) {
            provider.defaultSerializeValue(content, gen);
        }
        gen.writeEndArray();

    }

}
