package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * OMNI-REALTIME 数据交换接口
 */
public interface OmniRealtimeEmitter extends Realtime.Emitter<ClientEvent> {

    /**
     * 获取会话
     *
     * @return 会话
     */
    OmniRealtimeSession session();

    /**
     * 手动 VAD
     */
    interface ManualVad extends OmniRealtimeEmitter {

        /**
         * 创建一个新地提交操作
         * <p>
         * 你可以进行图片和音频的提交。需要注意的是，图像提交必需要在音频之后进行
         * </p>
         *
         * @return 提交操作
         */
        CompletionStage<InputOp> newInput();

        /**
         * 提交操作
         */
        interface InputOp {

            /**
             * 提交图片
             *
             * @param image 图片
             * @return 提交操作
             */
            CompletionStage<InputOp> image(ByteBuffer image);

            /**
             * 提交音频
             *
             * @param buffer 音频数据
             * @return 提交操作
             */
            CompletionStage<InputOp> audio(ByteBuffer buffer);

            /**
             * 清空提交
             *
             * @return 提交操作
             */
            CompletionStage<InputOp> clear();

            /**
             * 提交
             * <p>
             * 输入被提交之后将无法继续输入，开始转入到响应操作。在响应输入阶段进行响应提交、取消的操作。
             * </p>
             *
             * @return 响应操作
             */
            CompletionStage<ResponseOp> commit();

            /**
             * 取消提交
             * <p>
             * 输入被取消之后将无法继续输入，响应输入将无法继续进行。
             * </p>
             *
             * @return 取消操作
             */
            CompletionStage<Void> cancel();

        }

        /**
         * 响应操作
         */
        interface ResponseOp {

            /**
             * 创建一个响应
             *
             * @return 创建结果
             */
            CompletableFuture<Void> create();

        }

    }

    /**
     * 服务器 VAD
     */
    interface ServerVad extends OmniRealtimeEmitter {

        /**
         * 输入图片
         *
         * @param image 图片
         * @return 输入结果
         */
        CompletionStage<Void> image(ByteBuffer image);

        /**
         * 输入音频
         *
         * @param buffer 音频数据
         * @return 输入结果
         */
        CompletionStage<Void> audio(ByteBuffer buffer);

    }

}
