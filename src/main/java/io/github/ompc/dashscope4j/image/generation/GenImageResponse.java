package io.github.ompc.dashscope4j.image.generation;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.internal.algo.AlgoResponse;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

import java.net.URI;
import java.util.List;

/**
 * 文生图应答
 */
public interface GenImageResponse extends AlgoResponse<GenImageResponse.Output> {

    interface Output extends ApiResponse.Output {

        List<Item> results();

    }

    interface Item {

        Ret ret();
        URI image();

    }

}
