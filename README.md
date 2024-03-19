# DashScope4j：灵积 Java SDK
![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![LLM-通义千问](https://img.shields.io/badge/LLM-%E9%80%9A%E4%B9%89%E5%8D%83%E9%97%AE-blue.svg)

`DashScope4j`是一个开源的灵积非官方 Java SDK，基于 JDK17 构建。它旨在提供一个功能丰富、易于集成和使用灵积API（通义千问模型）的Java库，以便开发者能够通灵积API轻松实现多模态对话、向量嵌入和图像处理等功能。

> 请注意：在使用 DashScope4j 时，你需要遵守灵积API的使用条款和条件。

## 重要更新：1.2.0；支持函数调用

灵积在 2024-03-12 放出了 [函数调用](#函数调用示例) 功能，当前支持的模型是大语言模型 qwen-turbo、qwen-plus、qwen-max、qwen-max-longcontext，
DashScope4j从`1.2.0`版本开始作为 Java SDK 首发支持`函数调用`的功能。

函数调用是我实际开发中最喜欢的一个功能，它扩展了大模型的能力边界，让AI具备了操纵现实的能力。而之前要做到这些事情我得通过 LangChain 来实现。

## 依赖使用

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j</artifactId>
    <version>1.2.1</version>
</dependency>
```

### 

## 一、主要功能

`DashScope4j`支持以下API功能：

- **对话（Chat）**
    - 提供用户与灵积进行多模态(图、文)对话。
    - 提供用户与灵积进行多模态(图、音)对话。
    - 提供用户与灵积进行函数对话
      > 函数功能从`1.2.0`版本中支持：qwen-turbo、qwen-plus、qwen-max、qwen-max-longcontext

- **向量（Embeddings）**
    - 将文本转换为向量表示，用于文本相似度比较、聚类等任务。

- **图像（Images）**
    - **文生图：** 将文本描述转换为相应的图像。

- **插件应用（Plugin）**
    - **OCR插件：** 图像理解识别，并对图像内容进行总结概述，输出用户可理解的句子或段落。
    - **PDF解析插件：** 对PDF文件进行解析，提取、理解文本内容。
    - **计算器插件：** 对用户输入的数学表达式进行计算。
    - **文生图插件：** 将文本描述转换为相应的图像。

## 二、系统要求

1. **JDK17**或更高版本

## 三、跑通测试

1. 到阿里云的[模型服务-灵积](https://dashscope.console.aliyun.com/)中开通服务，获取`AK`
2. 到[API-KEY管理]()中创建一个`API-KEY`，获取其`AK`
3. 声明环境变量`export DASHSCOPE_AK=<YOUR APP-KEY>`
4. 运行测试用例：`mvn test`

## 四、使用示例

### 创建客户端
```java

// 线程池
final var executor = Executors.newFixedThreadPool(10);

// 创建客户端
final var client = DashScopeClient.newBuilder()
    .ak("<YOUR APP-KEY>")
    .executor(executor)
    .build();
```

### 对话示例（异步）

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
System.out.println(response.output().best().message().text());
```

输出日志

```text
2024-02-29 00:49:56 DEBUG dashscope://chat/qwen-vl-max => {"model":"qwen-vl-max","input":{"messages":[{"role":"user","content":[{"image":"https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"},{"text":"图片中一共多少辆自行车?"}]}]},"parameters":{}}
2024-02-29 00:49:59 DEBUG dashscope://chat/qwen-vl-max <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":[{"text":"图片中有两辆自行车。"}]}}]},"usage":{"output_tokens":7,"input_tokens":1264,"image_tokens":1230},"request_id":"f11e20f0-6774-9649-a0c9-6095e6287cdc"}
图片中有两辆自行车。
```

### 对话示例（流式）

```java
// 创建请求
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_VL_MAX)
    .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
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
final var latch = new CountDownLatch(1);
final var stringSB = new StringBuilder();
publisher.subscribe(new Flow.Subscriber<>(){

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }
    
    @Override
    public void onNext(ChatResponse response) {
        stringSB.append(response.output().best().message().text());
    }
    
    @Override
    public void onError(Throwable ex) {
        ex.printStackTrace(System.err);
    }
    
    @Override
    public void onComplete() {
        latch.countDown();
    }

});

// 等待处理完成
latch.await();
System.out.println(stringSB);
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

### 函数调用示例

灵积在`2024-03-12`放出了`函数调用`的功能，当前支持的模型是大语言模型`qwen-turbo`、`qwen-plus`、`qwen-max`、`qwen-max-longcontext`，下面是一个`函数调用`的示例：

假设我们有一个回显函数:`echo`
```java
@ChatFn(name = "echo", description = "当用户输入echo:，回显后边的文字")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

    @Override
    public CompletableFuture<Echo> call(Echo echo) {
        return CompletableFuture.completedFuture(new Echo(echo.words()));
    }

    public record Echo(
            @JsonPropertyDescription("需要回显的文字")
            String words
    ) {

    }

}
```

我们可以通过以下代码来调用这个函数：
```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_MAX)
    .functions(new EchoFunction())
    .user("echo: HELLO!")
    .build();
final var response = client.chat(request)
    .async()
    .join();
```

输出日志
```text
2024-03-19 21:28:38 DEBUG dashscope://chat/qwen-max => {"model":"qwen-max","input":{"messages":[{"role":"user","content":"echo: HELLO!"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"echo","description":"当用户输入echo:，回显后边的文字","parameters":{"type":"object","properties":{"words":{"type":"string","description":"需要回显的文字"}}}},"type":"function"}]}}
2024-03-19 21:28:40 DEBUG dashscope://chat/qwen-max <= {"output":{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","tool_calls":[{"function":{"name":"echo","arguments":"{\"words\": \"HELLO!\"}"},"id":"","type":"function"}],"content":""}}]},"usage":{"total_tokens":28,"output_tokens":23,"input_tokens":5},"request_id":"8af40d7a-d43d-9d7f-9f12-8d52accfe8ac"}
2024-03-19 21:28:40 DEBUG dashscope://chat/function <= {"words":"HELLO!"}
2024-03-19 21:28:40 DEBUG dashscope://chat/function => {"words":"HELLO!"}
2024-03-19 21:28:40 DEBUG dashscope://chat/qwen-max => {"model":"qwen-max","input":{"messages":[{"role":"user","content":"echo: HELLO!"},{"role":"assistant","tool_calls":[{"function":{"name":"echo","arguments":"{\"words\": \"HELLO!\"}"},"type":"function"}],"content":""},{"role":"tool","name":"echo","content":"{\"words\":\"HELLO!\"}"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"echo","description":"当用户输入echo:，回显后边的文字","parameters":{"type":"object","properties":{"words":{"type":"string","description":"需要回显的文字"}}}},"type":"function"}]}}
2024-03-19 21:28:42 DEBUG dashscope://chat/qwen-max <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"HELLO!"}}]},"usage":{"total_tokens":8,"output_tokens":3,"input_tokens":5},"request_id":"37ff7303-c1b2-9d7c-966d-82a7446fc52e"}
```

### 文生图示例

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

### 插件调用示例

#### PDF提取插件

我从网上下载了一份[《十四五规划》](https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf)的PDF文件，然后通过`PDF提取插件`来提取PDF文件的内容。

```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_PLUS)
    .plugins(ChatPlugin.PDF_EXTRACTER)
    .user(
            Content.ofText("请总结这篇文档"),
            Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
    )
    .build();
