package io.github.oldmanpushcart.dashscope4j.image.generation;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.net.URI;
import java.util.List;

/**
 * 文生图应答
 */
public interface GenImageResponse extends AlgoResponse<GenImageResponse.Output> {

    /**
     * 文生图应答数据
     */
    interface Output extends ApiResponse.Output {

        /**
         * 获取结果
         *
         * @return 结果
         */
        List<Item> results();

    }

    /**
     * 结果项
     */
    interface Item {

        /**
         * 获取返回
         *
         * @return 返回
         */
        Ret ret();

        /**
         * 获取图片
         *
         * @return 图片
         */
        URI image();

    }

}
