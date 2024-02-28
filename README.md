# DashScope4j：灵积的Java客户端

`DashScope4j`是一个开源的灵积非官方Java客户端，基于`JDK17`
构建。它旨在提供一个功能丰富、易于集成和使用的Java库，以便开发者能够通灵积API轻松实现多模态对话、续向量嵌入和图像处理等功能。

> 请注意：在使用`DashScope4j`时，你需要遵守灵积API的使用条款和条件。

## 一、主要功能

`DashScope4j`支持以下API功能：

- **对话（Chat）**
    - 提供用户与灵积进行多模态(图、文)对话。
    - 提供用户与灵积进行多模态(图、音)对话。

- **向量（Embeddings）**
    - 将文本转换为向量表示，用于文本相似度比较、聚类等任务。

- **图像（Images）**
    - **文生图：** 将文本描述转换为相应的图像。

- **插件应用（Plugin）**
    - **OCR插件：** 图片理解识别，并对图片内容进行总结概述，输出用户可理解的句子或段落。
    - **PDF解析插件：** 对PDF文件进行解析，提取、理解文本内容。
    - **计算器插件：** 对用户输入的数学表达式进行计算。
    - **文生图插件：** 将文本描述转换为相应的图像。

## 二、系统要求

1. **JDK17**或更高版本

## 三、跑通测试

1. 到阿里云的[模型服务-灵积](https://dashscope.console.aliyun.com/)中开通服务，获取`SK`
2. 到[API-KEY管理]()中创建一个`API-KEY`，获取其`AK`
3. 声明环境变量`export DASHSCOPE_AK=<YOUR APP-KEY>`
4. 运行测试用例：`mvn test`

## 四、依赖使用

### 添加依赖

项目仓库托管在Maven中央仓库，你可以在`pom.xml`中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j</artifactId>
    <version>1.1.0</version>
</dependency>
```

### 使用示例

#### 创建客户端
```java

// 线程池
ExecutorService executor = Executors.newFixedThreadPool(10);

// 创建客户端
DashScopeClient client = DashScopeClient.newBuilder()
        .ak("<YOUR APP-KEY>")
        .executor(executor)
        .build();
```

#### 对话示例（异步）

```java
// 创建请求
final var request = ChatRequest.newBuilder()
        .model(ChatModel.QWEN_VL_MAX)
        .user(
                Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                Content.ofText("图片中一共多少辆自行车?")
        )
        .build();

// 异步应答
final var response = client.chat(request)
        .async()
        .join();

// 输出结果（异步）
System.out.println(response.best().message().text());
```

输出日志

```text
2024-02-29 00:49:56 DEBUG dashscope://chat/qwen-vl-max => {"model":"qwen-vl-max","input":{"messages":[{"role":"user","content":[{"image":"https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"},{"text":"图片中一共多少辆自行车?"}]}]},"parameters":{}}
2024-02-29 00:49:59 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":[{"text":"图片中有两辆自行车。"}]}}]},"usage":{"output_tokens":7,"input_tokens":1264,"image_tokens":1230},"request_id":"f11e20f0-6774-9649-a0c9-6095e6287cdc"}
图片中有两辆自行车。
```

#### 对话示例（流式）

```java
// 创建请求
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_VL_MAX)
    .user(
            Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
            Content.ofText("图片中一共多少辆自行车?")
    )
    .build();

// 流式应答
final var publisher = client.chat(request)
    .flow()
    .join();

// 应答输出（流式）
publisher.subscribe(new Flow.Subscriber<>(){

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      subscription.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onNext(ChatResponse response) {
      System.out.println(response.best().message().text());
    }
    
    @Override
    public void onError(Throwable ex) {
      ex.printStackTrace(System.err);
    }
    
    @Override
    public void onComplete() {
      System.out.println("Complete");
    }

});
```

输出日志

```text
2024-02-29 01:21:42 DEBUG dashscope://chat/qwen-vl-max => {"model":"qwen-vl-max","input":{"messages":[{"role":"user","content":[{"image":"https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"},{"text":"图片中一共多少辆自行车?"}]}]},"parameters":{"incremental_output":true}}
2024-02-29 01:21:44 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"message":{"content":[{"text":"图片"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"input_tokens":1264,"output_tokens":1,"image_tokens":1230},"request_id":"9713405c-31b3-97a5-8e99-ac2c685798a0"}
2024-02-29 01:21:44 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"message":{"content":[{"text":"中有"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"input_tokens":1264,"output_tokens":2,"image_tokens":1230},"request_id":"9713405c-31b3-97a5-8e99-ac2c685798a0"}
2024-02-29 01:21:45 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"message":{"content":[{"text":"两"}],"role":"assistant"},"finish_reason":"null"}]},"usage":{"input_tokens":1264,"output_tokens":3,"image_tokens":1230},"request_id":"9713405c-31b3-97a5-8e99-ac2c685798a0"}
2024-02-29 01:21:45 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"message":{"content":[{"text":"辆自行车。"}],"role":"assistant"},"finish_reason":"stop"}]},"usage":{"input_tokens":1264,"output_tokens":7,"image_tokens":1230},"request_id":"9713405c-31b3-97a5-8e99-ac2c685798a0"}
图片中有两辆自行车。
```

#### 文生图示例

```java
// 创建请求
final var request = GenImageRequest.newBuilder()
    .model(GenImageModel.WANX_V1)
    .option(GenImageOptions.NUMBER, 1)
    .option(GenImageOptions.STYLE, GenImageRequest.Style.ANIME)
    .prompt("画古风美少女，黑发，面容白皙精致，发饰精美")
    .build();

