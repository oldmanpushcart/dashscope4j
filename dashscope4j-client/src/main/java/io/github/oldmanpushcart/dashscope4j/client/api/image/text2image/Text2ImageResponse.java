package io.github.oldmanpushcart.dashscope4j.client.api.image.text2image;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import java.net.URI;
import java.util.List;

public class Text2ImageResponse extends AlgoResponse<Text2ImageResponse.Output> {

    private final Output output;

    @JsonCreator
    public Text2ImageResponse(

            @JacksonInject("dashscope/request")
            Text2ImageRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(request, uuid, code, desc, usage);
        this.output = output;
    }

    @Override
    public Output output() {
        return output;
    }

    public static class Output {

        private final List<Item> items;

        @JsonCreator
        public Output(

                @JsonProperty("results")
                List<Item> items

        ) {
            this.items = items;
        }

        public List<Item> items() {
            return items;
        }

    }

    public static class Item extends Ret {

        private final URI image;

        @JsonCreator
        public Item(

                @JsonProperty("code")
                String code,

                @JsonProperty("message")
                String desc,

                @JsonProperty("url")
                URI image

        ) {
            super(code, desc);
            this.image = image;
        }

        public URI image() {
            return image;
        }

    }

}
