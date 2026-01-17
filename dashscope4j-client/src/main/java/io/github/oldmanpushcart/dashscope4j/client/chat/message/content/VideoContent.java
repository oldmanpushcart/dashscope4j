package io.github.oldmanpushcart.dashscope4j.client.chat.message.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class VideoContent implements Content {

    private final List<URI> resources;
    private final Float fps;
    private final Integer minPixels;
    private final Integer maxPixels;
    private final Integer totalPixels;
    private final CacheControl cacheControl;

    @JsonCreator
    private VideoContent(

            @JsonProperty("video")
            @JsonDeserialize(using = URIListJsonDeserializer.class)
            List<URI> resources,

            @JsonProperty("fps")
            Float fps,

            @JsonProperty("min_pixels")
            Integer minPixels,

            @JsonProperty("max_pixels")
            Integer maxPixels,

            @JsonProperty("total_pixels")
            Integer totalPixels,

            @JsonProperty("cache_control")
            CacheControl cacheControl

    ) {
        this.resources = resources;
        this.fps = fps;
        this.minPixels = minPixels;
        this.maxPixels = maxPixels;
        this.totalPixels = totalPixels;
        this.cacheControl = cacheControl;
    }

    @JsonProperty("video")
    @JsonSerialize(using = URIListJsonSerializer.class)
    public List<URI> resources() {
        return resources;
    }

    @JsonProperty("fps")
    public Float fps() {
        return fps;
    }

    @JsonProperty("min_pixels")
    public Integer minPixels() {
        return minPixels;
    }

    @JsonProperty("max_pixels")
    public Integer maxPixels() {
        return maxPixels;
    }

    @JsonProperty("total_pixels")
    public Integer totalPixels() {
        return totalPixels;
    }

    @Override
    public CacheControl cacheControl() {
        return cacheControl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VideoContent content) {
        return new Builder(content);
    }

    public static class Builder implements Buildable<VideoContent, Builder> {

        private Float fps;
        private Integer minPixels;
        private Integer maxPixels;
        private Integer totalPixels;
        private CacheControl cacheControl;
        private final List<URI> resources = new ArrayList<>();

        public Builder() {
        }

        public Builder(VideoContent content) {
            this.fps = content.fps();
            this.minPixels = content.minPixels();
            this.maxPixels = content.maxPixels();
            this.totalPixels = content.totalPixels();
            this.cacheControl = content.cacheControl();
            this.resources.addAll(content.resources());
        }

        public Builder fps(Float fps) {
            this.fps = fps;
            return this;
        }

        public Builder minPixels(Integer minPixels) {
            this.minPixels = minPixels;
            return this;
        }

        public Builder maxPixels(Integer maxPixels) {
            this.maxPixels = maxPixels;
            return this;
        }

        public Builder totalPixels(Integer totalPixels) {
            this.totalPixels = totalPixels;
            return this;
        }

        public Builder resources(List<URI> resources) {
            this.resources.clear();
            this.resources.addAll(resources);
            return this;
        }

        public Builder addResource(URI resource) {
            this.resources.add(resource);
            return this;
        }

        public Builder addResources(List<URI> resources) {
            this.resources.addAll(resources);
            return this;
        }

        public Builder cacheControl(CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        @Override
        public VideoContent build() {
            return new VideoContent(resources, fps, minPixels, maxPixels, totalPixels, cacheControl);
        }

    }

    private static class URIListJsonSerializer extends JsonSerializer<List<URI>> {

        @Override
        public void serialize(List<URI> uris, JsonGenerator gen, SerializerProvider provider) throws IOException {

            if (uris.isEmpty()) {
                gen.writeNull();
                return;
            }

            if (uris.size() == 1) {
                provider.defaultSerializeValue(uris.get(0), gen);
            } else {
                gen.writeStartArray();
                for (URI uri : uris) {
                    provider.defaultSerializeValue(uri, gen);
                }
                gen.writeEndArray();
            }

        }

    }

    private static class URIListJsonDeserializer extends JsonDeserializer<List<URI>> {

        @Override
        public List<URI> deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {

            final var mapper = parser.getCodec();
            final var node = mapper.<JsonNode>readTree(parser);
            if (null == node || node.isNull()) {
                return List.of();
            }

            if (node.isTextual()) {
                final var uri = mapper.treeToValue(node, URI.class);
                return List.of(uri);
            }

            if (node.isArray()) {
                final var uris = new ArrayList<URI>();
                for (final var item : node) {
                    final var uri = mapper.treeToValue(item, URI.class);
                    uris.add(uri);
                }
                return uris;
            }

            return List.of();
        }

    }

}
