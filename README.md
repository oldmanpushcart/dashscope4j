# Dashscope4j：灵积 / 通义千问 Java SDK

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![LLM-通义千问](https://img.shields.io/badge/LLM-%E9%80%9A%E4%B9%89%E5%8D%83%E9%97%AE-blue.svg)

**Dashscope4j** 是一个开源的灵积非官方 Java SDK，基于 JDK8 构建。 它旨在提供一个功能丰富、易于集成和使用的Java库，
以便Java开发者能轻松调用灵积平台的多模态对话、向量嵌入和图像处理等模型API。

我个人使用于自己的智能助理项目：[MOSS-桌面个人助手](https://github.com/oldmanpushcart/moss)

> 请注意：在使用 Dashscope4j 时，你需要遵守灵积的使用条款和条件。

## 一、功能特性

### 独有功能特性

- **对话智能体**
  - 支持MCP

- **增强FunctionCall**
  - 本地函数：注解或构造器方式声明 FunctionCall
  - 多级调用：当大模型需要串联、并行调用多个函数时，自动帮你完成多级请求串联

- **增强拦截器**
  - OkHttp拦截器
  - Dashscope拦截器。支持按照请求、全局两个范围设置拦截器

- **增强对话请求**：统一的多模态对话编码风格

- 响应式编程风格：友好的任务、同步、异步、流、数据双工通讯请求API

- 支持请求上下文透传

### 支持以下阿里云百炼平台以下API功能

- **对话（Chat）**
  - 提供用户与灵积进行多模态(图、音、文)对话
  - 函数、插件调用

- **向量（Embeddings）**
  - 将文本转换为向量表示，用于文本相似度比较、聚类等任务
  - 将图音文本转换为向量表示，用于图音文相似度比较、聚类等任务

- **图像（Images）**
  - **文生图：** 将文本描述转换为相应的图像
  - **图生图：** 将文本描述和参考图片转换为相应的图像

- **视频（Video）**
  - **文生视频：** 将文本描述转换为相应的视频
  - **图生视频：** 将文本描述和参考图片转换为相应的视频

- **语音识别与合成**
  - 实时、非实时语音识别、合成
  - 音视频文件语音转录文本
  - 语音识别热词管理
  - 语音合成音色管理

- **基础功能**
  - Tokenizer计算（远程、本地）
  - 灵积提供的临时空间、文件管理


## 二、快速使用

### 申请灵积账号

> 如已申请则可跳过

到阿里云的 [模型服务-灵积](https://dashscope.console.aliyun.com/) 中开通服务，
然后到 [API-KEY管理](https://dashscope.console.aliyun.com/apiKey) 页面中创建并获取`AK`。

### 客户端例子

#### 添加 Maven 依赖
```xml

<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-client</artifactId>
    <version>3.2.0</version>
</dependency>
```

#### 简单示例
```java
public static void main(String... args) {

    // 初始化客户端
    final DashScopeClient client = DashScopeClient.newBuilder()
            .ak("...") // 请替换为你自己的AK
            .build();

    final ChatRequest request = ChatRequest.newBuilder()
            .model(ChatModel.QWEN_TURBO)
            .addMessage(Message.ofUser("你好呀!"))
            .build();

    final ChatResponse response = client.chat().async(request)
            .toCompletableFuture()
            .join();

    System.out.println(response.output().best().message().text());

    // 销毁客户端
    client.shutdown();

}
```

运行这段代码后,我可以得到如下的输出日志

```
2024-12-28 01:35:23 DEBUG dashscope://algo/qwen-turbo >>> {"model":"qwen-turbo","input":{"messages":[{"role":"user","content":"你好呀!"}]},"parameters":{}}
2024-12-28 01:35:23 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, Authorization: Bearer ******, X-DashScope-Client: dashscope4j@3.0.0-SNAPSHOT, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2024-12-28 01:35:24 TRACE HTTP:// <<< 200  eagleeye-traceid: 5ed0561e54849bd4d1af8d32703a0cf3, vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: db13c38b-4291-9f90-9117-a6be2d823ee5, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-finished: true, req-cost-time: 427, req-arrive-time: 1735320910529, resp-start-time: 1735320910957, x-envoy-upstream-service-time: 421, set-cookie: acw_tc=db13c38b-4291-9f90-9117-a6be2d823ee5e00679cda1184bc0403ad232d70f1ab7;path=/;HttpOnly;Max-Age=1800, date: Fri, 27 Dec 2024 17:35:10 GMT, server: istio-envoy
2024-12-28 01:35:24 DEBUG dashscope://algo/qwen-turbo <<< {"output":{"finish_reason":"stop","text":"你好！很高兴为你提供帮助。"},"usage":{"total_tokens":18,"output_tokens":7,"input_tokens":11},"request_id":"db13c38b-4291-9f90-9117-a6be2d823ee5"}
你好！很高兴为你提供帮助。
```

### 智能体例子

#### 添加 Maven 依赖
```xml

<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j</artifactId>
    <version>3.2.0</version>
</dependency>
```

#### 简单示例
```java
public static void main(String... args) {

  // 初始化客户端
  final DashScopeClient client = DashScopeClient.newBuilder()
          .ak("...") // 请替换为你自己的AK
          .build();
  
  // 初始化智能体
  final ChatAgent agent = ReActChatAgent.newBuilder()
          .client(client)
          .addFunction(new SystemDateTimeFunction())
          .addFunction(new DashscopeWebSearchFunction())
          .addFunction(new DashscopeGenImageByTextFunction())
          .build();

  final ChatRequest request = ChatRequest.newBuilder()
          .model(ChatModel.QWEN_TURBO)
          .addMessage(Message.ofUser("请根据杭州今天天气画一副因地制宜的水墨画"))
          .build();

  final ChatResponse response = agent.async(request)
          .toCompletableFuture()
          .join();

  System.out.println(response.output().best().message().text());
}
```

运行这段代码后,我可以得到如下的输出日志

```
2025-05-18 16:26:16 DEBUG dashscope-client://algo/qwen-turbo >>> {"model":"qwen-turbo","parameters":{"stop":["Observation:"]},"input":{"messages":[{"role":"user","reasoning_content":"","content":"Answer the following questions as best you can. You have access to the following tools:\n\n[### system_date_time\n#### SUMMARY\n获取系统当前时间。在处理涉及时间相关问题时，需调用此函数获取系统当前时间以作校准\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{}}\n\n, ### dashscope_web_search\n#### SUMMARY\n通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"keywords\":{\"type\":\"string\",\"description\":\"搜索关键词\"}},\"required\":[\"keywords\"]}\n\n, ### dashscope_gen_image_by_text\n#### SUMMARY\n根据文本提示生成图片\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"正向提示，描述期望图像包含的内容\"},\"negative\":{\"type\":\"string\",\"description\":\"负向提示，描述不期望图像包含的内容\"}},\"required\":[\"prompt\"]}\n\n]\n\nUse the following format:\n\nQuestion: the input question you must answer\nThought: you should always think about what to do\nAction: the action to take, should be one of [system_date_time, dashscope_web_search, dashscope_gen_image_by_text]\nAction Input: the input to the action\nObservation: the result of the action\n... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\nThought: I now know the final answer.\nFinal Answer: the final answer to the original input question\n\nBegin!\n\nQuestion:\n### INPUT\n请根据杭州今天天气画一副因地制宜的水墨画\n\n### PARTS\n"}]}}
2025-05-18 16:26:16 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:17 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: c007a8a3-a9ed-9669-b5b8-12d3a9ee95fb, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-finished: true, req-cost-time: 1265, req-arrive-time: 1747556776594, resp-start-time: 1747556777859, x-envoy-upstream-service-time: 1260, set-cookie: acw_tc=c007a8a3-a9ed-9669-b5b8-12d3a9ee95fb56a784052291d302825c587a0f02b568;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:17 GMT, server: istio-envoy
2025-05-18 16:26:17 DEBUG dashscope-client://algo/qwen-turbo <<< {"output":{"finish_reason":"stop","text":"Thought: 为了绘制一幅因地制宜的水墨画，我首先需要了解杭州今天的天气情况。我将使用系统_date_time工具来获取当前时间，并结合网络搜索来了解杭州的天气状况。\nAction: system_date_time\nAction Input: {}\n"},"usage":{"total_tokens":405,"output_tokens":52,"input_tokens":353,"prompt_tokens_details":{"cached_tokens":0}},"request_id":"c007a8a3-a9ed-9669-b5b8-12d3a9ee95fb"}
2025-05-18 16:26:17 DEBUG dashscope-client://algo/qwen-turbo >>> {"model":"qwen-turbo","parameters":{"stop":["Observation:"]},"input":{"messages":[{"role":"user","reasoning_content":"","content":"Answer the following questions as best you can. You have access to the following tools:\n\n[### system_date_time\n#### SUMMARY\n获取系统当前时间。在处理涉及时间相关问题时，需调用此函数获取系统当前时间以作校准\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{}}\n\n, ### dashscope_web_search\n#### SUMMARY\n通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"keywords\":{\"type\":\"string\",\"description\":\"搜索关键词\"}},\"required\":[\"keywords\"]}\n\n, ### dashscope_gen_image_by_text\n#### SUMMARY\n根据文本提示生成图片\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"正向提示，描述期望图像包含的内容\"},\"negative\":{\"type\":\"string\",\"description\":\"负向提示，描述不期望图像包含的内容\"}},\"required\":[\"prompt\"]}\n\n]\n\nUse the following format:\n\nQuestion: the input question you must answer\nThought: you should always think about what to do\nAction: the action to take, should be one of [system_date_time, dashscope_web_search, dashscope_gen_image_by_text]\nAction Input: the input to the action\nObservation: the result of the action\n... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\nThought: I now know the final answer.\nFinal Answer: the final answer to the original input question\n\nBegin!\n\nQuestion:\n### INPUT\n请根据杭州今天天气画一副因地制宜的水墨画\n\n### PARTS\n"},{"role":"assistant","reasoning_content":"","content":"Thought: 为了绘制一幅因地制宜的水墨画，我首先需要了解杭州今天的天气情况。我将使用系统_date_time工具来获取当前时间，并结合网络搜索来了解杭州的天气状况。\nAction: system_date_time\nAction Input: {}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"datetime\":\"2025-05-18T16:26:17.849\",\"pattern\":\"yyyy-MM-dd'T'HH:mm:ss.SSS\"}"}]}}
2025-05-18 16:26:17 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:19 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: e3ea5ca2-cd76-9ce5-8fe3-7c3461923c3f, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-finished: true, req-cost-time: 1377, req-arrive-time: 1747556777961, resp-start-time: 1747556779338, x-envoy-upstream-service-time: 1371, set-cookie: acw_tc=e3ea5ca2-cd76-9ce5-8fe3-7c3461923c3f28235a9ba8c8e7739faa6ea866f5894b;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:19 GMT, server: istio-envoy
2025-05-18 16:26:19 DEBUG dashscope-client://algo/qwen-turbo <<< {"output":{"finish_reason":"stop","text":"Thought: 我已经获取了当前系统时间，即2025年5月18日。接下来，我将通过网络搜索了解杭州当天的天气情况。\nAction: dashscope_web_search\nAction Input: {\"keywords\":\"杭州 2025年5月18日 天气\"}\n"},"usage":{"total_tokens":524,"output_tokens":66,"input_tokens":458,"prompt_tokens_details":{"cached_tokens":0}},"request_id":"e3ea5ca2-cd76-9ce5-8fe3-7c3461923c3f"}
2025-05-18 16:26:19 DEBUG dashscope-client://algo/qwen-plus >>> {"model":"qwen-plus","parameters":{"search_options":{"enable_source":false,"enable_citation":false,"forced_search":true},"enable_search":true},"input":{"messages":[{"role":"user","reasoning_content":"","content":"## 根据关键词搜索\n请使用以下关键词执行网络搜索，并按照指示的方式简洁回答。\n\n## 搜索关键词\n杭州 2025年5月18日 天气"}]}}
2025-05-18 16:26:19 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:25 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: bdf0f2c2-577c-9416-b1dc-787069e78ca6, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-inner-csi: verified, x-dashscope-finished: true, req-cost-time: 5951, req-arrive-time: 1747556779388, resp-start-time: 1747556785340, x-envoy-upstream-service-time: 5944, set-cookie: acw_tc=bdf0f2c2-577c-9416-b1dc-787069e78ca6523679bfc5ab2fdda0d0b3c2faba8935;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:25 GMT, server: istio-envoy
2025-05-18 16:26:25 DEBUG dashscope-client://algo/qwen-plus <<< {"output":{"finish_reason":"stop","text":"根据关键词“杭州 2025年5月18日 天气”搜索的结果：\n\n2025年5月18日，杭州的天气为多云转晴，气温范围在22°C至30°C之间，风力较小，适宜户外活动。请注意实际天气情况可能有所变化，建议关注当地气象预报以获取最新信息。"},"usage":{"total_tokens":332,"output_tokens":80,"input_tokens":252,"plugins":{"search":{"count":1}},"prompt_tokens_details":{"cached_tokens":0}},"request_id":"bdf0f2c2-577c-9416-b1dc-787069e78ca6"}
2025-05-18 16:26:25 DEBUG dashscope-client://algo/qwen-turbo >>> {"model":"qwen-turbo","parameters":{"stop":["Observation:"]},"input":{"messages":[{"role":"user","reasoning_content":"","content":"Answer the following questions as best you can. You have access to the following tools:\n\n[### system_date_time\n#### SUMMARY\n获取系统当前时间。在处理涉及时间相关问题时，需调用此函数获取系统当前时间以作校准\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{}}\n\n, ### dashscope_web_search\n#### SUMMARY\n通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"keywords\":{\"type\":\"string\",\"description\":\"搜索关键词\"}},\"required\":[\"keywords\"]}\n\n, ### dashscope_gen_image_by_text\n#### SUMMARY\n根据文本提示生成图片\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"正向提示，描述期望图像包含的内容\"},\"negative\":{\"type\":\"string\",\"description\":\"负向提示，描述不期望图像包含的内容\"}},\"required\":[\"prompt\"]}\n\n]\n\nUse the following format:\n\nQuestion: the input question you must answer\nThought: you should always think about what to do\nAction: the action to take, should be one of [system_date_time, dashscope_web_search, dashscope_gen_image_by_text]\nAction Input: the input to the action\nObservation: the result of the action\n... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\nThought: I now know the final answer.\nFinal Answer: the final answer to the original input question\n\nBegin!\n\nQuestion:\n### INPUT\n请根据杭州今天天气画一副因地制宜的水墨画\n\n### PARTS\n"},{"role":"assistant","reasoning_content":"","content":"Thought: 为了绘制一幅因地制宜的水墨画，我首先需要了解杭州今天的天气情况。我将使用系统_date_time工具来获取当前时间，并结合网络搜索来了解杭州的天气状况。\nAction: system_date_time\nAction Input: {}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"datetime\":\"2025-05-18T16:26:17.849\",\"pattern\":\"yyyy-MM-dd'T'HH:mm:ss.SSS\"}"},{"role":"assistant","reasoning_content":"","content":"Thought: 我已经获取了当前系统时间，即2025年5月18日。接下来，我将通过网络搜索了解杭州当天的天气情况。\nAction: dashscope_web_search\nAction Input: {\"keywords\":\"杭州 2025年5月18日 天气\"}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"output\":\"根据关键词“杭州 2025年5月18日 天气”搜索的结果：\\n\\n2025年5月18日，杭州的天气为多云转晴，气温范围在22°C至30°C之间，风力较小，适宜户外活动。请注意实际天气情况可能有所变化，建议关注当地气象预报以获取最新信息。\"}"}]}}
2025-05-18 16:26:25 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:27 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: 69d2c5cf-97ec-98d1-8713-fc01bfc7af8e, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-finished: true, req-cost-time: 2311, req-arrive-time: 1747556785387, resp-start-time: 1747556787699, x-envoy-upstream-service-time: 2304, set-cookie: acw_tc=69d2c5cf-97ec-98d1-8713-fc01bfc7af8e6dcf43d88250f718b4754d98eff78eba;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:27 GMT, server: istio-envoy
2025-05-18 16:26:27 DEBUG dashscope-client://algo/qwen-turbo <<< {"output":{"finish_reason":"stop","text":"Thought: 我已经了解到杭州今天的天气为多云转晴，气温适中。现在，我将根据这些信息使用dashscope_gen_image_by_text工具来生成一幅水墨画。\nAction: dashscope_gen_image_by_text\nAction Input: {\"prompt\":\"多云转晴，杭州，水墨画，风景画，天空中有几片白云，远处有青山，近处有一条小河，河上有几只小船，河边有一些树木和房屋。\",\"negative\":\"\"}\n"},"usage":{"total_tokens":726,"output_tokens":102,"input_tokens":624,"prompt_tokens_details":{"cached_tokens":0}},"request_id":"69d2c5cf-97ec-98d1-8713-fc01bfc7af8e"}
2025-05-18 16:26:27 DEBUG dashscope-client://algo/wanx2.1-t2i-turbo >>> {"model":"wanx2.1-t2i-turbo","parameters":{"n":1},"input":{"negative_prompt":"","prompt":"多云转晴，杭州，水墨画，风景画，天空中有几片白云，远处有青山，近处有一条小河，河上有几只小船，河边有一些树木和房屋。"}}
2025-05-18 16:26:27 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: enable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:27 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: 9d711cca-9363-97c5-aa7b-0e279e98949b, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, req-cost-time: 113, req-arrive-time: 1747556787772, resp-start-time: 1747556787886, x-envoy-upstream-service-time: 109, set-cookie: acw_tc=9d711cca-9363-97c5-aa7b-0e279e98949bda4c8a65a6b93908530c54c9372f0a73;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:27 GMT, server: istio-envoy
2025-05-18 16:26:27 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 >>> GET
2025-05-18 16:26:27 TRACE HTTP:// >>> GET https://dashscope.aliyuncs.com/api/v1/tasks/c2145ead-f262-44c3-8b4e-7ff711cc8f37 Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:27 TRACE HTTP:// <<< 200  server: istio-envoy, date: Sun, 18 May 2025 08:26:27 GMT, content-type: application/json;charset=UTF-8, vary: Accept-Encoding, req-cost-time: 53, req-arrive-time: 1747556787940, resp-start-time: 1747556787994, x-envoy-upstream-service-time: 45, set-cookie: acw_tc=7c238d21-24b9-9bd0-b8db-36d2387f1d2e0628d527fb78d1f113403ae44cfbdd6d;path=/;HttpOnly;Max-Age=1800
2025-05-18 16:26:27 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 <<< {"request_id":"7c238d21-24b9-9bd0-b8db-36d2387f1d2e","output":{"task_id":"c2145ead-f262-44c3-8b4e-7ff711cc8f37","task_status":"RUNNING","submit_time":"2025-05-18 16:26:27.871","scheduled_time":"2025-05-18 16:26:27.890"}}
2025-05-18 16:26:32 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 >>> GET
2025-05-18 16:26:32 TRACE HTTP:// >>> GET https://dashscope.aliyuncs.com/api/v1/tasks/c2145ead-f262-44c3-8b4e-7ff711cc8f37 Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:33 TRACE HTTP:// <<< 200  server: istio-envoy, date: Sun, 18 May 2025 08:26:33 GMT, content-type: application/json;charset=UTF-8, vary: Accept-Encoding, req-cost-time: 62, req-arrive-time: 1747556793080, resp-start-time: 1747556793142, x-envoy-upstream-service-time: 53, set-cookie: acw_tc=4a8a7d8d-d9ea-9f68-b48d-807a99751e82c688724990aee2b3cf0a887331a8cf71;path=/;HttpOnly;Max-Age=1800
2025-05-18 16:26:33 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 <<< {"request_id":"4a8a7d8d-d9ea-9f68-b48d-807a99751e82","output":{"task_id":"c2145ead-f262-44c3-8b4e-7ff711cc8f37","task_status":"RUNNING","submit_time":"2025-05-18 16:26:27.871","scheduled_time":"2025-05-18 16:26:27.890"}}
2025-05-18 16:26:38 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 >>> GET
2025-05-18 16:26:38 TRACE HTTP:// >>> GET https://dashscope.aliyuncs.com/api/v1/tasks/c2145ead-f262-44c3-8b4e-7ff711cc8f37 Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:38 TRACE HTTP:// <<< 200  server: istio-envoy, date: Sun, 18 May 2025 08:26:38 GMT, content-type: application/json;charset=UTF-8, vary: Accept-Encoding, req-cost-time: 48, req-arrive-time: 1747556798183, resp-start-time: 1747556798231, x-envoy-upstream-service-time: 40, set-cookie: acw_tc=ae572916-c40f-9a36-9701-317538cb510511f88f9df427d9eb37ed18554926de65;path=/;HttpOnly;Max-Age=1800
2025-05-18 16:26:38 DEBUG dashscope-client://base/task/get/c2145ead-f262-44c3-8b4e-7ff711cc8f37 <<< {"request_id":"ae572916-c40f-9a36-9701-317538cb5105","output":{"task_id":"c2145ead-f262-44c3-8b4e-7ff711cc8f37","task_status":"SUCCEEDED","submit_time":"2025-05-18 16:26:27.871","scheduled_time":"2025-05-18 16:26:27.890","end_time":"2025-05-18 16:26:36.803","results":[{"orig_prompt":"多云转晴，杭州，水墨画，风景画，天空中有几片白云，远处有青山，近处有一条小河，河上有几只小船，河边有一些树木和房屋。","actual_prompt":"写意水墨风景画，杭州多云转晴的秀丽景致。近景处，一条清澈的小河蜿蜒流淌，河面上几只乌篷小船随波荡漾。河边垂柳依依，几棵高大的树木与几间白墙黑瓦的民居错落有致。中景是开阔的田野，远处连绵的青山若隐若现，天空中飘着几缕淡雅的白云。运用干湿浓淡的笔墨变化，营造出空灵悠远的意境，构图讲究疏密有致，展现江南水乡的独特韵味。整体色调清雅，远近层次分明。","url":"https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/1d/df/20250518/8928fb36/c2145ead-f262-44c3-8b4e-7ff711cc8f371125540556.png?Expires=1747643196&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=hYtUqvYDYnQxYS1RKPzU0nAPaR0%3D"}],"task_metrics":{"TOTAL":1,"SUCCEEDED":1,"FAILED":0}},"usage":{"image_count":1}}
2025-05-18 16:26:38 DEBUG dashscope-client://algo/wanx2.1-t2i-turbo <<< {"request_id":"ae572916-c40f-9a36-9701-317538cb5105","output":{"task_id":"c2145ead-f262-44c3-8b4e-7ff711cc8f37","task_status":"SUCCEEDED","submit_time":"2025-05-18 16:26:27.871","scheduled_time":"2025-05-18 16:26:27.890","end_time":"2025-05-18 16:26:36.803","results":[{"orig_prompt":"多云转晴，杭州，水墨画，风景画，天空中有几片白云，远处有青山，近处有一条小河，河上有几只小船，河边有一些树木和房屋。","actual_prompt":"写意水墨风景画，杭州多云转晴的秀丽景致。近景处，一条清澈的小河蜿蜒流淌，河面上几只乌篷小船随波荡漾。河边垂柳依依，几棵高大的树木与几间白墙黑瓦的民居错落有致。中景是开阔的田野，远处连绵的青山若隐若现，天空中飘着几缕淡雅的白云。运用干湿浓淡的笔墨变化，营造出空灵悠远的意境，构图讲究疏密有致，展现江南水乡的独特韵味。整体色调清雅，远近层次分明。","url":"https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/1d/df/20250518/8928fb36/c2145ead-f262-44c3-8b4e-7ff711cc8f371125540556.png?Expires=1747643196&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=hYtUqvYDYnQxYS1RKPzU0nAPaR0%3D"}],"task_metrics":{"TOTAL":1,"SUCCEEDED":1,"FAILED":0}},"usage":{"image_count":1}}
2025-05-18 16:26:38 DEBUG dashscope-client://algo/qwen-turbo >>> {"model":"qwen-turbo","parameters":{"stop":["Observation:"]},"input":{"messages":[{"role":"user","reasoning_content":"","content":"Answer the following questions as best you can. You have access to the following tools:\n\n[### system_date_time\n#### SUMMARY\n获取系统当前时间。在处理涉及时间相关问题时，需调用此函数获取系统当前时间以作校准\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{}}\n\n, ### dashscope_web_search\n#### SUMMARY\n通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"keywords\":{\"type\":\"string\",\"description\":\"搜索关键词\"}},\"required\":[\"keywords\"]}\n\n, ### dashscope_gen_image_by_text\n#### SUMMARY\n根据文本提示生成图片\n#### PARAMETER-SCHEMA\n{\"type\":\"object\",\"properties\":{\"prompt\":{\"type\":\"string\",\"description\":\"正向提示，描述期望图像包含的内容\"},\"negative\":{\"type\":\"string\",\"description\":\"负向提示，描述不期望图像包含的内容\"}},\"required\":[\"prompt\"]}\n\n]\n\nUse the following format:\n\nQuestion: the input question you must answer\nThought: you should always think about what to do\nAction: the action to take, should be one of [system_date_time, dashscope_web_search, dashscope_gen_image_by_text]\nAction Input: the input to the action\nObservation: the result of the action\n... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\nThought: I now know the final answer.\nFinal Answer: the final answer to the original input question\n\nBegin!\n\nQuestion:\n### INPUT\n请根据杭州今天天气画一副因地制宜的水墨画\n\n### PARTS\n"},{"role":"assistant","reasoning_content":"","content":"Thought: 为了绘制一幅因地制宜的水墨画，我首先需要了解杭州今天的天气情况。我将使用系统_date_time工具来获取当前时间，并结合网络搜索来了解杭州的天气状况。\nAction: system_date_time\nAction Input: {}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"datetime\":\"2025-05-18T16:26:17.849\",\"pattern\":\"yyyy-MM-dd'T'HH:mm:ss.SSS\"}"},{"role":"assistant","reasoning_content":"","content":"Thought: 我已经获取了当前系统时间，即2025年5月18日。接下来，我将通过网络搜索了解杭州当天的天气情况。\nAction: dashscope_web_search\nAction Input: {\"keywords\":\"杭州 2025年5月18日 天气\"}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"output\":\"根据关键词“杭州 2025年5月18日 天气”搜索的结果：\\n\\n2025年5月18日，杭州的天气为多云转晴，气温范围在22°C至30°C之间，风力较小，适宜户外活动。请注意实际天气情况可能有所变化，建议关注当地气象预报以获取最新信息。\"}"},{"role":"assistant","reasoning_content":"","content":"Thought: 我已经了解到杭州今天的天气为多云转晴，气温适中。现在，我将根据这些信息使用dashscope_gen_image_by_text工具来生成一幅水墨画。\nAction: dashscope_gen_image_by_text\nAction Input: {\"prompt\":\"多云转晴，杭州，水墨画，风景画，天空中有几片白云，远处有青山，近处有一条小河，河上有几只小船，河边有一些树木和房屋。\",\"negative\":\"\"}\n"},{"role":"user","reasoning_content":"","content":"Observation:{\"image_uris\":[\"https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/1d/df/20250518/8928fb36/c2145ead-f262-44c3-8b4e-7ff711cc8f371125540556.png?Expires=1747643196&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=hYtUqvYDYnQxYS1RKPzU0nAPaR0%3D\"]}"}]}}
2025-05-18 16:26:38 TRACE HTTP:// >>> POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation Content-Type: application/json, X-DashScope-Client: dashscope4j-client@3.2.0-SNAPSHOT, Authorization: Bearer ******, X-DashScope-SSE: disable, X-DashScope-Async: disable, X-DashScope-OssResourceResolve: enable
2025-05-18 16:26:41 TRACE HTTP:// <<< 200  vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding, content-type: application/json, x-request-id: 6d37de95-9891-9c80-89f0-e098e22113a5, x-dashscope-timeout: 180, x-dashscope-call-gateway: true, x-dashscope-finished: true, req-cost-time: 3527, req-arrive-time: 1747556798292, resp-start-time: 1747556801820, x-envoy-upstream-service-time: 3521, set-cookie: acw_tc=6d37de95-9891-9c80-89f0-e098e22113a5283027daea3057695514765622f495db;path=/;HttpOnly;Max-Age=1800, date: Sun, 18 May 2025 08:26:41 GMT, server: istio-envoy
2025-05-18 16:26:41 DEBUG dashscope-client://algo/qwen-turbo <<< {"output":{"finish_reason":"stop","text":"Thought: 我已经得到了一幅描绘杭州今天多云转晴天气的水墨画。现在我可以回答原始问题了。\nFinal Answer: 根据杭州今天的天气（多云转晴），我为您生成了一幅因地制宜的水墨画。您可以点击以下链接查看：\n\n![](https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/1d/df/20250518/8928fb36/c2145ead-f262-44c3-8b4e-7ff711cc8f371125540556.png?Expires=1747643196&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=hYtUqvYDYnQxYS1RKPzU0nAPaR0%3D)"},"usage":{"total_tokens":1117,"output_tokens":216,"input_tokens":901,"prompt_tokens_details":{"cached_tokens":0}},"request_id":"6d37de95-9891-9c80-89f0-e098e22113a5"}
2025-05-18 16:26:41 DEBUG dashscope-agent://chat-agent-101/async completed.
根据杭州今天的天气（多云转晴），我为您生成了一幅因地制宜的水墨画。您可以点击以下链接查看：

![](https://dashscope-result-wlcb-acdr-1.oss-cn-wulanchabu-acdr-1.aliyuncs.com/1d/df/20250518/8928fb36/c2145ead-f262-44c3-8b4e-7ff711cc8f371125540556.png?Expires=1747643196&OSSAccessKeyId=LTAI5tKPD3TMqf2Lna1fASuh&Signature=hYtUqvYDYnQxYS1RKPzU0nAPaR0%3D)

```



## 三、使用说明

- [多模态对话生成](https://github.com/oldmanpushcart/dashscope4j/wiki/Chat)
- 多模态向量计算
- 文生图
- 语音处理

## 四、关于软件

### 版本号声明

软件版本号采用：`大版本`.`小版本`.`漏洞修复`的格式

- **大版本：** 程序的架构设计进行重大升级或重大改造

- **小版本：**
    1. 增加新的API功能
    2. 在现有架构下完成局部架构的微调

- **漏洞修复：** 在不改变现有架构和API情况下，对漏洞修复和增强

### 写在最后

灵积是有官方的Java客户端的，我之所以还需要开发这个 Dashscope4j 主要是基于以下几点考虑

1. ~~官方的SDK并不开源，你无法查看其源码，也无法自行修改和定制~~
   > 官方的SDK已经开源了，你可以查看 [dashscope-sdk-java](https://github.com/dashscope/dashscope-sdk-java)
2. 我个人更喜欢响应式的编程风格，也更喜欢chain式的API声明
3. 个人练手习惯，反正也不花我多少时间

## 七、相关链接

- [模型服务-灵积](https://dashscope.aliyun.com)
- [帮助文档-灵积](https://help.aliyun.com/zh/dashscope/)