final var response = client.chat(request)
    .async()
    .join();
```

输出日志
```text
2024-03-20 00:24:22 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":[{"text":"请总结这篇文档"},{"file":"https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"}]}]},"parameters":{"result_format":"message"}}
2024-03-20 00:24:47 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"这篇文档是中国国民经济和社会发展第十四个五年规划和2035年远景目标纲要，它阐述了中国在新发展阶段的主要任务和战略目标。规划涵盖了多个领域，包括创新驱动发展、产业升级、市场体系建设、数字化发展、深化改革、乡村振兴、城镇化、区域协调发展、文化建设、绿色发展、对外开放、教育与健康、民生福祉、国家安全、国防和军队现代化、民主法治以及监督制度的完善。\n\n在创新驱动方面，强调了强化国家战略科技力量，提升企业创新能力，激发人才创新活力，并完善科技创新机制。在产业发展上，致力于制造业升级，发展战略性新兴产业，促进服务业繁荣，同时建设现代化基础设施体系。\n\n为了构建新发展格局，规划提出畅通国内大循环，促进国内外双循环，加快培育完整内需体系。数字化发展被放在重要位置，旨在打造数字经济新优势，加速数字社会建设，提升数字政府水平，并营造健康的数字环境。\n\n在深化改革方面，将激发各类市场主体活力，建设高标准市场体系，改革财税金融体制，提升政府经济治理能力。农业、农村发展和乡村振兴是关注焦点，包括提高农业质量和效益，实施乡村建设行动，以及城乡融合发展。\n\n此外，规划还强调优化区域经济布局，促进区域协调发展，发展社会主义先进文化，推动绿色发展，实行高水平对外开放，提升国民素质，增进民生福祉，加强国家安全和国防建设，以及加强民主法治和监督制度。\n\n最后，规划提出了一系列实施保障措施，包括加强党的领导，健全规划体系，完善实施机制，确保党中央重大决策的贯彻落实，激发全社会参与规划实施的积极性，并通过监测评估、政策保障和考核监督机制，确保规划目标的实现。整个规划为全面建设社会主义现代化国家描绘了蓝图，是全国各族人民共同遵循的行动纲领。"}}]},"usage":{"total_tokens":365,"output_tokens":361,"input_tokens":4},"request_id":"d1eb2274-35cc-97fb-b60a-570fab96ab0b"}
```

#### 计算器插件

```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_PLUS)
    .plugins(ChatPlugin.CALCULATOR)
    .user("1+2*3-4/5=?")
    .build();
final var response = client.chat(request)
    .async()
    .join();
```

输出日志
```text
2024-03-20 00:29:11 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":"1+2*3-4/5=?"}]},"parameters":{"result_format":"message"}}
2024-03-20 00:29:15 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"stop","messages":[{"role":"assistant","plugin_call":{"name":"calculator","arguments":"{\"payload__input__text\": \"1+2*3-4/5\"}"},"content":""},{"role":"plugin","name":"calculator","content":"{\"equations\": [\"1 + 2 * 3 - 4 / 5\"], \"results\": [6.2]}","status":{"code":200,"name":"Success","message":"success"}},{"role":"assistant","content":"The result of the expression \\(1 + 2 \\times 3 - \\frac{4}{5}\\) is approximately 6.2."}]}]},"usage":{"total_tokens":103,"output_tokens":93,"input_tokens":10},"request_id":"3659340f-7dad-9815-bd11-ce6c6eb4d1c2"}
```

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
