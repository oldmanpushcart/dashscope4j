package io.github.ompc.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.internal.algo.AlgoRequest;

import java.net.http.HttpRequest;

public final class GenImageRequest extends AlgoRequest<GenImageModel, GenImageResponse> {

    private GenImageRequest(Builder builder, Input input) {
        super(builder, GenImageResponse.class, input);
    }

    public record Input(
            @JsonProperty("prompt")
            String prompt,
            @JsonProperty("negative")
            String negative
    ) {

    }

    @Override
    protected HttpRequest newHttpRequest() {
        final var request = super.newHttpRequest();
        return HttpRequest.newBuilder(request, (k,v)->true)
                .header("X-DashScope-Async", "enable")
                .build();
    }

    public static class Builder extends AlgoRequest.Builder<GenImageModel, GenImageRequest, Builder> {
        private String prompt;
        private String negative;

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder negative(String negative) {
            this.negative = negative;
            return this;
        }

        @Override
        public GenImageRequest build() {
            return new GenImageRequest(this, new Input(prompt, negative));
        }

    }


    public enum Style {
        @JsonProperty("auto")
        AUTO,
        @JsonProperty("3d_cartoon")
        CARTOON_3D,
        @JsonProperty("anime")
        ANIME,
        @JsonProperty("oil_painting")
        OIL_PAINTING,
        @JsonProperty("watercolor")
        WATERCOLOR,
        @JsonProperty("sketch")
        SKETCH,
        @JsonProperty("chinese_painting")
        CHINESE_PAINTING,
        @JsonProperty("flat_illustration")
        FLAT_ILLUSTRATION
    }

    public enum Size {
        @JsonProperty("1024*1024")
        S_1024_1024,
        @JsonProperty("720*1280")
        S_720_1280,
        @JsonProperty("1280*720")
        S_1280_720;
    }

}
