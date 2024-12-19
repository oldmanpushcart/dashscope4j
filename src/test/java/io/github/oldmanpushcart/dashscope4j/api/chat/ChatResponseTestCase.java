package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChatResponseTestCase {

    @Test
    public void test$chat$response$message_format() {
        final String json = "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":[{\"text\":\"图片中有五个男孩。\"}]}}]},\"usage\":{\"output_tokens\":6,\"input_tokens\":1277,\"image_tokens\":1249},\"request_id\":\"2fcd6ae2-6c04-9e3b-bb6c-1c50f91b49eb\"}";
        final ChatResponse response = JacksonUtils.toObject(json, ChatResponse.class);
        Assertions.assertEquals("2fcd6ae2-6c04-9e3b-bb6c-1c50f91b49eb", response.uuid());
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(6, response.usage().total(item-> "output_tokens".equals(item.name())));
        Assertions.assertEquals(1277, response.usage().total(item-> "input_tokens".equals(item.name())));
        Assertions.assertEquals(1249, response.usage().total(item-> "image_tokens".equals(item.name())));
        Assertions.assertEquals(2532, response.usage().total());
        Assertions.assertEquals(1, response.output().choices().size());
        Assertions.assertEquals(ChatResponse.Finish.NORMAL, response.output().best().finish());
        Assertions.assertEquals(Message.Role.AI, response.output().best().message().role());
        Assertions.assertEquals("图片中有五个男孩。", response.output().best().message().text());
    }

    @Test
    public void test$chat$response$text_format() {
        final String json = "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"我是通义千问，由阿里云开发的AI助手。我被设计用来回答各种问题、提供信息和与用户进行对话。有什么我可以帮助你的吗？\"}}]},\"usage\":{\"total_tokens\":58,\"output_tokens\":36,\"input_tokens\":22},\"request_id\":\"39377fd7-26dd-99f5-b539-5fd004b6ecb5\"}";
        final ChatResponse response = JacksonUtils.toObject(json, ChatResponse.class);
        Assertions.assertEquals("39377fd7-26dd-99f5-b539-5fd004b6ecb5", response.uuid());
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(58, response.usage().total());
        Assertions.assertEquals(36, response.usage().total(item-> "output_tokens".equals(item.name())));
        Assertions.assertEquals(22, response.usage().total(item-> "input_tokens".equals(item.name())));
        Assertions.assertEquals(1, response.output().choices().size());
        Assertions.assertEquals(ChatResponse.Finish.NORMAL, response.output().best().finish());
        Assertions.assertEquals(Message.Role.AI, response.output().best().message().role());
        Assertions.assertEquals("我是通义千问，由阿里云开发的AI助手。我被设计用来回答各种问题、提供信息和与用户进行对话。有什么我可以帮助你的吗？", response.output().best().message().text());
    }

    @Test
    public void test$chat$response$tool_call() {
        final String json = "{\"output\": {\"choices\": [{\"finish_reason\": \"tool_calls\",\"message\": {\"role\": \"assistant\",\"tool_calls\": [{\"function\": {\"name\": \"get_current_weather\",\"arguments\": \"{\\\"location\\\":\\\"杭州市\\\"}\"},\"index\": 0,\"id\": \"call_240d6341de4c484384849d\",\"type\": \"function\"}],\"content\": \"\"}}]},\"usage\": {\"total_tokens\": 235,\"output_tokens\": 18,\"input_tokens\": 217},\"request_id\": \"235ed6a4-b6c0-9df0-aa0f-3c6dce89f3bd\"}";
        final ChatResponse response = JacksonUtils.toObject(json, ChatResponse.class);
        Assertions.assertEquals("235ed6a4-b6c0-9df0-aa0f-3c6dce89f3bd", response.uuid());
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(235, response.usage().total());
        Assertions.assertEquals(18, response.usage().total(item-> "output_tokens".equals(item.name())));
        Assertions.assertEquals(217, response.usage().total(item-> "input_tokens".equals(item.name())));
        Assertions.assertEquals(1, response.output().choices().size());
        Assertions.assertEquals(ChatResponse.Finish.TOOL_CALLS, response.output().best().finish());
        Assertions.assertEquals(Message.Role.AI, response.output().best().message().role());
        final ToolCallMessage message = (ToolCallMessage) response.output().best().message();
        final ChatFunctionTool.Call call = (ChatFunctionTool.Call)message.calls().get(0);
        Assertions.assertEquals("get_current_weather", call.stub().name());
        Assertions.assertEquals("{\"location\":\"杭州市\"}", call.stub().arguments());
    }

    @Test
    public void test$chat$response$tool_plugin() {
        final String json = "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"messages\":[{\"role\":\"assistant\",\"plugin_call\":{\"name\":\"calculator\",\"arguments\":\"{\\\"payload__input__text\\\": \\\"1+2*3-4/5\\\", \\\"header__request_id\\\": \\\"1234567890\\\"}\"},\"content\":\"\"},{\"role\":\"plugin\",\"name\":\"calculator\",\"content\":\"{\\\"equations\\\": [\\\"1 + 2 * 3 - 4 / 5\\\"], \\\"results\\\": [6.2]}\",\"status\":{\"code\":200,\"name\":\"Success\",\"message\":\"success\"}},{\"role\":\"assistant\",\"content\":\"计算结果是 6.2。\"}]}]},\"usage\":{\"plugins\":{\"calculator\":{\"count\":1}},\"total_tokens\":583,\"output_tokens\":57,\"input_tokens\":526},\"request_id\":\"0da4c1c9-f00c-91e6-b57a-dad214aef393\"}";
        final ChatResponse response = JacksonUtils.toObject(json, ChatResponse.class);
        Assertions.assertEquals("0da4c1c9-f00c-91e6-b57a-dad214aef393", response.uuid());
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(583, response.usage().total());
        Assertions.assertEquals(57, response.usage().total(item-> "output_tokens".equals(item.name())));
        Assertions.assertEquals(526, response.usage().total(item-> "input_tokens".equals(item.name())));
    }

}
