# Dashscope4j：灵积 / 通义千问 Java SDK

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK8+](https://img.shields.io/badge/JDK-8+-blue.svg)
![LLM-通义千问](https://img.shields.io/badge/LLM-%E9%80%9A%E4%B9%89%E5%8D%83%E9%97%AE-blue.svg)

`Dashscope4j`是一个开源的灵积非官方 Java SDK，基于 JDK8 构建。 它旨在提供一个功能丰富、易于集成和使用的Java库，
以便Java开发者能轻松调用灵积平台的多模态对话、向量嵌入和图像处理等模型API。

> 请注意：在使用 Dashscope4j 时，你需要遵守灵积的使用条款和条件。

## 一、功能特性

Dashscope4j 支持以下API功能：

- **对话（Chat）**
  - 提供用户与灵积进行多模态(图、音、文)对话
  - 支持函数、插件调用

- **向量（Embeddings）**
  - 将文本转换为向量表示，用于文本相似度比较、聚类等任务
  - 将图音文本转换为向量表示，用于图音文相似度比较、聚类等任务

- **图像（Images）**
  - **文生图：** 将文本描述转换为相应的图像

- **语音识别与合成**
  - 支持实时、非实时语音识别、合成
  - 支持音视频文件语音转录文本

- **基础功能**
  - 支持Tokenizer计算（远程、本地）
  - 支持灵积提供的临时空间、文件管理
  - 拦截器

## 二、快速使用

### 申请灵积账号

> 如已申请则可跳过

到阿里云的 [模型服务-灵积](https://dashscope.console.aliyun.com/) 中开通服务，
然后到 [API-KEY管理](https://dashscope.console.aliyun.com/apiKey) 页面中创建并获取`AK`。

### 添加 Maven 依赖

```xml

<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 简单对话示例

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

## 三、使用说明

- 多模态对话生成
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

1. 官方的SDK并不开源，你无法查看其源码，也无法自行修改和定制
2. 个人练手习惯，反正也不花我多少时间，嗯嗯

## 七、相关链接

- [模型服务-灵积](https://dashscope.aliyun.com)
- [帮助文档-灵积](https://help.aliyun.com/zh/dashscope/)
