package io.github.ompc.dashscope4j.chat;

import io.github.ompc.dashscope4j.Model;

import java.net.URI;

/**
 * 对话模型
 */
public class ChatModel extends Model {

    /**
     * 构造对话模型
     *
     * @param name   名称
     * @param remote 远程地址
     */
    public ChatModel(String name, URI remote) {
        super(name, remote);
    }

    /**
     * QWEN-TURBO
     * <p>通义千问超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static ChatModel QWEN_TURBO = new ChatModel(
            "qwen-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    );

}
