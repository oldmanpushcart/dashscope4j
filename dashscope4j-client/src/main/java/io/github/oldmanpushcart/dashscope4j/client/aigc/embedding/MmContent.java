package io.github.oldmanpushcart.dashscope4j.client.aigc.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.net.URI;
import java.util.List;

public interface MmContent {

    static MmContent ofText(String text) {
        return new Text(text);
    }

    static MmContent ofImage(URI imageURI) {
        return new Image(imageURI);
    }

    static MmContent ofImages(List<URI> imageURIs) {
        return new ImageList(imageURIs);
    }

    static MmContent ofVideo(URI videoURI) {
        return new Video(videoURI);
    }

    static MmContent ofComplex(String text, URI imageURI, URI videoURI) {
        return new Complex(text, imageURI, videoURI);
    }

    record Text(

            @JsonProperty("text")
            String text

    ) implements MmContent {

    }

    record Image(

            @JsonProperty("image")
            URI uri

    ) implements MmContent {

    }

    record ImageList(

            @JsonProperty("multi_images")
            List<URI> uris

    ) implements MmContent {

    }

    record Video(

            @JsonProperty("video")
            URI uri

    ) implements MmContent {

    }

    record Complex(

            @JsonProperty("text")
            String text,

            @JsonProperty("image")
            URI image,

            @JsonProperty("video")
            URI video

    ) implements MmContent {

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Complex complex) {
            return new Builder(complex);
        }

        public static class Builder implements Buildable<Complex, Builder> {

            private String text;
            private URI image;
            private URI video;

            public Builder() {

            }

            public Builder(Complex complex) {
                this.text = complex.text;
                this.image = complex.image;
                this.video = complex.video;
            }

            public Builder text(String text) {
                this.text = text;
                return this;
            }

            public Builder image(URI image) {
                this.image = image;
                return this;
            }

            public Builder video(URI video) {
                this.video = video;
                return this;
            }

            @Override
            public Complex build() {
                return new Complex(text, image, video);
            }

        }

    }

}
