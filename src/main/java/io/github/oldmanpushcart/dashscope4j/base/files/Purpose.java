package io.github.oldmanpushcart.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Purpose {

    @JsonProperty("file-extract")
    FILE_EXTRACT,

    @JsonProperty("batch")
    BATCH

}
