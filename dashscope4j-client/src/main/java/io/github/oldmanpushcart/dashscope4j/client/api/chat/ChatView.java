package io.github.oldmanpushcart.dashscope4j.client.api.chat;

public interface ChatView {

    interface OpenAI {

    }

    interface Dashscope {

    }

    interface Text {
    }

    interface Multimodal {
    }

    interface OpenAIText extends OpenAI, Text{

    }

    interface OpenAIMultimodal extends OpenAI, Multimodal{

    }

    interface DashscopeText extends Dashscope, Text{

    }

    interface DashscopeMultimodal extends Dashscope, Multimodal {

    }

}