// 任务应答
final var response = client.genImage(request)
    .task(Task.WaitStrategies.perpetual(Duration.ofSeconds(1L)))
    .join();
```

输出日志

```text
2024-02-29 01:27:01 DEBUG dashscope://image/generation/wanx-v1 => {"model":"wanx-v1","input":{"prompt":"画古风美少女，黑发，面容白皙精致，发饰精美","negative_prompt":null},"parameters":{"style":"<anime>","n":1}}
2024-02-29 01:27:02 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:02 DEBUG dashscope://task/get <= {"request_id":"becb655f-2e7c-9a82-8d0a-c0e48f925221","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:03 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:03 DEBUG dashscope://task/get <= {"request_id":"f329109b-8d7f-9e7f-9049-faced6937cc8","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:04 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:04 DEBUG dashscope://task/get <= {"request_id":"ec190c02-33b4-91ca-8fa2-2abb0dfd979e","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:05 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:05 DEBUG dashscope://task/get <= {"request_id":"f7e2dbe9-11e8-9681-9a07-1e5076369d62","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
...
2024-02-29 01:27:26 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:26 DEBUG dashscope://task/get <= {"request_id":"5e50bc5e-9215-9737-9ecb-dc3fd8c586d1","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:27 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:27 DEBUG dashscope://task/get <= {"request_id":"408498bc-26c9-9792-b47d-d8b4950a116c","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:28 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:29 DEBUG dashscope://task/get <= {"request_id":"cd3bcc40-3ad6-9f7c-acad-f29653f53c2d","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:30 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:30 DEBUG dashscope://task/get <= {"request_id":"34b79abd-aa21-988c-87b5-f00261a40ad9","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:31 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:31 DEBUG dashscope://task/get <= {"request_id":"682cec73-19d1-9fbd-a50c-465908c64106","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"RUNNING","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","task_metrics":{"TOTAL":1,"SUCCEEDED":0,"FAILED":0}}}
2024-02-29 01:27:32 DEBUG dashscope://task/get => dd693d99-0b5e-4261-8b08-7c633919d2fc
2024-02-29 01:27:32 DEBUG dashscope://task/get <= {"request_id":"7414bd26-8b51-9e04-abad-c223985ee809","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"SUCCEEDED","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","end_time":"2024-02-29 01:27:32.081","results":[{"url":"https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/1d/a8/20240229/c34adf05/3464efa2-8874-4aa9-86a4-8aba14b9707d-1.png?Expires=1709227651&OSSAccessKeyId=LTAI5tQZd8AEcZX6KZV4G8qL&Signature=yZ04L3oCKULBD%2By3PGSdmh0jg6M%3D"}],"task_metrics":{"TOTAL":1,"SUCCEEDED":1,"FAILED":0}},"usage":{"image_count":1}}
2024-02-29 01:27:32 DEBUG dashscope://image/generation/wanx-v1 <= {"request_id":"7414bd26-8b51-9e04-abad-c223985ee809","output":{"task_id":"dd693d99-0b5e-4261-8b08-7c633919d2fc","task_status":"SUCCEEDED","submit_time":"2024-02-29 01:27:02.856","scheduled_time":"2024-02-29 01:27:02.911","end_time":"2024-02-29 01:27:32.081","results":[{"url":"https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/1d/a8/20240229/c34adf05/3464efa2-8874-4aa9-86a4-8aba14b9707d-1.png?Expires=1709227651&OSSAccessKeyId=LTAI5tQZd8AEcZX6KZV4G8qL&Signature=yZ04L3oCKULBD%2By3PGSdmh0jg6M%3D"}],"task_metrics":{"TOTAL":1,"SUCCEEDED":1,"FAILED":0}},"usage":{"image_count":1}}
```

然后你就可以通过`response.output().results().get(0)`拿到生成的图片地址。

![文生图-美女](https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-003.png)

## 五、参与贡献

如果你对`DashScope4j`感兴趣并希望为其做出贡献，请遵循以下步骤：

1. Fork本项目到你的GitHub账户。
2. 克隆项目到你的本地环境。
3. 创建一个新的分支用于你的修改。
4. 提交你的更改并通过`Pull Request`请求合并到主分支。

在提交Pull Request之前，请确保你的代码符合项目的编码规范和最佳实践，并且已经通过了相关的测试。

## 六、关于软件

### 版本号声明

软件版本号采用：`大版本`.`小版本`.`漏洞修复`的格式

- **大版本：** 程序的架构设计进行重大升级或重大改造

- **小版本：** 
  1. 增加新的API功能
  2. 在现有架构下完成局部架构的微调

- **漏洞修复：** 在不改变现有架构和API情况下，对漏洞修复和增强

### 写在最后

灵积是有官方的Java客户端的，我之所以还需要开发这个`DashScope4j`主要是基于以下几点考虑

1. 官方的SDK并不开源，你无法查看其源码，也无法自行修改和定制
2. 个人练手习惯，反正也不花我多少时间，嗯嗯

## 七、相关链接

- [模型服务-灵积](https://dashscope.aliyun.com)
- [帮助文档-灵积](https://help.aliyun.com/zh/dashscope/)
