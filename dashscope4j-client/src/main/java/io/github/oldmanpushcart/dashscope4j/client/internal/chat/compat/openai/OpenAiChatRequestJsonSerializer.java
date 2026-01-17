package io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.*;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.content.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class OpenAiChatRequestJsonSerializer extends JsonSerializer<OpenAiChatRequest> {

    @Override
    public void serialize(OpenAiChatRequest request, JsonGenerator generator, SerializerProvider provider) throws IOException {
        final var requestPojo = new HashMap<>() {{
            put("model", request.model());
            put("messages", serializeMessageList(request.messages()));
            request.parameters().forEach(this::put);
            if (!request.tools().isEmpty()) {
                put("tools", request.tools());
            }
        }};
        generator.writeObject(requestPojo);
    }

    private List<Object> serializeMessageList(List<Message> messages) {
        return messages.stream()
                .map(this::serializeMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    private Object serializeMessage(Message message) {

        // system
        if (message instanceof SystemMessage system) {
            return new HashMap<>() {{
                put("role", system.role());
                put("content", serializeContentList(system.contents()));
            }};
        }

        // tool
        else if (message instanceof ToolMessage tool) {
            return new HashMap<>() {{
                put("role", tool.role());
                put("content", tool.content());
                put("tool_call_id", tool.id());
            }};
        }

        // user
        else if (message instanceof UserMessage user) {
            return new HashMap<>() {{
                put("role", user.role());
                put("content", serializeContentList(user.contents()));
            }};
        }

        // assistant
        else if (message instanceof AssistantMessage assistant) {
            return new HashMap<>() {{
                put("role", assistant.role());
                put("content", serializeContentList(assistant.contents()));
                put("tool_calls", assistant.calls());
                put("reasoning_content", assistant.reasoningContent());
                put("partial", assistant.partial());
            }};
        }
        return null;
    }

    private List<Object> serializeContentList(List<Content> contents) {
        return contents.stream()
                .map(this::serializeContent)
                .filter(Objects::nonNull)
                .toList();
    }

    private Object serializeContent(Content content) {

        // text
        if (content instanceof TextContent text) {
            return new HashMap<>() {{
                put("type", "text");
                put("cache_control", text.cacheControl());
                put("text", text.text());
            }};
        }

        // image
        else if (content instanceof ImageContent image) {
            return new HashMap<>() {{
                put("type", "image_url");
                put("image_url", new HashMap<>() {{
                    put("url", image.image());
                }});
                put("cache_control", image.cacheControl());
            }};
        }

        // audio
        else if (content instanceof AudioContent audio) {
            return new HashMap<>() {{
                put("type", "input_audio");
                put("input_audio", new HashMap<>() {{
                    put("data", audio.audio());
                }});
                put("cache_control", audio.cacheControl());
            }};
        }

        // video
        else if (content instanceof VideoContent video) {
            return new HashMap<>() {{
                if (video.resources().size() == 1) {
                    put("type", "video_url");
                    put("video_url", new HashMap<>() {{
                        put("video_url", video.resources().get(0));
                    }});
                } else {
                    put("type", "video");
                    put("video", video.resources());
                }
                put("cache_control", video.cacheControl());
                put("fps", video.fps());
                put("min_pixels", video.minPixels());
                put("max_pixels", video.maxPixels());
                put("total_pixels", video.totalPixels());
            }};
        }
        return null;
    }

}
