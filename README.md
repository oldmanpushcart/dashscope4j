# DashScope4j：灵积 Java SDK

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![LLM-通义千问](https://img.shields.io/badge/LLM-%E9%80%9A%E4%B9%89%E5%8D%83%E9%97%AE-blue.svg)

`DashScope4j`是一个开源的灵积非官方 Java SDK，基于 JDK17
构建。它旨在提供一个功能丰富、易于集成和使用灵积API（通义千问模型）的Java库，以便开发者能够通灵积API轻松实现多模态对话、向量嵌入和图像处理等功能。

> 请注意：在使用 DashScope4j 时，你需要遵守灵积API的使用条款和条件。

## 依赖使用

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j</artifactId>
    <version>2.2.1</version>
</dependency>
```

## 重要更新

- **2.2.1：** 问题修复版本，修复问题 #19 #18

- **2.2.0：** 语音识别与合成支持
  > [例子：如何用通义千问做一个同声传译](https://github.com/oldmanpushcart/dashscope4j/wiki/Example-SimulaTrans)
  - 新增双工数据交互操作接口`Exchange`，用于支持语音、视频等多模态模型交互
  - 支持实时语音识别
  - 支持实时语音合成
  - 支持音视频转录
  - 支持远程、本地Tokenizer
  - 优化部分API，存在不向下兼容可能
    - 所有对外暴露的`CompletableFuture`变更为`CompletionStage`，解决暴露接口功能过于强大的问题
    - 增加`HttpApiRequest / Response`和`ExchangeApiRequest / Response`的API分层，为了更好地支持多模态数据交互做准备

- **2.1.0：** 不兼容API修复
    - 修复模块中错误的exports，该修复会将模块中不应该暴露的内部api重新收回。本次修复不向下兼容。
    - 修复ChatRequest中函数调用因为Message序列化问题丢失PluginCall、Plugin、ToolCall、Tool等信息的BUG

- **2.0.0：** 大版本重构。核心API进行不兼容调整和实现重构（请注意），删除1.x.x版本被标记为已废弃的方法
    - 重构拦截器接口和实现重构，并添加了流控、重试等拦截器实现
    - 调整部分类、API的位置和命名；删除已废弃的方法
    - Flow相关实现进行重构

- **1.x.x：** 历代版本核心功能
    - **1.4.0：** 支持无感临时空间、请求和应答拦截器
    - **1.3.0：** 支持多模态向量计算
    - **1.2.0：** 支持多函数级联调用
    - **1.1.1：** 第一个稳定版本

### 语音合成

我们可以通过以下代码进行语音合成：

```java
// 文本集合
final var strings = new String[]{
    "白日依山尽，",
    "黄河入海流。",
    "欲穷千里目，",
    "更上一层楼。"
};

/*
 * 语音合成请求
 * 采样率：16000
 * 编码格式：WAV(PCM)
 */
final var request = SpeechSynthesisRequest.newBuilder()
    .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
    .option(SpeechSynthesisOptions.SAMPLE_RATE, 16000)
    .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisRequest.Format.WAV)
    .build();

// 以语音合成请求为模板，对每个文本生成一个语音合成请求
final var requests = Stream.of(strings)
    .map(string -> SpeechSynthesisRequest.newBuilder(request)
        .text(string)
        .build()
    )
    .toList();

// 聚合成请求发布器
final var requestPublisher = FlowPublishers.fromIterator(requests);

// 进行语音合成
client.audio().synthesis(request)

    // 打开语音合成数据交互通道：全双工模式，输出到audio.wav文件
    .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofPath(Path.of("./audio.wav")))
    
    // 发送语音合成请求序列
    .thenCompose(exchange -> exchange.writeDataPublisher(requestPublisher))
    
    // 语音合成结束
    .thenCompose(Exchange::finishing)
    
    // 等待通道关闭
    .thenCompose(Exchange::closeFuture)
    .toCompletableFuture()
    .join();
```
这样我们就可以获取到生成的`audio.wav`文件，可以试着播放下，符不符合你的要求。

当然你也可以通过`ExchangeListeners.ofByteChannel(...)`方法，将输出的字节流转入到你指定的`ByteChannel`中，比如音频播放设备。这样就可以实现语音播放。
在语音合成结束后，为了避免对通道的占用，需要及时关闭`Exchange`

发送语音合成结束请求：
```java
exchange.finishing()
```

服务端收到结束请求后会主动来关闭通道，你可以调用`Exchange.closeFuture()`方法来获取关闭通道的Future。
```java
exchange.closeFuture()
```

### 语音识别

基于上一节 "语音合成" 的示例，我们得到了`audio.wav`，接下来我们可以用语音识别来识别这个音频文件。

```java
// 构建音频文件的ByteBuffer发布器
final var byteBufPublisher = FlowPublishers.fromURI(Path.of("./audio.wav").toUri());

/*
 * 构建语音识别请求
 * 采样率：16000
 * 音频格式：WAV(PCM)
 */
final var request = RecognitionRequest.newBuilder()
    .model(RecognitionModel.PARAFORMER_REALTIME_V2)
    .option(RecognitionOptions.SAMPLE_RATE, 16000)
    .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
    .build();

// 识别文本缓存
final var stringBuf = new StringBuilder();

// 进行语音识别
client.audio().recognition(request)

    // 打开语音识别数据交互通道：全双工模式，输出到文本缓存
    .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofConsume(response -> {
        if (response.output().sentence().isEnd()) {
            stringBuf.append(response.output().sentence().text()).append("\n");
        }
    }))
    
    // 发送音频文件字节流数据
    .thenCompose(exchange -> exchange.writeByteBufferPublisher(byteBufPublisher))
    
    // 语音识别结束
    .thenCompose(Exchange::finishing)
    
    // 等待通道关闭
    .thenCompose(Exchange::closeFuture)
    .toCompletableFuture()
    .join();

// 输出识别文本
System.out.println(stringBuf);
```

文本识别结果为

```text
白日依山尽，黄河入海流。
欲穷千里目，更上一层楼。
```

### 音视频转录

音视频转录不仅能转录音频文件，而且还可以将视频文件中的音频转录为文本。这就省得我们用ffmpeg剥离视频中的音轨这样繁琐的操作了。
在这个例子中，我用我最喜欢的一个动漫《钢之炼金术士》来演示如何从通过音视频转录功能识别视频音轨的文本信息。

> 这对用AI做字幕多少有点启发，毕竟谁没有几个没有字幕的动作片呢？

```java
/*
 * 构建音视频转录请求
 * 语言：日文
 * 选项：过滤语气词（日片中很多以库以库的语气词，各位懂的都懂）
 */
final var request = TranscriptionRequest.newBuilder()
    .model(TranscriptionModel.PARAFORMER_V2)
    // 也可以使用本地文件，本地文件会自动上传到DashScope的临时空间
    .resources(List.of(URI.create("https://ompc-storage.oss-cn-hangzhou.aliyuncs.com/dashscope4j/video/%5Bktxp%5D%5BFullmetal%20Alchemist%5D%5Bjap_chn%5D01.rmvb")))
    .option(TranscriptionOptions.ENABLE_DISFLUENCY_REMOVAL, true)
    .option(TranscriptionOptions.LANGUAGE_HINTS, new LanguageHint[]{LanguageHint.JA})
    .build();

// 进行音视频转录
final var response = client.audio().transcription(request)

    // 等待任务完成，每隔30s进行检查任务状态
    .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L * 30)))
    .toCompletableFuture()
    .join();

// 合并音视频转录文本（当前只有一个视频）
final var text = response.output().results().stream()
    .map(result-> {
    
        // 下载转录结果
        final var transcription = result.lazyFetchTranscription()
            .toCompletableFuture()
            .join();
    
        // 合并转录句子（每行一个句子）
        return transcription.transcripts().stream()
            .flatMap(transcript->transcript.sentences().stream())
            .map(sentence-> "%s - %s: %s".formatted(
                sentence.begin(),
                sentence.end(),
                sentence.text()
            ))
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
    
    })
    
    // 合并多个音视频转录文本，当前只有一个视频
    .reduce((a, b) -> a + b)
    .orElse("");

// 输出音视频转录文本
System.out.println(text);
```

输出摘录
```text
13920 - 19960: で き た あ る 大 丈 夫. 
20060 - 24620: 完 璧 だ. 
28380 - 32940: や る ぞ. 
49480 - 58200: 錬 金 術 と は 物 質 の 構 造 を 理 解 し 分 解 し 再 構 築 す る 科 学 技 術 で あ る. 
58820 - 64900: そ れ は う ま く す れ ば 鉛 り か ら 黄 金 を 生 み 出 す こ と も 可 能 に な る. 
65500 - 71760: し か し 科 学 で あ る 以 上 そ こ に は 大 自 然 の 計 測 が 存 在 し た. 
75300 - 79860: 質 量 が 一 の も の か ら は 一 の も の し か 生 み 出 せ な い. 
80200 - 84760: 強 化 交 換 に 計 測. 
```

例子中采用的是一个远程文件，但实际上你也可以使用一个本地文件，例如`Path.of("./video.mp4").toUri()`，dashscope4j会将视频文件上传到DashScope的临时空间。

### Tokenizer

在实际开发过程中，为了让AI记住上下文，你需要把之前的聊天记录作为对话上下文输入。但模型的输入长度有限，所以需要将上下文进行分段，然后进行分段输入。
有一种分割方案就是根据模型能支撑的最大token进行切分，这个时候你就需要一个工具来计算一段文本的token数到底是多少，以达到最大化保留记忆的分割目的。

Tokenizer工具能很好的帮助你实现这一点，他分远程和本地两种调用方式，远程性能较差但计算的token会更精准，本地性能最佳，但会存在一定的版本滞后性。
不同的模型，同样的文字，计算的token可能不一样。但大差不差。

在实际生产环境中，推荐使用本地计算的方案。

#### 远程计算

```java
final var messages = List.of(
    Message.ofUser("北京有哪些好玩地方？"),
    Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
    Message.ofUser("帮我安排一些行程")
);

// 远程调用需要明确算法模型
final var list = client.base().tokenize().remote(ChatModel.QWEN_PLUS)
    .encode(messages)
    .toCompletableFuture()
    .join();

System.out.println("total tokens: " + list.size());
```

#### 本地计算

```java
final var messages = List.of(
    Message.ofUser("北京有哪些好玩地方？"),
    Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
    Message.ofUser("帮我安排一些行程")
);

final var list = client.base().tokenize().local()
    .encode(messages)
    .toCompletableFuture()
    .join();

System.out.println("total tokens: " + list.size());
```

## 一、主要功能

`DashScope4j`支持以下API功能：

- **对话（Chat）**
    - 提供用户与灵积进行多模态(图、文)对话
    - 提供用户与灵积进行多模态(图、音)对话
    - 提供用户与灵积进行函数对话

- **向量（Embeddings）**
    - 将文本转换为向量表示，用于文本相似度比较、聚类等任务
    - 将图音文本转换为向量表示，用于图音文相似度比较、聚类等任务

- **图像（Images）**
    - **文生图：** 将文本描述转换为相应的图像

- **语音识别与合成**
  - 支持实时语音识别
  - 支持实时语音合成
  - 支持音视频转录

- **基础功能**
  - 支持Tokenizer计算（远程、本地）
  - 支持灵积提供的临时空间
  - 请求、应答拦截器

- **插件应用（Plugin）**
    - **OCR插件：** 图像理解识别，并对图像内容进行总结概述，输出用户可理解的句子或段落
    - **PDF解析插件：** 对PDF文件进行解析，提取、理解文本内容
    - **计算器插件：** 对用户输入的数学表达式进行计算
    - **文生图插件：** 将文本描述转换为相应的图像

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
    .messages(List.of(
        Message.ofUser(List.of(
            Content.ofImage(new File("./document/image/image-002.jpeg").toURI()),
            Content.ofText("图片中一共多少辆自行车?")
        ))
    ))
    .build();

// 异步应答
final var response = client.chat(request)
    .async()
    .toCompletableFuture()
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
    .messages(List.of(
        Message.ofUser(List.of(
            Content.ofImage(new File("./document/image/image-002.jpeg").toURI()),
            Content.ofText("图片中一共多少辆自行车?")
        ))
    ))
    .build();

// 流式应答
final var publisher = client.chat(request)
    .flow()
    .toCompletableFuture()
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

灵积在 2024-03-12 放出了**函数调用**的功能，当前支持的模型是大语言模型：qwen-turbo、qwen-plus、qwen-max、qwen-max-longcontext，下面是一个函数调用的示例：

#### 单个函数调用示例：回显

假设我们有一个回显函数:`echo`

```java
@ChatFn(name = "echo", description = "当用户输入echo:，回显后边的文字")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

  @Override
  public CompletionStage<Echo> call(Echo echo) {
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
    .functions(List.of(new EchoFunction()))
    .messages(List.of(
        Message.ofUser("echo: HELLO!")
    ))
    .build();
final var response = client.chat(request)
    .async()
    .toCompletableFuture()
    .join();
```

输出日志

```text
2024-03-19 21:28:38 DEBUG dashscope://chat/qwen-max => {"model":"qwen-max","input":{"messages":[{"role":"user","content":"echo: HELLO!"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"echo","description":"当用户输入echo:，回显后边的文字","parameters":{"type":"object","properties":{"words":{"type":"string","description":"需要回显的文字"}}}},"type":"function"}]}}
2024-03-19 21:28:40 DEBUG dashscope://chat/qwen-max <= {"output":{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","tool_calls":[{"function":{"name":"echo","arguments":"{\"words\": \"HELLO!\"}"},"id":"","type":"function"}],"content":""}}]},"usage":{"total_tokens":28,"output_tokens":23,"input_tokens":5},"request_id":"8af40d7a-d43d-9d7f-9f12-8d52accfe8ac"}
2024-03-19 21:28:40 DEBUG dashscope://chat/function/echo <= {"words":"HELLO!"}
2024-03-19 21:28:40 DEBUG dashscope://chat/function/echo => {"words":"HELLO!"}
2024-03-19 21:28:40 DEBUG dashscope://chat/qwen-max => {"model":"qwen-max","input":{"messages":[{"role":"user","content":"echo: HELLO!"},{"role":"assistant","tool_calls":[{"function":{"name":"echo","arguments":"{\"words\": \"HELLO!\"}"},"type":"function"}],"content":""},{"role":"tool","name":"echo","content":"{\"words\":\"HELLO!\"}"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"echo","description":"当用户输入echo:，回显后边的文字","parameters":{"type":"object","properties":{"words":{"type":"string","description":"需要回显的文字"}}}},"type":"function"}]}}
2024-03-19 21:28:42 DEBUG dashscope://chat/qwen-max <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"HELLO!"}}]},"usage":{"total_tokens":8,"output_tokens":3,"input_tokens":5},"request_id":"37ff7303-c1b2-9d7c-966d-82a7446fc52e"}
HELLO!
```

#### 级联函数调用示例：成绩查询

我们有两个函数

- 查询成绩函数：[query_score](https://github.com/oldmanpushcart/dashscope4j/blob/main/src/test/java/io/github/oldmanpushcart/test/dashscope4j/chat/function/QueryScoreFunction.java)
- 计算平均分函数：[compute_avg_score](https://github.com/oldmanpushcart/dashscope4j/blob/main/src/test/java/io/github/oldmanpushcart/test/dashscope4j/chat/function/ComputeAvgScoreFunction.java)

现在需要查询某个同学的所有成绩，并计算其平均分。LLM需要先调用 `query_score` 函数查询成绩，然后再调用 `compute_avg_score`
函数计算平均分。

```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_PLUS)
    .functions(List.of(
        new QueryScoreFunction(),
        new ComputeAvgScoreFunction()
    ))
    .messages(List.of(
        Message.ofUser("张三的所有成绩，并计算平均分")
    ))
    .build();
final var response = client.chat(request)
    .async()
    .toCompletableFuture()
    .join();
```

输出日志

```text
2024-03-20 23:50:17 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":"张三的所有成绩，并计算平均分"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"query_score","description":"query student's scores","parameters":{"type":"object","properties":{"name":{"type":"string","description":"the student name to query"},"subjects":{"type":"array","description":"the subjects to query","items":{"type":"string","enum":["CHINESE","MATH","ENGLISH"]}}},"required":["name","subjects"]}},"type":"function"},{"function":{"name":"compute_avg_score","description":"计算平均成绩","parameters":{"type":"object","properties":{"scores":{"type":"array","description":"分数集合","items":{"type":"number"}}}}},"type":"function"}]}}
2024-03-20 23:50:20 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","tool_calls":[{"function":{"name":"query_score","arguments":"{\"name\": \"张三\", \"subjects\": [\"CHINESE\", \"MATH\", \"ENGLISH\"]}"},"id":"","type":"function"}],"content":""}}]},"usage":{"total_tokens":47,"output_tokens":39,"input_tokens":8},"request_id":"4703f631-a245-967e-ba86-8f01327a82bf"}
2024-03-20 23:50:20 DEBUG dashscope://chat/function/query_score <= {"name":"张三","subjects":["CHINESE","MATH","ENGLISH"]}
2024-03-20 23:50:20 DEBUG dashscope://chat/function/query_score => {"message":"查询成功","data":[{"name":"张三","subject":"CHINESE","value":90.0},{"name":"张三","subject":"MATH","value":80.0},{"name":"张三","subject":"ENGLISH","value":70.0}],"success":true}
2024-03-20 23:50:20 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":"张三的所有成绩，并计算平均分"},{"role":"assistant","tool_calls":[{"function":{"arguments":"{\"name\": \"张三\", \"subjects\": [\"CHINESE\", \"MATH\", \"ENGLISH\"]}","name":"query_score"},"type":"function"}],"content":""},{"role":"tool","name":"query_score","content":"{\"message\":\"查询成功\",\"data\":[{\"name\":\"张三\",\"subject\":\"CHINESE\",\"value\":90.0},{\"name\":\"张三\",\"subject\":\"MATH\",\"value\":80.0},{\"name\":\"张三\",\"subject\":\"ENGLISH\",\"value\":70.0}],\"success\":true}"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"query_score","description":"query student's scores","parameters":{"type":"object","properties":{"name":{"type":"string","description":"the student name to query"},"subjects":{"type":"array","description":"the subjects to query","items":{"type":"string","enum":["CHINESE","MATH","ENGLISH"]}}},"required":["name","subjects"]}},"type":"function"},{"function":{"name":"compute_avg_score","description":"计算平均成绩","parameters":{"type":"object","properties":{"scores":{"type":"array","description":"分数集合","items":{"type":"number"}}}}},"type":"function"}]}}
2024-03-20 23:50:24 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","tool_calls":[{"function":{"name":"compute_avg_score","arguments":"{\"scores\": [90.0, 80.0, 70.0]}"},"id":"","type":"function"}],"content":"张三的成绩如下：\n\n- 中文: 90.0分\n- 数学: 80.0分\n- 英语: 70.0分\n\n现在我们来计算他的平均分。"}}]},"usage":{"total_tokens":93,"output_tokens":85,"input_tokens":8},"request_id":"0f662c8b-ca5d-9512-9f92-597045977eca"}
2024-03-20 23:50:24 DEBUG dashscope://chat/function/compute_avg_score <= {"scores":[90.0,80.0,70.0]}
2024-03-20 23:50:24 DEBUG dashscope://chat/function/compute_avg_score => {"avg_score":80.0}
2024-03-20 23:50:24 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":"张三的所有成绩，并计算平均分"},{"role":"assistant","tool_calls":[{"function":{"arguments":"{\"name\": \"张三\", \"subjects\": [\"CHINESE\", \"MATH\", \"ENGLISH\"]}","name":"query_score"},"type":"function"}],"content":""},{"role":"tool","name":"query_score","content":"{\"message\":\"查询成功\",\"data\":[{\"name\":\"张三\",\"subject\":\"CHINESE\",\"value\":90.0},{\"name\":\"张三\",\"subject\":\"MATH\",\"value\":80.0},{\"name\":\"张三\",\"subject\":\"ENGLISH\",\"value\":70.0}],\"success\":true}"},{"role":"assistant","tool_calls":[{"function":{"arguments":"{\"scores\": [90.0, 80.0, 70.0]}","name":"compute_avg_score"},"type":"function"}],"content":"张三的成绩如下：\n\n- 中文: 90.0分\n- 数学: 80.0分\n- 英语: 70.0分\n\n现在我们来计算他的平均分。"},{"role":"tool","name":"compute_avg_score","content":"{\"avg_score\":80.0}"}]},"parameters":{"result_format":"message","tools":[{"function":{"name":"query_score","description":"query student's scores","parameters":{"type":"object","properties":{"name":{"type":"string","description":"the student name to query"},"subjects":{"type":"array","description":"the subjects to query","items":{"type":"string","enum":["CHINESE","MATH","ENGLISH"]}}},"required":["name","subjects"]}},"type":"function"},{"function":{"name":"compute_avg_score","description":"计算平均成绩","parameters":{"type":"object","properties":{"scores":{"type":"array","description":"分数集合","items":{"type":"number"}}}}},"type":"function"}]}}
2024-03-20 23:50:25 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"张三的平均分是 80.0 分。"}}]},"usage":{"total_tokens":68,"output_tokens":13,"input_tokens":55},"request_id":"c01da60a-21d7-9e2f-ae5d-17a9b622ed41"}
张三的平均分是 80.0 分。
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
final var response = client.image().generation(request)
    .task(Task.WaitStrategies.perpetual(Duration.ofSeconds(1L)))
    .toCompletableFuture()
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

我从网上下载了一份[《第十四个五年规划》](https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf)
的PDF文件，然后通过`PDF提取插件`来提取PDF文件的内容。

```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_PLUS)
    .plugins(List.of(ChatPlugin.PDF_EXTRACTER))
    .messages(List.of(
        Message.ofUser(List.of(
            Content.ofText("请总结这篇文档"),
            Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
        ))
    ))
    .build();
final var response = client.chat(request)
    .async()
    .toCompletableFuture()
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
    .plugins(List.of(ChatPlugin.CALCULATOR))
    .messages(List.of(
        Message.ofUser("1+2*3-4/5=?")
    ))
    .build();
final var response = client.chat(request)
    .async()
    .toCompletableFuture()
    .join();
```

输出日志

```text
2024-03-20 00:29:11 DEBUG dashscope://chat/qwen-plus => {"model":"qwen-plus","input":{"messages":[{"role":"user","content":"1+2*3-4/5=?"}]},"parameters":{"result_format":"message"}}
2024-03-20 00:29:15 DEBUG dashscope://chat/qwen-plus <= {"output":{"choices":[{"finish_reason":"stop","messages":[{"role":"assistant","plugin_call":{"name":"calculator","arguments":"{\"payload__input__text\": \"1+2*3-4/5\"}"},"content":""},{"role":"plugin","name":"calculator","content":"{\"equations\": [\"1 + 2 * 3 - 4 / 5\"], \"results\": [6.2]}","status":{"code":200,"name":"Success","message":"success"}},{"role":"assistant","content":"The result of the expression \\(1 + 2 \\times 3 - \\frac{4}{5}\\) is approximately 6.2."}]}]},"usage":{"total_tokens":103,"output_tokens":93,"input_tokens":10},"request_id":"3659340f-7dad-9815-bd11-ce6c6eb4d1c2"}
```

### 向量计算示例

#### 文本向量计算

```java
// 请求
final var request = EmbeddingRequest.newBuilder()
    .model(EmbeddingModel.TEXT_EMBEDDING_V2)
    .documents(List.of(
        "我爱北京天安门", 
        "天安门上太阳升"
    ))
    .build();

// 应答
final var response = client.embedding(request)
    .async()
    .toCompletableFuture()
    .join();
```

输出日志

```text
2024-03-29 00:06:04 DEBUG dashscope://embedding/text-embedding-v2 => {"model":"text-embedding-v2","input":{"texts":["我爱北京天安门","天安门上太阳升"]},"parameters":{}}
2024-03-29 00:06:05 DEBUG dashscope://embedding/text-embedding-v2 <= {"output":{"embeddings":[{"embedding":[-0.04154389871036614,-0.006419809590355932,0.047459534772683644,-0.017466811782486734,0.012682765194211021,-0.008638173113724997,-0.01198812611113586,-0.010318751540519745,-0.011618398857241015,0.024491629606488773,0.009825781868659952,0.014195285778326293,-0.004310683664728525,0.009540083536104844,-0.009002298439530525,0.034126945920111985,0.00946725847094374,0.0031062691255256234,-0.007248894947574674,0.02547756895020836,-0.019539525175533586,0.03952720459821244,0.043605408247234356,-0.010492411311288535,-0.009175958210299316,0.021914742685403495,0.03372360709768125,-0.012133776241458071,0.025141453264849407,0.00010241024788280488,-0.018172654721740525,0.012593134344781967,0.0057531801477273495,-0.006128509329711509,0.07645511456298233,-0.004946502502865871,-0.021522607719151387,-0.015371690677082616,-0.038608488391564645,-0.04893844378826302,-0.0028121679008365425,0.021231307458506966,-0.023079943727981188,0.038675711528636435,0.0338132379471103,-0.000059389190759387267,-0.021455384582079597,-0.02420032934584435,0.015920679629835566,0.02684443940400142,0.01676096884323294,0.09608427058794498,0.0316172821360985,0.00848692105531347,-0.007316118084646463,-0.009344016052978792,0.005722369543236112,0.044255231905594994,-0.003896140986119154,0.019371467332854112,-0.038541265254492856,-0.013993616367110924,-0.018105431584668735,-0.03683827911534084,-0.013354996564928919,-0.004081004613066576,0.023886621372842663,-0.022676604905550447,-0.011797660556099121,0.0057755878600846125,0.0002751947173876398,-0.017545238775737153,0.025163860977206673,-0.015550952375940721,0.043851893083164256,0.0245364450312033,0.02256456634376413,-0.0218811311168676,-0.032401552068602714,0.0064254115184452475,-0.016189572178122724,0.0005696460625823027,0.046115072031247845,-0.0020951211054041174,0.011416729446025646,0.007915524390203257,0.010402780461859482,0.001632962038035562,0.021309734451757385,0.005240603727554951,-0.028278532994866268,0.0161447567534082,-0.005243404691599609,0.008542940336206629,0.03166209756081303,-0.06292085629919532,0.016604114856732095,-0.02263178948083592,0.003719680251305706,0.012021737679671754,-0.030160780832876384,-0.027292593651146686,0.0005973055825232995,-0.0012877432195314746,-0.000897008735301696,-0.024155513921129825,0.006083693904996983,-0.01192090297406407,0.00919836592265658,0.049521044309551863,0.008923871446280105,-0.010946167486523117,0.009579297032730055,0.00006661042618702095,0.031751728410242076,0.01185367983699228,0.017276346227449994,0.0006046581131405266,-0.03291692945281977,-0.0071256525296097255,-0.015069186560259561,0.01118144846627438,0.03990813570828591,-0.030608935080021653,-0.029152433776799536,-0.009321608340621528,0.044008747069665094,-0.05785671330645381,-0.00882303674067242,-0.03428379990661283,0.019113778640745583,-0.019909252429428433,0.03544900094919052,-0.04961067515898092,-0.029331695475657643,0.0056439425499856906,0.050148460255555236,0.010850934709004747,0.04320206942480362,0.023147166865052977,0.02420032934584435,-0.0038205149569133907,-0.016055125903979145,0.003453588667063204,-0.052837385738426834,0.008156407298043837,0.023752175098699084,0.012223407090887123,0.009125540857495474,-0.008077980304793415,0.019954067854142957,-0.05091032247570219,-0.020402222101288222,-0.018923313085708847,-0.009086327360870262,0.010912555917987222,0.009164754354120685,0.0005412863016301413,0.030071149983447332,-0.009624112457444583,-0.07107726359723915,0.031303574163096815,0.001981682061595472,0.003543219516492257,0.002997031527783965,-0.026889254828715944,0.009495268111390318,-0.037846626171417694,-0.009657724025980478,0.005344239397207294,0.018452751126206316,-0.026956477965787734,0.02487256071656225,0.0512688458734184,-0.0119321068302427,-0.006935186974572988,-0.04340373883601899,0.01431852819629124,0.0015153215481599298,0.03621086316933747,0.010010645495607373,0.0026217023457998046,-0.0009068121094579987,-0.030429673381163546,-0.02803204815893637,0.05588483461901464,-0.03524733153797515,0.018251081714990947,-0.015170021265867245,-0.03544900094919052,0.03289452174046251,-0.017758112043131154,-0.04992438313198261,0.053419986259715675,0.036569386567053684,0.003870932309717233,0.042193722368726774,0.04371744680902068,-0.017702092762237998,0.019685175305855798,-0.021477792294436863,-0.014251305059219451,-0.01921461334635327,0.0022197640053913943,0.000709344144309616,0.004428324154604158,0.014273712771576715,0.013668704537930605,-0.020301387395680538,-0.013881577805324606,-0.017623665768987575,-0.0006263655844866254,-0.010582042160717588,-0.012290630227958914,0.009937820430446269,0.017690888906059365,-0.009556889320372791,0.03222229036974461,-0.002870988145774359,-0.007086439032984514,0.02181390797979581,0.010245926475358639,0.0263066543074271,0.02015573726535833,-0.040580367079003817,-0.04499468641338468,0.010335557324787692,0.021511403862972758,-0.03320822971346419,-0.0014382950369318374,-0.03923590433756802,0.017343569364521784,0.02070472621811128,-0.01240266878974523,0.024245144770558877,-0.033409899124679565,-0.012290630227958914,0.007383341221718253,-0.015595767800655249,-0.008430901774420312,0.04750435019739817,-0.007332923868914411,-0.0005913535339284015,-0.011685621994312804,0.02778556332300648,-0.024424406469416984,-0.02467089130534688,-0.011629602713419646,0.003910145806342444,0.018172654721740525,-0.014565013032221137,-0.0032575211839371504,0.0148002940119724,-0.02413310620877256,-0.0025712849929959624,-0.02482774529184772,0.016391241589338097,-0.019124982496924216,-0.020357406676573698,0.018452751126206316,0.03114672017659597,-0.03486640042790168,0.0484006586916887,0.014755478587257875,-0.0061621208982474045,-0.00977536451585611,0.00022950399140915758,0.0075345932801297805,-0.00484006586916887,-0.01977480615528485,-0.01779172361166705,0.005178982518572477,0.004389110657978946,0.0386981192409937,0.024626075880632353,-0.026082577183854466,0.03479917729082989,-0.007232089163306726,-0.03535937009976147,-0.02160103471240181,-0.052434046915996096,0.017119492240949153,-0.0031258758738382285,0.00903591000806642,-0.00899669651144121,0.034126945920111985,-0.009461656542854423,-0.03966165087235602,0.020895191773148015,0.02173548098654539,0.013097307872820391,-0.007327321940825095,0.0012261220105490005,-0.016335222308444936,0.007814689684595571,0.04889362836354849,0.02079435706754033,0.0034479867389738883,0.008845444453029682,-0.0024396396828970405,-0.009461656542854423,0.018979332366602004,-0.01397120865475366,-0.008122795729507942,0.03500084670204526,-0.03385805337182483,-0.005263011439912215,-0.03939275832406886,0.027471855350004792,-0.04252983805408572,-0.005195788302840425,-0.00910313314513821,-0.017287550083628627,-0.002067111464957538,-0.01964035988114127,-0.003260322147981808,-0.026934070253430472,0.0010678675420258286,-0.02482774529184772,0.015943087342192828,-0.028816318091440588,0.037465695061344215,0.012156183953815333,0.014027227935646818,0.0008781022280002551,-0.03258081376746082,-0.038406818980349276,-0.01909137092838832,0.006722313707178986,-0.014677051594007454,-0.015898271917478304,-0.024648483592989618,-0.020525464519253172,0.00163576300208022,0.046025441181818796,-0.04004258198242949,-0.018239877858812318,-0.00014057338299126892,0.01273878447510418,0.03168450527317029,0.018564789687992633,0.020603891512503594,-0.005377850965743189,-0.03020559625759091,0.02868187181729701,0.0002687174880343683,0.023326428563911084,0.03910145806342444,0.01141112751793633,0.030228003969948174,0.0015405302245618511,0.020357406676573698,0.023124759152695712,0.0028527818794840823,0.03141561272488313,-0.02291188588530171,-0.021511403862972758,0.06991206255466145,-0.02271021647408634,-0.03031763481937723,-0.0034087732423486775,0.02467089130534688,-0.0256344229367092,0.005299423972492768,-0.04965549058369544,0.004658003206266106,-0.005324632648894689,-0.001445297447043482,-0.08963084942905315,-0.036502163429981895,-0.023214390002124767,-0.02720296280171763,0.031998213246171976,0.016391241589338097,-0.018363120276777264,0.01756764648809442,-0.022642993337014552,-0.018038208447596946,0.033163414288749665,-0.007478573999236622,0.021085657328184754,0.0067279156352683025,0.006078091976907667,-0.014553809176042506,-0.013119715585177655,-0.01756764648809442,0.01865442053742169,-0.019181001777817373,-0.012010533823493123,-0.011175846538185064,-0.028435386981367113,-0.016391241589338097,0.002071312911024525,0.018295897139705475,0.031034681614809653,-0.0028737891098190166,0.01141112751793633,0.018788866811565268,-0.005758782075816665,-0.021578627000044547,0.03004874227109007,-0.019853233148535272,-0.0296005880239448,-0.014878721005222823,-0.005949247630853403,0.015517340807404826,-0.028099271296008164,0.014441770614256189,-0.10576440232628272,0.004851269725347502,-0.001069968265059322,0.009898606933821058,0.022855866604408553,-0.0055235010960654005,0.002205759185168105,-0.019561932887890852,-0.00378130146028818,-0.05077587620155861,-0.0035124089120010206,0.03565067036040589,0.02446922189413151,-0.004677609954578711,0.009752956803498846,-0.007557000992487043,-0.040714813353147396,0.006627080929660617,0.00003549346625339946,-0.0014284916627755346,-0.013175734866070814,0.013724723818823763,0.008806230956404473,0.021780296411259916,-0.0006932386010528329,0.009327210268710844,-0.03591956290869305,0.0009719345234962951,0.01513640969733135,0.04031147453071666,-0.021040841903470227,-0.011018992551684221,0.0333874914123223,-0.03264803690453261,0.014957147998473244,-0.010184305266376164,-0.027023701102859524,0.051492922996991035,0.012850823036890495,0.01165201042577691,0.030788196778879756,0.006251751747676457,-0.10737775761600568,0.015080390416438192,0.012581930488603336,0.0013787745509828567,-0.027315001363503948,0.01887849766099432,-0.0070528274644486195,-0.006677498282464459,-0.00620693632296193,0.02370735967398456,0.019393875045211374,0.014957147998473244,-0.0018192261470053133,0.026709993129857838,-0.01267156133803239,-0.02964540344865933,0.019685175305855798,0.02841297926900985,0.018508770407099476,-0.023393651700982874,0.01915859406546011,-0.007164866026234936,0.022351693076370128,-0.01704106524769873,-0.02167946170565223,-0.018732847530672107,-0.043851893083164256,-0.015338079108546721,-0.02037981438893096,-0.017354773220700417,0.023595321112198243,0.011898495261706806,-0.012391464933566599,-0.02241891621344192,-0.021892334973046233,-0.018127839297026,-0.007019215895912725,0.03370119938532398,-0.03961683544764149,0.006526246224052933,0.036883094540055374,-0.0201109218406438,0.00009750856080465353,-0.03264803690453261,0.02271021647408634,0.010794915428111588,-0.000307755924406788,0.004380707765844973,-0.026149800320926255,0.016536891719660305,0.007741864619434466,0.011808864412277752,-0.04286595373944467,-0.017612461912808946,-0.027090924239931313,-0.02079435706754033,0.01240266878974523,0.02323679771448203,0.01322055029078534,0.052568493190139676,0.0038877380939851803,0.016604114856732095,0.03482158500318715,-0.033028968014606086,0.023752175098699084,-0.04140945243622256,-0.002676321144670634,-0.010906953989897905,0.027023701102859524,-0.04116296760029266,0.026799623979286893,0.006145315113979457,0.014912332573758718,0.051627369271134614,-0.02172427713036676,-0.016447260870231253,0.0026048965615318572,0.017534034919558524,0.0008185817420512745,0.014957147998473244,0.02181390797979581,-0.03551622408626231,0.018273489427348213,0.015170021265867245,-0.008274047787919469,-0.016615318712910727,-0.013265365715499867,-0.014508993751327979,-0.014195285778326293,0.009607306673176635,0.03695031767712716,-0.02133214216411465,-0.019248224914889166,-0.00034819484280153653,0.011864883693170911,-0.012369057221209335,-0.024558852743560563,0.015786233355691986,-0.01323175414696397,0.009338414124889475,-0.015304467540010826,0.02460366816827509,0.010262732259626586,0.019909252429428433,-0.010206712978733427,0.012055349248207649,-0.05068624535212956,0.012537115063888809,0.003680466754680495,0.036614201991768215,-0.0063301787409268786,-0.008173213082311784,-0.017511627207201258,-0.03690550225241263,-0.013590277544680184,0.026777216266929627,0.00722648723521741,-0.05037253737912787,-0.00661027514539267,0.009472860399033054,-0.026530731430999734,0.03222229036974461,-0.01826228557116958,-0.01226822251560165,0.0108341289247368,-0.025992946334425414,0.016548095575838938,0.02556719979963741,-0.00046846123646903564,-0.011685621994312804,0.02174668484272402,0.0063413825971055104,0.017085880672413258,-0.03627808630640926,-0.025522384374922883,-0.014329732052469872,0.03349952997410861,-0.011220661962899591,-0.001634362520057891,0.02814408672072269,-0.000595204859489806,0.008117193801418626,-0.013859170092967343,0.00154193070658418,0.021410569157365073,-0.026597954568071524,0.014587420744578401,-0.01867682824977895,0.031101904751881442,-0.009299200628264264,-0.022385304644906026,-0.013186938722249445,-0.04248502262937119,0.013399811989643446,0.01158478728870512,-0.04230576093051309,-0.006470226943159774,0.005008123711848345,0.03910145806342444,0.022452527781977816,0.026642769992786048,-0.013814354668252817,-0.014419362901898925,-0.03916868120049623,0.03318582200110693,0.013377404277286183,0.007512185567772518,0.01438575133336303,0.017164307665663677,0.0024382392008747114,0.014598624600757032,0.019561932887890852,-0.006290965244301668,0.010380372749502218,-0.042821138314730146,0.006711109851000355,-0.0015223239582715746,0.029466141749801222,-0.018643216681243056,-0.026799623979286893,-0.008559746120474577,0.01964035988114127,-0.013668704537930605,-0.00910313314513821,0.039706466297070545,-0.009836985724838583,-0.021511403862972758,0.000945325365072045,-0.008223630435115627,0.0012331244206606454,-0.027292593651146686,-0.01403843179182545,0.015898271917478304,-0.0010300545274229468,-0.0029438132109354644,0.0022225649694360525,0.0027281389794968055,-0.005943645702764087,0.008744609747421998,0.00950647196756895,0.00820682465084768,-0.018217470146455052,-0.02213881980897613,0.020760745499004436,0.008374882493527153,0.01848636269474221,-0.02290068202912308,0.00033839146864523385,0.01308610401664176,0.006212538251051247,0.006380596093730721,-0.010660469153968009,-0.022474935494335078,-0.006201334394872615,-0.025007006990705828,0.018665624393600318,-0.012862026893069128,-0.015248448259117668,0.00422385377934413,0.02146658843825823,-0.009114337001316843,0.03500084670204526,-0.006980002399287514,0.009047113864245053,-0.0148002940119724,-0.011786456699920489,0.00024000760657662474,0.003588034941206784,0.031236351026025025,0.04125259844972171,0.03811551871970485,-0.04255224576644298,-0.015506136951226195,0.0005297323249459274,-0.014352139764827136,0.026956477965787734,0.001987283989684788,-0.03831718813092022,0.009685733666427056,-0.04035628995543118,-0.014845109436686928,-0.06023193081632372,0.0030978662333916497,0.0316172821360985,0.03098986619009513,-0.009556889320372791,-0.0010132487431549994,0.011876087549349542,0.016312814596087674,-0.00006674172137661429,-0.027628709336505634,0.05158255384642009,-0.009932218502356953,0.01513640969733135,-0.008318863212633995,0.04696656510082385,0.005178982518572477,-0.048086950718687016,-0.005515098203931427,0.00489328418601737,-0.05212033894299441,-0.012503503495352914,-0.05350961710914473,-0.04559969464703079,-0.021769092555081283,-0.030967458477737863,-0.00977536451585611,0.018228674002633685,-0.0022071596671904337,0.030608935080021653,0.004803653336588318,-0.03907905035106717,-0.008234834291294258,-0.0667749828246446,-0.03934794289935433,-0.06139713185890141,-0.010128285985483007,-0.020323795108037803,0.00780348582841694,0.006862361909411882,-0.0071872737385922,-0.026485916006285207,0.026866847116358682,-0.0568259585380197,-0.033477122261751355,0.0137023161064665,-0.027763155610649213,0.012862026893069128,-0.03903423492635265,-0.009646520169801845,0.015326875252368088,-0.0010657668189923352,0.049252151761264705,-0.01350064669525113,0.011237467747167539,0.008010757167721625,0.001639964448147207,-0.020839172492254858,0.06368271851934226,0.01199932996731449,-0.0037841024243328377,-0.0014971152818696535,0.028525017830796164,0.006890371549858461,0.010486809383199219,-0.01465464388165019,-0.0022211644874137234,0.006257353675765773,0.00023493085924568228,-0.012503503495352914,-0.024043475359343508,0.01957313674406948,0.0510895841745603,0.026351469732141627,-0.0077866800441489924,0.03414935363246925,-0.008346872853080575,-0.029017987502655957,0.05951488402089129,0.0074617682149686745,0.03278248317867619,-0.009332812196800159,-0.0395496123105697,-0.005699961830878849,-0.04344855426073352,0.026665177705143314,0.001364769730759567,0.060276746241038244,0.048086950718687016,0.032020620958529235,-0.007153662170056305,-0.004686012846712685,0.02323679771448203,-0.009091929288959579,-0.01779172361166705,0.03269285232924714,-0.003996975691726839,-0.010453197814663324,0.010408382389948798,-0.019124982496924216,-0.036681425128840005,-0.011399923661757698,0.000722648723521741,0.020883987916969386,0.025791276923210045,-0.005842810997156402,0.025970538622068152,0.015427709957975773,-0.008374882493527153,-0.03383564565946756,-0.026889254828715944,-0.012660357481853757,0.007506583639683201,-0.0038821361658958648,0.0163576300208022,0.004226654743388788,-0.013926393230039134,-0.03773458760963137,-0.0016973842110626941,0.004601983925372948,0.020021290991214746,0.0013346593672794946,0.0020349003784439724,-0.011063807976398748,-0.04396393164495057,0.023684951961627294,0.017287550083628627,-0.01397120865475366,0.03388046108418209,0.028054455871293637,0.02114167660907791,-0.007669039554273361,0.01969637916203443,0.03459750787961452,0.042955584588873726,-0.03842922669270654,-0.017197919234199572,0.011159040753917117,-0.013668704537930605,0.03253599834274629,0.015102798128795456,-0.0057531801477273495,0.03278248317867619,0.03842922669270654,0.003946558338922997,-0.04503950183809921,-0.02384180594812814,0.03524733153797515,0.012391464933566599,-0.007579408704844307,0.008122795729507942,-0.005355443253385926,0.026015354046782676,0.048221396992830595,-0.0014957147998473244,-0.04557728693467353,0.050148460255555236,0.018027004591418316,-0.008901463733922841,0.017186715378020943,0.04212649923165498,-0.006442217302713195,0.03081060449123702,0.056108911742587274,-0.007512185567772518,-0.0008837041560895709,-0.0019088569964343662,-0.049521044309551863,0.0023696155817805927,-0.018318304852062737,0.02774074789829195,0.010212314906822744,-0.03141561272488313,0.014172878065969029,0.05857376010188623,-0.01630161073990904,-0.005579520376958558,-0.014520197607506611,0.008778221315957893,-0.021589830856223177,0.058349682978313604,0.005954849558942718,0.031751728410242076,0.0008059774038503139,0.07058429392537936,0.025007006990705828,-0.025880907772639097,-0.0004730128030416047,-0.0076074183452908865,-0.0036776657906358373,0.019113778640745583,-0.02249734320669234,0.01881127452392253,0.02296790516619487,-0.00930480255635358,-0.009921014646178321,-0.05557112664601296,0.014688255450186085,0.03134838958781134,-0.041050929038506344,0.00964091824171253,0.03813792643206212,-0.03504566212675978,-0.03990813570828591,-0.003361156853589493,-0.015494933095047564,0.02243012006962055,0.005960451487032035,0.043851893083164256,-0.0074617682149686745,0.016884211261197886,-0.0028527818794840823,-0.02964540344865933,-0.006358188381373458,-0.016133552897229567,0.004747634055695159,0.018553585831814004,-0.005938043774674771,0.03318582200110693,-0.001476108051534719,-0.03589715519633579,-0.010626857585432114,0.02079435706754033,-0.010817323140468852,-0.002983026707560675,0.04844547411640323,-0.0035152098760456783,0.0014368945549095083,0.0008017759577833271,0.006240547891497826,0.03748810277370148,0.004781245624231054,0.040087397407144024,-0.04575654863353163,-0.009142346641763421,-0.0012289229745936585,-0.021421773013543702,-0.04562210235938805,0.0012275224925713294,-0.03782421845906043,0.006044480408371772,0.016200776034301357,0.016133552897229567,-0.03004874227109007,0.024021067646986246,-0.006178926682515352,0.0026273042738891206,-0.009327210268710844,0.035336962387404205,-0.052568493190139676,0.03253599834274629,0.002425634862673751,-0.011943310686421332,-0.01787015060491747,0.005131366129813293,0.017466811782486734,0.013142123297534919,-0.010839730852826116,0.007036021680180672,0.00886225023729763,0.03455269245489999,-0.01472186701872198,0.001104980315617546,-0.03986332028357139,-0.007248894947574674,-0.0163576300208022,0.03948238917349791,-0.03755532591077327,-0.021096861184363387,0.0013752733459270342,-0.035157700688546095,-0.030564119655307125,0.002012492666086709,0.02399865993462898,-0.00009033109044021762,-0.01602151433544325,0.03208784409560103,0.009013502295709158,-0.054809264425866,-0.004251863419790709,-0.0029354103188014907,-0.04880399751411944,-0.0052574095118228985,0.049565859734266395,0.024155513921129825,0.006750323347625565,-0.005386253857877163,-0.0003277127932249756,-0.02012212569682243,-0.047056195950252906,-0.007809087756506256,0.05705003566159233,0.037779403034345904,0.006873565765590514,0.04781805817039986,0.015394098389439878,0.015875864205121038,0.00485407068939216,-0.005226598907331662,0.006711109851000355,-0.0016875808369063913,0.0012814410504309944,-0.01323175414696397,-0.015416506101797142,0.010094674416947112,0.07009132425351956,-0.020805560923718963,-0.029891888284589226,0.018116635440847368,-0.008061174520525467,0.04669767255253669,0.005363846145519899,-0.025387938100779304,0.015170021265867245,0.04125259844972171,-0.03170691298552755,0.01756764648809442,0.04140945243622256,0.025992946334425414,0.005120162273634661,0.015315671396189457,0.01465464388165019,0.029107618352085012,0.02201557739101118,-0.00402218436812876,0.009657724025980478,0.006335780669016195,-0.020480649094538644,-0.00301663827609657,-0.0014179880476080675,0.0074617682149686745,0.005576719412913901,0.013993616367110924,-0.06520644295963617,0.0231919822897675,0.013646296825573343,-0.03031763481937723,0.006699905994821723,0.03455269245489999,0.03986332028357139,0.05978377656917845,0.027023701102859524,-0.08165370382986742,0.015013167279366402,0.005803597500531192,0.052254785217137986,-0.002229567379547697,-0.03132598187545407,0.014116858785075872,-0.05377850965743189,-0.017153103809485048,-0.011573583432526489,-0.027315001363503948,0.14905610260051538,-0.00422385377934413,-0.006072490048818351,-0.0042462614917013934,0.04499468641338468,0.041812791258653295,0.0010482607937132233,-0.0036776657906358373,-0.01574141793097746,-0.04477060928981205,-0.009388831477693317,0.017141899953306415,0.00435830005348771,0.04452412445388215,-0.007837097396952835,0.019080167072209688,-0.015371690677082616,-0.012369057221209335,0.07833736240099246,-0.025858500060281835,0.03197580553381471,0.01084533278091543,-0.005086550705098766,0.003870932309717233,0.010117082129304374,0.02554479208728015,-0.02848020240608164,0.03289452174046251,0.0035768310850281524,0.011237467747167539,-0.055660757495442005,0.03466473101668631,-0.0000625840403728252,0.05055179907798598,-0.016178368321944095,-0.004879279365794081,0.03392527650889662,0.0015601369728744565,-0.018127839297026,-0.0007366535437450305,0.02794241730950732,0.025119045552492145,-0.07125652529609726,0.023729767386341822,0.001450899375132798,0.017903762173453366,0.0012891437015538035,0.021993169678653918,-0.010464401670841955,0.06991206255466145,-0.027292593651146686,-0.005164977698349188,0.05633298886615991,0.015394098389439878,0.0019606748312605378,0.016772172699411572,0.046294333730105955,0.020469445238360015,0.022441323925799183,-0.030855419915951546,0.005120162273634661,0.00048316629770348967,-0.004512353075943895,0.0013227552700896985,-0.0005822504007832632,0.020189348833894224,-0.013377404277286183,0.018643216681243056,0.013052492448105866,0.03728643336248611,0.026597954568071524,-0.015999106623085988,0.009808976084392004,-0.01039157660568085,-0.031303574163096815,0.001665173124549128,-0.0057531801477273495,-0.020211756546251486,-0.021634646280937704,0.02541034581313657,0.0006806342628518724,-0.017007453679162836,0.03159487442374124,-0.026261838882712572,0.006940788902662303,-0.008895861805833525,0.0064254115184452475,0.033028968014606086,-0.007271302659931937,-0.01704106524769873,0.013063696304284497,0.05382332508214642,0.010341159252877007,-0.02924206462622859,-0.016324018452266303,-0.0010615653729253484,0.011864883693170911,-0.029264472338585854,0.04434486275502405,-0.05229960064185251,-0.019315448051960955,0.04183519897101056,-0.01738838478923631,0.0011161841717961774,0.005747578219638033,0.008722202035064734,-0.00896868687099463,-0.004019383404084103,0.030720973641807967,-0.007820291612684887,0.007848301253131467,-0.024088290784058036,0.005394656750011137,-0.017970985310525156,0.009433646902407843,-0.01717551152184231,0.028816318091440588,-0.058349682978313604,0.015853456492763776,0.023124759152695712,0.0024592464312096457,0.017074676816234625,0.018923313085708847,0.00529662300844811,-0.002299591480664145,-0.00804436873625752,-0.012615542057139231,-0.007327321940825095,-0.02399865993462898,0.02227326608311971,-0.02283345889205129,-0.06386198021820037,-0.0402218436812876,0.023281613139196557,-0.01513640969733135,0.003966165087235602,0.006346984525194826,-0.05722929736045044,-0.046876934251394796,-0.03025041168230544,0.013982412510932291,0.00954568546419416,-0.030564119655307125,0.008722202035064734,-0.00892947337436942,0.01288443460542639,-0.010346761180966323,-0.0013745731049158699,-0.04102852132614908,-0.0209960264787557,0.03177413612259934,-0.042574653478800246,0.010934963630344484,0.01729875393980726,0.06471347328777638,0.015427709957975773,-0.006027674624103824,-0.044322455042666783,-0.01573021407479883,-0.003529214696268968,-0.0061621208982474045,0.007635427985737465,-0.018015800735239684,-0.014408159045720294,0.04403115478202236,-0.012660357481853757,0.013590277544680184,0.026261838882712572,-0.04893844378826302,-0.025791276923210045,-0.027897601884792792,0.017164307665663677,-0.01826228557116958,-0.00261469993568816,0.004798051408499001,-0.008733405891243367,-0.010738896147218431,-0.01765727733752347,-0.025612015224351938,-0.017545238775737153,-0.0008837041560895709,0.0349112158526162,-0.005206992159019056,-0.04889362836354849,-0.024984599278348566,0.022407712357263288,0.006862361909411882,0.020469445238360015,0.013511850551429762,-0.03435102304368462,0.0009803374156302689,-0.029690218873373857,0.03898941950163812,-0.002837376577238464,-0.0011546974274102239,0.054898895275295054,-0.01955072903171222,0.001305949485821751,0.052254785217137986,0.017702092762237998,0.03267044461688987,-0.013108511728999024,0.015338079108546721,-0.024424406469416984,-0.010049858992232584,0.004072601720932602,-0.0333874914123223,0.022374100788727393,0.009909810789999689,-0.035852339771621264,-0.017668481193702103,0.042955584588873726,-0.011506360295454699,0.009472860399033054,0.021085657328184754,-0.01560697165683388,-0.01315332715371355,-0.014766682443436506,0.02718055508936037,0.011366312093221803,-0.021836315692153073,0.013601481400858815,0.06435494989006016,0.009674529810248425,0.007024817824002041,0.034126945920111985,-0.01120945810672096,0.008122795729507942,0.00838048442161647,-0.07779957730441814,-0.0041622325703616556,-0.03529214696268968,-0.029936703709303753,0.002415831488517448,-0.02700129339050226,0.02774074789829195,0.03374601481003851,-0.03327545285053598,-0.014755478587257875,-0.02718055508936037,0.023886621372842663,0.02610498489621173,-0.027987232734221847,-0.06094897761175614,0.02119769588997107,0.018833682236279792,0.008806230956404473,-0.06601312060449764,0.016794580411768834,-0.013791946955895553,-0.02290068202912308,-0.001004145610009861,0.010677274938235956,0.018721643674493478,-0.021836315692153073,-0.012626745913317862,-0.003926951590610391,-0.002330402085155382,0.0013241557521120274,0.02547756895020836,-0.024177921633487087,0.021455384582079597,-0.03157246671138397,-0.022642993337014552,0.010649265297789378,-0.04571173320881711,-0.026732400842215103,0.005814801356709823,-0.05377850965743189,0.039504796885855176,-0.013545462119965657,-0.08057813363671879,-0.002799563562635582,0.041431860148579816,-0.03092264305302334,-0.003153885514284808,0.0320430286708865,-0.05991822284332203,0.03587474748397852,0.001319254065033876,0.046249518305391424,-0.042821138314730146,-0.011125429185381222,0.0018766459099208003,0.01799339302288242,0.02003249484739338,0.03150524357431218,0.01465464388165019,0.006800740700429408,0.00020236965222653408,0.002450843539075672,-0.0207831532113617,0.03345471454939409,0.01913618635310285,0.033678791672966724,-0.0005752479906716185,-0.05064142992741503,0.0011091817616845328,-0.019875640860892538,-0.002985827671605333,0.013713519962645132,-0.010951769414612431,0.004417120298425526,0.0006365190791485102,0.0025656830649066465,0.021981965822475285,-0.022799847323515397,-0.002425634862673751,-0.011226263890988908,0.01045879974275264,0.016267999171373147,-0.00459918296132829,0.030138373120519122,-0.003985771835548207,-0.002709932713206529,0.00592683991849614,-0.016010310479264617,0.01145034101456154,0.014262508915398082,0.020323795108037803,0.0064086057341773,0.011976922254957228,-0.006257353675765773,-0.02167946170565223,0.027404632212933,0.01519242897822451,0.0010314550094452756,0.02390902908519993,-0.006094897761175614,-0.03482158500318715,0.024177921633487087,0.019315448051960955,-0.02222845065840518,0.026351469732141627,0.021589830856223177,-0.002946614174980122,-0.0013759735869381987,-0.004576775248971027,0.01764607348134484,-0.04506190955045647,-0.013377404277286183,0.05633298886615991,-0.004579576213015684,-0.02003249484739338,0.002988628635649991,0.009484064255211687,-0.05382332508214642,0.019539525175533586,-0.04190242210808235,0.02181390797979581,-0.007730660763255835,0.029421326325086698,-0.02951095717451575,0.016312814596087674,0.04033388224307392,0.016704949562339783,-0.0014270911807532057,-0.0212088997461497,-0.04831102784225965,0.027964825021864582,-0.027763155610649213,0.0008423899364308667,-0.007842699325042151,0.0006564759479666979,0.016940230542091046,-0.0034479867389738883,0.0024382392008747114,0.030228003969948174,-0.026015354046782676,-0.01397120865475366,0.012918046173962285,-0.01969637916203443,0.058663390951315286,0.020872784060790753,-0.00977536451585611,-0.0015629379369191143,-0.021612238568580442,0.01001624742369669,-0.022441323925799183,-0.012895638461605023,-0.016099941328693673,0.013982412510932291,0.005534704952244032,0.032267105794459135,-0.012257018659423018,0.02711333195228858,0.06287604087448079,0.006453421158891827,0.025298307251350252,-0.01301888087956997,-0.02657554685571426,-0.04893844378826302,0.010542828664092377,0.04996919855669713,0.0023766179918922373,-0.03652457114233916,-0.02291188588530171,0.024558852743560563,0.030788196778879756,0.00698560432737683,0.00034364327622896746,-0.006559857792588828,-0.010413984318038113,0.005445074102814979,0.029197249201514064,0.017926169885810632,-0.023416059413340136,0.017410792501593574,-0.011562379576347856,0.0002358061605096379,-0.043291700274232674,0.006761527203804197,-0.005394656750011137,0.000050679943183029075,-0.019449894326104535,-0.02794241730950732,-0.01630161073990904,0.0012779398453751718,-0.000408940750520055,0.05606409631787274,0.010957371342701748,0.046115072031247845,-0.0017183914413976284,0.024850153004204987,0.006459023086981142,0.02814408672072269,0.011259875459524803,-0.021903538829224863,0.015237244402939035,0.03293933716517703,-0.016010310479264617,-0.010979779055059011,-0.027225370514074896,0.0015839451672540487,0.008128397657597257,0.01431852819629124,-0.024289960195273404,-0.027427039925290265,0.022508547062870973,0.033992499645968406,0.01192090297406407,0.034507877030185465,-0.02713573966464584,0.02325920542683929,-0.016133552897229567,0.035673078072763154,0.012436280358281124,0.04831102784225965,0.00579239364435256,0.040804444202576444,0.00038023086906231144,0.017825335180202944,-0.028883541228512378,-0.042910769164159195,-0.02167946170565223,0.03134838958781134,-0.033544345398823144,-0.008638173113724997,0.01142233137411496,0.003680466754680495,0.013926393230039134,-0.0025544792087280146,0.032334328931530924,0.008542940336206629,0.027449447637647527,-0.017914966029632,-0.020682318505754013,0.027830378747721003,0.0019928859177741035,0.002452244021098001,-0.028502610118438902,-0.00889025987774421,-0.004758837911873791,0.019371467332854112,-0.022586974056121395,-0.018564789687992633,0.029466141749801222,0.0028681871817297006,0.005576719412913901,0.0014971152818696535,-0.01573021407479883,0.01025713033153727,-0.004333091377085789,0.025880907772639097,-0.023572913399840977,0.0015503335987181537,0.011685621994312804,0.02397625222227172,0.019237021058710533,0.027471855350004792,-0.036367717155838315,-0.026687585417500576,0.08192259637815458],"text_index":0},{"embedding":[-0.042236045508837534,0.009051396989116752,0.06322843373091525,-0.0011735407452766514,0.03209391235257801,0.008246193850021823,-0.011095813469939335,0.01855964682311006,-0.004514277173436462,0.01183248868230278,0.030586297964485375,-0.0037718913005120596,-0.01262055983971484,-0.00776649836290144,-0.01739467206867484,0.019050763631352354,0.011392767819109096,0.008217640547217037,-0.03106599345160576,0.006675762195758664,-0.00763515316999943,0.046644675461896296,0.036000004176272554,-0.0048626274676548354,-0.034697973568374375,-0.0012477793325690915,-0.0026440358397230635,-0.002833915303374882,0.01926776873266872,0.02727411483913035,-0.010033630605601345,0.011118656112183163,0.022545687894658002,0.000780218999140742,0.027137058985667383,-0.02919289678761188,0.00039153716471061024,0.013568529492833692,-0.011347082534621442,-0.024441627200895707,-0.008645940089288809,0.014185280833417042,-0.01885660117227982,0.03501777055978796,0.035063455844275615,-0.0022071703068098575,0.01606979881853283,0.03213959763706566,0.0020786804441883266,0.01597842824955752,0.01073604185459905,0.14180712304968277,0.03394416637432806,-0.015293148982242688,-0.014048224979954074,-0.01956472308183848,0.005199556440751295,0.015098986523170151,-0.012529189270739528,-0.012734773050933979,-0.028210663171127286,-0.007349620141951583,0.003326459776757418,-0.02530964760616116,-0.01906218495247427,-0.008800127924434646,0.0627715808860387,0.016046956176289005,-0.03140863308526318,-0.002422747742985982,-0.0028610409410394274,-0.006481599736686128,0.013351524391517327,0.000810199967085766,0.04109391339664615,0.03764467441782816,0.007081219095586607,-0.007795051665706224,-0.02176903805836786,-0.020432743487103933,-0.03001523190838968,-0.008314721776753306,0.020763961799639435,0.008857234530044215,0.0024327413989676566,0.009274112750994072,0.004637056375497036,-0.024784266834553124,0.03730203478417074,-0.022260154866610156,-0.023173860556363265,0.031180206662824898,0.02524111967942968,0.01582995107497264,0.025994926873475994,-0.03919797409040844,0.022351525435585468,-0.014573605751562113,-0.026177668011426618,0.008274747152826608,-0.003326459776757418,0.0014583599407543787,-0.031225891947312553,0.009496828512871392,-0.0022899748849437335,0.01913071287920575,-0.0008444639304515077,-0.013500001566102208,-0.0027425447343995706,0.038855334456751024,0.002975254152258566,-0.010970178937598283,-0.02482995211904078,0.023002540739534557,0.014505077824830629,-0.006424493131076558,0.008788706603312732,0.009262691429872159,0.02154061163592958,0.030906094955898963,-0.03147716101199466,0.006612944929588137,0.0291243688608804,0.0008637374098447373,0.004157360888376653,-0.0002787516186317107,0.02482995211904078,-0.009251270108750244,-0.012118021710350629,-0.011032996203768809,0.016823606012579147,0.012392133417276562,0.02380203321806853,-0.02594924158898834,-0.028005079390932837,0.009354061998847469,0.02492132268801609,0.0062131986903211514,0.04943147781564328,0.01771446906008843,0.018970814383498957,-0.043789345181417826,-0.012243656242691681,-0.0019930205357739724,-0.044611680302195623,-0.0017460344665125847,0.015784265790484985,0.00603045755237053,0.00015302785721939301,0.028073607317664322,0.052766503583242134,-0.022305840151097812,-0.019781728183154844,-0.017017768471651682,0.006778554085855888,0.03625127324095466,-0.010741752515160005,0.000868020405265455,0.016812184691457233,-0.02727411483913035,-0.04107107075440232,0.08789848735424924,-0.002411326421864068,-0.012586295876349098,-0.02645177971835255,0.022579951858023743,-0.020752540478517524,0.0011528396007431825,-0.011055838846012637,0.013842641199759625,0.010164975798503355,-0.028347719024590254,0.021197972002272164,0.02029568763364097,0.001523318704635264,-0.010713199212355222,-0.02236294675670738,0.01547589012019331,-0.0006991990024321654,0.06930457656777343,-0.018639596070963455,0.02215736297651293,-0.004362944668571103,-0.039769040146504134,-0.022694165069242885,0.0640964541361807,-0.02359644943787408,-0.028027922033176667,-0.010427666184307374,-0.012597717197471011,-0.0018488263566098096,0.0026554571608449775,-0.06103554007550779,0.021255078607881735,0.02315101791411944,-0.015338834266730344,0.04623350790150739,0.01950761647622891,0.007983503464217803,0.003212246565538279,-0.03631980116768614,-0.003277919161989284,0.009748097577553499,-0.021426398424710443,0.006818528709782588,-0.0028267769776736855,-0.0015404506863181348,0.004151650227815696,-0.026063454800207476,-0.010878808368622973,-0.011204316020597517,0.014573605751562113,0.011901016609034264,0.016846448654822977,-0.009245559448189286,0.057974626014834864,0.005476523477957706,0.03689086722378184,-0.02348223622665494,0.012883250225518858,0.027639597115031594,0.0010464785477953594,0.026634520856303173,-0.013180204574688619,-0.038969547667970166,-0.022522845252414176,-0.013203047216932447,-0.003866117199767849,-0.005442259514591965,-0.031819800645652076,0.002468433027473638,-0.0019159266182010536,0.023527921511142597,0.0017674494436161732,0.011615483580986418,-0.011992387178009576,0.016263961277605366,0.0028253493125334466,0.012129443031472542,0.018696702676573026,0.03357868409842681,-0.01235786945391082,-0.0009051396989116751,-0.03054061267999772,0.01809137265711159,-0.031134521378337242,-0.021609139562661063,-0.0378502581980226,-0.017371829426431014,0.0030637693909533986,-0.005268084367482778,0.010079315890089001,-0.004011739044072251,-0.01651523034228747,-0.00378902328219493,-0.027228429554642695,0.010244925046356752,0.0012913231193463883,-0.02212309901314719,0.015464468799071396,0.023082489987387954,-0.034560917714911404,0.06606092136914989,-0.006481599736686128,-0.0006356679036915194,-0.024670053623333986,0.010027919945040388,0.008309011116192349,-0.07962945086198359,-0.010444798165990244,0.00199730353119469,0.04207614701313074,0.011147209414987948,0.009656727008578187,0.022568530536901832,-0.031111678736093412,0.029170054145368055,-0.00003473241598988264,-0.0028638962713199057,-0.008914341135653784,-0.0419619338019116,0.013557108171711777,-0.013351524391517327,-0.021049494827687285,-0.021963200517440395,0.0390837608791893,0.028210663171127286,-0.02212309901314719,0.005633566643384022,0.017326144141943358,0.004568528448765553,0.00030212963530312813,-0.0070869297561475635,0.001090022334572656,0.019496195155106998,0.023230967161972836,-0.009114214255287278,0.01879949456667025,0.00640165048883273,0.005279505688604692,0.005550762065250146,0.017280458857455702,-0.005142449835141725,0.008149112620485555,0.022739850353730537,-0.03305330332681877,-0.01220939227932594,-0.01582995107497264,0.06619797722261286,-0.023013962060656472,0.011341371874060485,-0.022991119418412642,-0.007001269847733209,-0.0005464388324265672,-0.034675130926130546,-0.005939086983395218,-0.04536548749624194,-0.00481694218316718,0.019781728183154844,0.021460662388076184,-0.003774746630792538,0.013294417785907758,0.012392133417276562,0.015909900322826038,0.015315991624486516,0.011067260167134552,-0.04486294936687773,-0.009508249833993307,0.003429251666854643,-0.022020307123049965,-0.02725127219688652,0.006989848526611295,-0.021152286717784508,0.022408632041195035,0.0246015256966025,-0.015852793717216467,-0.018970814383498957,-0.011855331324546609,0.009548224457920005,0.03618274531422318,0.011158630736109862,0.024167515493969772,0.002713991431594786,-0.02960406434800078,0.006607234269027181,0.003197969914135887,0.0205697993405669,0.034149750154522505,-0.016755078085847665,0.015544418046924793,-0.003246510528904021,0.042304573435569016,0.013579950813955605,0.0270685310589359,0.03284771954662433,-0.02027284499139714,-0.04810660456550127,0.0392436593748961,0.0007174017329702157,-0.026931475205472934,-0.004148794897535218,0.031842643287895905,-0.026611678214059343,-0.00481694218316718,-0.031728430076676764,0.027228429554642695,0.009456853888944693,0.0010236359055515317,-0.0657411243777363,-0.012883250225518858,-0.009702412293065843,-0.026520307645084035,0.03481218677959351,0.00946256454950565,-0.02992386133941437,0.021129444075540682,-0.012837564941031204,-0.008177665923290339,0.023413708299923456,-0.0007438135380646415,0.03531472490895772,0.03298477540008729,0.01597842824955752,-0.014036803658832161,-0.02380203321806853,-0.02919289678761188,0.048928939686279066,-0.019164976842571492,-0.002154346696621006,0.01515609312877972,-0.03042639946877858,0.015932742965069864,-0.010204950422430054,0.020250002349153313,0.031774115361164416,0.006276015956491678,0.005927665662273304,0.006441625112759429,-0.009702412293065843,-0.01795431680364862,0.015498732762437137,-0.0010400540546642828,-0.029695434916976092,0.0054822341385186634,-0.01570431654263159,0.0040888329616451695,-0.0352918822667139,0.026177668011426618,-0.08753300507834799,-0.019290611374912545,0.0011114373116762446,-0.0006884915138803711,0.024989850614747573,-0.004976840678873974,0.02036421556037245,-0.019473352512863168,-0.020147210459056087,-0.05075635106578529,-0.022328682793341638,0.03828426840065533,0.015110407844292066,-0.00033889201266428843,0.009588199081846703,-0.009485407191749479,-0.06167513405833496,0.0015761423148241158,0.007538071940463162,-0.008674493392093594,-0.031157364020581068,0.013225889859176275,0.002335660169431389,0.00011117942279613044,0.011210026681158475,0.01060469666169704,-0.010142133156259527,0.015384519551218,-0.0050168153028006725,0.04436041123751352,0.019644672329691876,-0.021026652185443456,0.010953046955915412,-0.014607869714927854,0.007155457682879047,-0.027799495610738387,-0.02489848004577226,0.060395946092680605,-0.012883250225518858,0.01764594113335695,0.0007338198820829669,0.0024555840412114846,-0.2982335371354153,0.020192895743543743,0.0020972400910114364,0.009068528970799622,-0.026703048783034655,0.01215228567371637,0.003552030868915217,-0.02195177919631848,0.0025640865918696663,0.020752540478517524,0.004314404053802969,0.04883756911730376,0.010022209284479432,0.0202385810280314,0.010102158532332829,-0.03003807455063351,0.013077412684591394,0.04059137526728194,0.03332741503374471,-0.02292259149168116,0.0007106203235540793,0.001396256507153972,0.008177665923290339,-0.019119291558083836,-0.00826903649226565,-0.030403556826534752,-0.026931475205472934,-0.0578375701613719,-0.0030837567029167482,-0.01821700718945264,0.01627538259872728,-0.014059646301075989,-0.02366497736460556,-0.009965102678869861,-0.04152792359927887,-0.00531948031253139,-0.01573858050599733,0.023265231125338577,0.023265231125338577,0.031796958003408246,0.023779190575824703,0.007201142967366702,-0.006150381424150625,-0.031842643287895905,0.005465102156835792,0.027730967684006905,0.0039060918236945475,0.006858503333709286,-0.020044418568958864,0.028416246951321736,0.011341371874060485,0.009371193980530339,-0.006510153039490913,0.015327412945608429,-0.013796955915271969,-0.0025883568992537335,0.025195434394942023,0.005719226551798376,0.06761422104173018,0.045616756560924045,0.0004350809514879069,-0.006019036231248616,0.03104315080936193,0.010404823542063546,0.012986042115616084,-0.0392436593748961,0.00517100313794651,-0.008023478088144503,0.04392640103488079,-0.046119294690288255,0.03787310084026643,0.02718274427015504,-0.014048224979954074,0.04810660456550127,-0.0057706224968469885,-0.03958629900855351,0.012426397380642303,0.014516499145952544,-0.01748604263765015,0.004274429429876271,-0.012997463436737997,-0.022225890903244415,0.008463198951338187,0.00798921412477876,0.018696702676573026,-0.005819163111615123,-0.005567894046933018,-0.02200888580192805,-0.004260152778473878,0.01521319973438929,0.03207106971033418,-0.020398479523738192,0.004822652843728136,-0.004728426944472347,-0.021894672590708913,-0.019484773833985083,-0.0011278554607889958,-0.0033664344006841167,-0.0295355364212693,0.011335661213499527,-0.021106601433296852,-0.003946066447621246,-0.033121831253550255,0.018753809282182593,-0.0037233506857439255,0.007338198820829669,0.004014594374352729,0.0018987946365181829,0.007657995812243258,0.025560916670843266,-0.015510154083559052,0.0202385810280314,0.010107869192893785,-0.034035536943303364,-0.0032379445380625854,-0.014219544796782783,-0.008668782731532636,-0.0772538160686255,0.021346449176857046,0.00233280483915091,-0.03574873511159045,-0.0023342325042911495,0.0011271416282188761,-0.04109391339664615,0.010319163633649191,-0.00008815832240977278,0.025583759313087095,0.04783249285857533,-0.012163706994838284,0.01971320025642336,-0.03526903962447007,0.02070685519402987,0.03339594296047619,-0.04543401542297342,0.0017331854802504316,-0.02074111915739561,0.045479700707461074,-0.014847717458488046,0.018399748327403265,0.04760406643613706,0.010981600258720197,-0.019027920989108528,-0.0037005080435000975,0.0024013327658823935,0.01650380902116556,-0.023916246429287667,-0.0038889598420116765,-0.008668782731532636,0.062223357472186824,-0.008548858859752541,-0.018022844730380107,-0.0038746831906092843,-0.020467007450469674,-0.0013655617066388286,0.01882233720891408,-0.03360152674067064,0.0072182749490495735,0.010216371743551967,-0.0040888329616451695,0.019701778935301447,0.012540610591861442,0.0023813454539190443,0.008668782731532636,0.013876905163125366,0.03287056218886815,0.018434012290769006,0.013991118374344505,0.013762691951906228,-0.008074874033193114,-0.028073607317664322,0.01989594139437398,0.03191117121462739,0.025263962321673505,0.0003615561967655863,-0.04579949769887467,0.013397209676004983,0.01580710843272881,0.011295686589572829,-0.014607869714927854,-0.020181474422421828,-0.008154823281046511,0.0291243688608804,-0.01338578835488307,0.0032807744922697625,0.034195435439010165,-0.012734773050933979,-0.035977161534028725,0.00420019084258383,-0.019918784036617808,0.013911169126491109,-0.030586297964485375,-0.007035533811098951,0.0012627698165416036,-0.001663229888378709,0.006727158140807276,-0.014996194633072927,0.0001914855869345874,-0.0065329956817347405,0.011923859251278092,-0.013111676647957137,-0.006498731718368999,0.01256345323410527,-0.013340103070395414,-0.02150634767256384,0.017931474161404795,-0.0009265546760152636,-0.028758886584979153,0.008149112620485555,0.0061903560480773235,0.0025883568992537335,-0.01838832700628135,-0.008012056767022588,-0.01485913877960996,-0.01491624538521953,-0.029261424714343363,0.03250507991296691,0.02050127141383542,0.0070298231505379945,-0.0024070434264433505,0.041345182461328255,-0.012540610591861442,0.031020308167118104,-0.006841371352026415,0.020489850092713504,0.0133286817492735,-0.004991117330276366,-0.016492387700043645,-0.0026054888809366045,0.023048226024022213,0.02727411483913035,0.028553302804784703,-0.026428937076108723,-0.012197970958204025,0.024373099274164225,-0.017634519812235034,0.022534266573536087,0.011227158662841345,-0.004191624851742394,-0.02489848004577226,-0.045616756560924045,0.022579951858023743,-0.03360152674067064,0.03287056218886815,0.002075825113907848,0.07455838428385382,-0.019930205357739723,0.00784073695019388,-0.015875636359460297,0.013751270630784313,0.005053934596446892,-0.01882233720891408,0.03134010515853169,-0.017543149243259722,0.01650380902116556,0.006424493131076558,0.018993657025742784,-0.005742069194042204,-0.010290610330844408,-0.002682582798509523,0.002998096794502394,-0.008834391887800387,-0.0017374684756711492,-0.04696447245330988,-0.0330761459690626,-0.011541244993693977,-0.04639340639721419,-0.017782996986819913,-0.005396574230104309,-0.019473352512863168,-0.006087564157980099,-0.0027939406794481834,-0.008440356309094359,-0.005810597120773687,-0.046073609405800596,-0.0061560920847115825,-0.06331980429989056,-0.019439088549497427,0.019279190053790633,0.0190050783468647,-0.010370559578697805,0.004148794897535218,0.009833757485967851,0.03821574047392385,-0.02562944459757475,-0.02990101869717054,0.011935280572400007,-0.031819800645652076,0.002138642380078374,-0.030791881744679825,0.018742387961060678,0.0038689725300483273,0.02032995159700671,0.004631345714936079,-0.048563457410377826,-0.02348223622665494,-0.003375000391525552,-0.006315990580418376,-0.017588834527747378,0.06633503307607583,0.004445749246704978,0.03501777055978796,0.0004846923151112204,-0.0006021177728958975,0.02891878508068595,-0.004871193458496271,-0.00869733603433742,-0.0011428459447615077,0.03209391235257801,-0.03291624747335581,-0.000705623495563242,-0.02736548540810566,0.014356600650245748,0.0400659944956739,0.022374368077829294,0.0007530933614761966,0.03207106971033418,0.02441878455865188,-0.06683757120544004,0.04065990319401342,-0.01836548436403752,-0.002541243949625839,-0.006487310397247085,-0.007869290252998665,-0.006053300194614357,0.017200509609602305,0.02027284499139714,0.004988261999995888,0.04251015721576347,0.027593911830543938,0.011072970827695509,0.0053166249822509115,0.018970814383498957,-0.00132558708271213,-0.005447970175152922,-0.030906094955898963,0.08364975589689727,-0.011101524130500293,0.010798859120769576,0.021255078607881735,-0.02339086565767963,0.005350888945616653,-0.007246828251854358,0.00015186787929294865,0.008143401959924598,0.034058379585547194,-0.00015088635950903417,0.04034010620259983,0.0070640871139037355,-0.006224620011443065,-0.02480710947679695,-0.026862947278741448,-0.005013959972520194,0.02380203321806853,0.0007288230540921296,0.0035092009147080403,0.03134010515853169,0.001577569979964355,-0.015041879917560582,-0.02492132268801609,0.022854063564949678,-0.009439721907261823,-0.026086297442451306,-0.013819798557515797,-0.02725127219688652,-0.04328680705205361,0.003828997906121629,0.004180203530620481,-0.003089467363477705,0.023173860556363265,0.013465737602736467,0.011695432828839815,-0.003623414125927179,-0.017371829426431014,-0.006418782470515602,0.01741751471091867,-0.0022500002610170346,-0.0030837567029167482,0.01388832648424728,-0.01476776821063465,0.011495559709206321,0.01768020509672269,-0.04639340639721419,0.023916246429287667,-0.012015229820253404,-0.0027154190967350255,-0.020958124258711974,-0.026291881222645756,-0.003283629822550241,0.007383884105317325,-0.0017831537601588048,0.034058379585547194,-0.004659899017740864,0.024213200778457428,-0.020055839890080775,0.02859898808927236,-0.0120951790681068,0.05505076780762491,0.022203048261000585,0.003320749116196461,0.01865101739208537,0.04308122327185916,0.013145940611322878,0.009565356439602875,0.03474365885286203,-0.0034349623274156,-0.009342640677725556,-0.007041244471659908,-0.027548226546056282,-0.025903556304500683,0.010810280441891489,-0.020147210459056087,-0.010918782992549671,-0.015178935771023548,0.026063454800207476,0.049385792531155624,-0.014699240283903165,0.019416245907253597,-0.03083756702916748,0.009302666053798857,-0.021814723342855512,0.0785786893187675,-0.006555838323978568,-0.02001015460559312,-0.009776650880358282,0.06025889023921764,0.042213202866593705,-0.02150634767256384,-0.0010993021579842111,-0.0034692262907813414,-0.0018259837143659817,-0.00037190676903232076,-0.033532998813939154,-0.019850256109886326,0.02551523138635561,0.011472717066962495,0.012689087766446323,-0.04970558952256921,-0.004791244210642873,0.05834010829073611,-0.019884520073252067,0.023756347933580873,0.04723858416023582,-0.02245431732568269,-0.03346447088720767,0.003520622235829954,0.002070114453346891,0.01206091510474106,-0.009730965595870627,0.011101524130500293,-0.006652919553514836,-0.009525381815676177,-0.0005210977761873209,-0.012015229820253404,-0.015578682010290534,-0.02150634767256384,0.031682744792189105,0.023870561144800014,0.01359137213507752,0.02668020614079083,0.013934011768734936,0.0000027884084770297563,0.02051269273495733,0.02006726121120269,-0.014379443292489576,-0.00927982341155503,0.02604061215796365,-0.007172589664561918,0.011135788093866035,0.002011580182597082,-0.003235089207782107,0.01500761595419484,-0.025172591752698197,0.023253809804216662,-0.015898479001704123,-0.0001541878351458374,-0.00849175225414297,0.01570431654263159,-0.011752539434449384,-0.013145940611322878,-0.041185283965621454,-0.00785786893187675,0.013043148721225653,0.010187818440747183,0.0023042515363461257,-0.00694416324212364,0.007875000913559622,0.03531472490895772,0.000940831327417656,0.025675129882062407,-0.0010143560821399766,0.0000020076541034614245,-0.006949873902684597,0.0026468911700035422,-0.036479699663392935,-0.011289975929011872,0.009833757485967851,0.0015561550028607663,-0.005073921908410242,0.002536960954205121,-0.014128174227807471,0.02071827651515178,0.0040888329616451695,-0.017668783775600775,-0.0258350283777692,-0.019941626678861638,-0.032642135766429874,0.03912944616367696,-0.04075127376298873,-0.024350256631920395,-0.01562436729477819,-0.007509518637658377,-0.03581726303832193,-0.007366752123634454,0.010998732240403068,0.011729696792205556,0.009125635576409191,0.03191117121462739,0.017748733023454172,-0.04390355839263696,0.00681281804922163,-0.013991118374344505,-0.04529695956951046,-0.006704315498563449,0.04390355839263696,0.043789345181417826,0.015395940872339912,0.003586294832280959,-0.011415610461352924,-0.021700510131636375,-0.024784266834553124,0.030380714184290926,0.0334873135294515,0.029147211503124226,-0.0003610208223379966,0.0036890867223781836,0.028644673373760015,0.014265230081270439,0.03010660247736499,-0.011404189140231011,0.026565992929571687,-0.021928936554074654,-0.0010457647152252397,-0.011112945451622207,0.0005956932797648209,-0.0035548861991956957,0.018411169648525176,-0.0006777840253285769,-0.020855332368614747,0.03202538442584652,-0.0019944482009142117,0.0008630235772746176,-0.012003808499131489,-0.03382995316310892,-0.008965737080702398,0.042304573435569016,0.000980092118774235,0.009251270108750244,0.024236043420701257,0.004225888815108136,-0.0014476524522025844,-0.022762692995974367,-0.0026240485277597143,-0.0037661806399511026,0.002718274427015504,0.021643403526026804,0.002828204642813925,0.023870561144800014,-0.020204317064665658,0.00025323210424993436,-0.01786294623467331,0.0038090105941582797,0.014699240283903165,0.007480965334853593,-0.07195432306805745,0.027845180895226043,0.00909137161304345,-0.008029188748705458,0.0068699246548312,0.026497465002840205,0.021609139562661063,0.07136041436971793,0.025149749110454367,-0.0459137109100938,0.0021814723342855513,0.0407284311207449,0.034697973568374375,0.04100254282767084,-0.025583759313087095,0.008822970566678474,-0.026314723864889582,-0.004571383779046031,-0.028142135244395804,-0.047421325298186434,0.25729952223447594,0.018399748327403265,-0.018753809282182593,-0.011301397250133786,0.010970178937598283,0.04906599553974204,-0.011901016609034264,-0.003831853236402107,0.0262461959381581,-0.024670053623333986,-0.0011828205686882063,0.02697716048996059,-0.015818529753850726,0.0545482296782607,0.0021229380635357426,0.04086548697420787,-0.03478934413734968,-0.013168783253566706,0.037941628766997915,-0.012437818701764218,0.01362563609844326,0.03063198324897303,0.004314404053802969,0.020992388222077715,0.023276652446460492,0.023379444336557715,-0.021848987306221257,0.017200509609602305,-0.019484773833985083,0.014276651402392352,-0.01739467206867484,0.014493656503708716,-0.025994926873475994,0.03821574047392385,0.003946066447621246,-0.02727411483913035,0.01624111863536154,0.008834391887800387,-0.02154061163592958,-0.038489852180849785,0.057974626014834864,0.005807741790493209,-0.06738579461929191,0.010598986001136082,0.0004468591888948806,0.019473352512863168,0.001814562393244068,-0.01903934231023044,-0.019164976842571492,0.0315913742232138,-0.025286804963917334,-0.00640165048883273,0.041345182461328255,-0.005670685937030242,0.016115484103020487,0.02245431732568269,0.026497465002840205,0.026817261994253796,0.007520939958780291,-0.03565736454261514,0.00631027991985742,0.015236042376633119,-0.01814847926272116,-0.013488580244980295,-0.01577284446936307,0.015464468799071396,0.013442894960492639,0.040796959047476385,0.0028381982987955995,0.01603553485516709,0.014642133678293597,0.0034692262907813414,-0.0026854381287900013,-0.0024484457155102882,-0.0349720852753003,-0.00388324918145072,-0.02297969809729073,-0.019085027594718095,-0.007315356178585841,-0.003769035970231581,0.004571383779046031,0.007880711574120578,0.03424112072349782,-0.030700511175704513,-0.007338198820829669,-0.02412183020948212,0.011489849048645365,0.004300127402400576,-0.009274112750994072,-0.03641117173666145,0.006527285021173783,0.013934011768734936,0.020444164808225848,0.014413707255855319,0.002367068802516652,-0.009611041724090531,-0.006059010855175314,-0.030791881744679825,0.017874367555795224,-0.04433756859526969,-0.02869035865824767,0.04860914269486548,0.0043429573566077535,0.015658631258143933,-0.01977030686203293,0.012711930408690151,-0.011661168865474074,0.02560660195533092,0.014173859512295127,-0.02219162693987867,0.016115484103020487,-0.002299968540925408,0.01491624538521953,0.011569798296498762,0.04554822863419256,-0.0025398162844855994,0.021883251269586998,-0.0015575826680010057,0.018319799079549868,0.026428937076108723,-0.022876906207193504,-0.004228744145388615,0.01883375853003599,-0.03689086722378184,0.014881981421853787,0.022260154866610156,-0.01338578835488307,-0.020467007450469674,-0.016526651663409386,0.019576144402960394,-0.032619293124186044,-0.0516700567555384,-0.03193401385687121,-0.005308058991409476,-0.025401018175136472,0.0024670053623333985,-0.005342322954775218,-0.03565736454261514,0.010730331194038092,-0.013876905163125366,0.0023485091556935418,-0.0008173382927869621,-0.013203047216932447,0.005099619880934548,-0.00315513995992871,-0.00789784355580345,-0.009565356439602875,-0.009394036622774167,-0.0038204319152801932,-0.02919289678761188,0.03081472438692365,-0.01665228619575044,0.044565995017707964,0.017737311702332257,0.03449238978817992,0.009873732109894551,-0.0380101566937294,-0.02718274427015504,-0.02171193145275829,0.009274112750994072,0.0065444170028566544,0.006635787571831965,-0.010895940350305843,-0.006835660691465458,0.033761425236377436,-0.03442386186144844,-0.015133250486535894,-0.0011399906144810292,-0.03880964917226337,0.006470178415564214,-0.034172592796766335,0.023322337730948148,-0.026223353295914274,0.013808377236393882,-0.020204317064665658,-0.028439089593565565,-0.019918784036617808,-0.006595812947905267,-0.010176397119625268,0.014550763109318285,0.01689213393931063,0.010439087505429288,0.01650380902116556,-0.049477163100130936,-0.044063456888343754,0.0067100261591244055,0.019758885540911014,0.013671321382930916,0.009439721907261823,-0.02439594191640805,-0.004522843164277897,-0.013922590447613022,0.023961931713775322,-0.022374368077829294,0.00012295766020310414,0.021494926351441925,0.014310915365758094,-0.0060247468918095725,0.031180206662824898,0.022899748849437334,-0.008497462914703928,-0.02962690699024461,0.020649748588420298,0.004003173053230815,0.0028182109868322503,-0.0036919420526586623,-0.06464467755003257,0.03209391235257801,0.025675129882062407,-0.014002539695466418,-0.004948287376069189,-0.0004043861509727634,0.021928936554074654,0.010130711835137612,0.004225888815108136,-0.012220813600447853,0.0033093277950745474,-0.030997465524874274,0.0017745877693173694,0.00882868122723943,-0.016252539956483455,-0.008920051796214742,0.016366753167702593,-0.014208123475660868,0.04157360888376653,0.028964470365173602,0.0027296957481374178,-0.006761422104173018,0.018285535116184123,-0.038055841978217056,0.017931474161404795,0.00048826147796181843,-0.01521319973438929,0.02736548540810566,-0.03657107023236825,0.0013805521906113405,0.029489851136781643,-0.006093274818541056,0.013842641199759625,-0.006196066708638281,-0.0035777288414395236,0.024967007972503744,-0.019941626678861638,-0.0452512742850228,0.014676397641659338,-0.01704061111389551,0.011689722168278857,-0.05770051430790893,0.0005268084367482778,-0.04534264485399811,-0.0028981602346856475,-0.00826903649226565,0.003800444603316844,0.03104315080936193,-0.005159581816824596,-0.02062690594617647,-0.010804569781330532,-0.02321954584085092,0.006555838323978568,0.015030458596438668,-0.0270685310589359,0.00903426500743388,-0.027502541261568626,-0.02359644943787408,0.021586296920417237,-0.014002539695466418,-0.018548225501988144,0.0005603585675438998,-0.049203051393205,-0.0005046796270745696,-0.004414340613619715,-0.07072082038689076,0.04280711156493323,0.039906095999967105,-0.014265230081270439,-0.002628331523180432,0.013774113273028141,-0.04168782209498567,0.023276652446460492,0.01118147337835369,0.0400659944956739,-0.011598351599303548,0.00018738104965639963,-0.004046003007437992,0.06126396649794606,-0.011855331324546609,0.012677666445324408,-0.006841371352026415,-0.005839150423578472,-0.019644672329691876,0.014482235182586801,-0.008423224327411489,-0.0063902291677108165,0.017463199995406326,0.004888325440179141,-0.01704061111389551,-0.03054061267999772,-0.007737945060096655,-0.007469544013731679,0.008526016217508713,0.002045844145962824,-0.00959390974240766,-0.018171321904964986,-0.01220939227932594,0.0032607871803064133,0.006715736819685362,-0.012677666445324408,-0.019107870236961925,-0.04404061424609993,-0.015041879917560582,0.03446954714593609,0.0034520943090984705,0.04330964969429744,0.007469544013731679,0.013785534594150056,0.009496828512871392,-0.002701142445332633,0.02597208423123217,0.0034206856760132073,0.010850255065818187,0.011295686589572829,-0.004919734073264404,-0.00378902328219493,-0.03574873511159045,-0.028827414511710638,0.026657363498547,-0.004057424328559906,0.002465577697193159,0.0009386898297072972,-0.050893406919248256,0.004188769521461916,-0.00789784355580345,-0.0006635073739261845,0.01000507730279656,-0.011135788093866035,0.010347716936453977,0.0014490801173428238,-0.0026982871150521546,0.019404824586131686,-0.02560660195533092,-0.02503553589923523,0.0500253865139828,0.008508884235825841,-0.00666434087463675,0.0005371590090150122,0.016926397902676374,-0.032185282921553315,0.009565356439602875,-0.023173860556363265,0.02348223622665494,-0.019953047999983552,-0.006675762195758664,-0.0270685310589359,0.01814847926272116,0.037416247995389874,0.018045687372623933,-0.007657995812243258,-0.02079822576300518,-0.025355332890648816,0.021369291819100873,-0.012197970958204025,0.013614214777321347,-0.012323605490545078,0.028256348455614942,0.004285850750998184,0.02816497788663963,0.007241117591293401,-0.017554570564381637,-0.03394416637432806,-0.028027922033176667,0.018114215299355415,-0.019553301760716565,0.04143655303030356,0.0050168153028006725,-0.005119607192897897,0.0037718913005120596,-0.035063455844275615,-0.0005086057062102275,-0.01050761543216077,-0.030654825891216857,-0.0084746202724601,0.014893402742975702,0.010039341266162303,0.04068274583625724,-0.007041244471659908,-0.006881345975953114,0.03255076519745456,0.01015355447738144,0.0014455109544922256,0.016846448654822977,0.00632741190154029,-0.04239594400454433,0.010067894568967086,0.02798223674868901,0.013145940611322878,-0.01935913930164403,0.007361041463073497,0.011484138388084408,0.049203051393205,0.026817261994253796,0.0018416880309086135,-0.00923984878762833,-0.004425761934741629,0.03081472438692365,0.028576145447028533,0.0037290613463048824,0.012323605490545078,0.008731599997703163,-0.010673224588428523,0.01697208318716403,0.009548224457920005,-0.008000635445900675,0.0020130078477373216,-0.005587881358896367,-0.020546956698323075,-0.01866243871320728,-0.021369291819100873,0.007326777499707755,-0.00632741190154029,0.05194416846246434,0.020467007450469674,0.023573606795630253,0.005490800129360099,0.049979701229495146,0.007618021188316559,0.010678935248989479,0.012289341527179337,-0.012118021710350629,0.045114218431559834,-0.0026240485277597143,-0.01146129574584058,-0.04097970018542701,0.0019330585998839245,0.020901017653102403,0.04582234034111849,0.014881981421853787,-0.019850256109886326,-0.03410406487003485,0.022854063564949678,0.031819800645652076,0.006087564157980099,0.022545687894658002,-0.03124873458955638,0.012689087766446323,-0.012666245124202495,0.042761426280445575,0.003392132373208423,0.06035026080819295,0.0014704950944464123,0.027845180895226043,-0.024076144924994464,0.04527411692726663,-0.013420052318248811,-0.02942132321005016,-0.02530964760616116,0.024373099274164225,-0.022020307123049965,-0.006824239370343544,0.026177668011426618,0.012437818701764218,0.020409900844860107,-0.002087246435029762,0.030723353817948343,0.02795939410644518,0.03437817657696078,0.007960660821973976,-0.017611677169991204,0.00591624434115139,-0.006498731718368999,0.004465736558668327,-0.018114215299355415,-0.007343909481390626,0.005527919423006318,0.011358503855743355,-0.004871193458496271,-0.025492388744111784,0.006687183516880578,0.02574365780879389,0.00007232016226024376,-0.006978427205489381,-0.026428937076108723,0.0129175141888846,-0.012243656242691681,0.05075635106578529,-0.03810152726270471,-0.033121831253550255,0.0054822341385186634,0.051715742040026054,0.017840103592429483,0.04139086774581591,-0.02597208423123217,-0.01667512883799427,0.04883756911730376],"text_index":1}]},"usage":{"total_tokens":9},"request_id":"f9ac3f63-7bc3-9057-b566-2b39636baa46"}
```

#### 多模态向量计算

```java
// 请求
final var request = MmEmbeddingRequest.newBuilder()
    .model(MmEmbeddingModel.MM_EMBEDDING_ONE_PEACE_V1)
    .option(MmEmbeddingOptions.AUTO_TRUNCATION, true)
    .contents(List.of(
        Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
        Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
        Content.ofText("一个帅哥在骑自行车念经"),
        Content.ofText("有两个自行车")
    ))
    .build();

// 应答
final var response = client.mmEmbedding(request)
    .async()
    .toCompletableFuture()
    .join();
```

输出日志

```text
2024-03-29 00:07:26 DEBUG dashscope://embeddingx/multimodal-embedding-one-peace-v1 => {"model":"multimodal-embedding-one-peace-v1","input":{"contents":[{"factor":1.0,"audio":"https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav"},{"image":"https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg","factor":1.0},{"factor":1.0,"text":"一个帅哥在骑自行车念经"},{"factor":1.0,"text":"有两个自行车"}]},"parameters":{"auto_truncation":true}}
2024-03-29 00:07:27 DEBUG dashscope://embeddingx/multimodal-embedding-one-peace-v1 <= {"output":{"embedding":[-0.026409149169921875,-0.0069332122802734375,0.019269943237304688,0.019007444381713867,-0.004364192485809326,-0.01785755157470703,-0.0218735933303833,0.0065555572509765625,0.02151799201965332,0.008321762084960938,0.022843360900878906,0.028423309326171875,-0.0106658935546875,0.010956764221191406,-0.010951757431030273,-0.01063084602355957,-0.001979351043701172,-0.01910686492919922,0.020587921142578125,0.004684567451477051,-0.010324716567993164,-0.04344940185546875,0.01678466796875,-0.000095367431640625,-0.008687973022460938,0.012877106666564941,-0.02930450439453125,0.013735294342041016,-0.004124641418457031,0.012449264526367188,0.0027028322219848633,0.013525009155273438,0.006553173065185547,-0.008073210716247559,-0.01627349853515625,-0.01111602783203125,-0.024732589721679688,0.0013022422790527344,0.014744758605957031,0.010503768920898438,0.01639533042907715,-0.007075309753417969,-0.024723052978515625,0.028194427490234375,0.018787384033203125,-0.035709381103515625,0.006737232208251953,-0.0068683624267578125,-0.02841949462890625,0.01554727554321289,0.011457443237304688,0.0052013397216796875,-0.002953052520751953,-0.016448020935058594,-0.02358722686767578,0.00925135612487793,0.03975677490234375,0.019580841064453125,-0.023984909057617188,-0.036394357681274414,0.0417633056640625,-0.020185470581054688,0.0011172294616699219,-0.014080047607421875,-0.006896018981933594,-0.04808187484741211,0.0038938522338867188,0.0037975311279296875,-0.002498626708984375,0.031757354736328125,-0.009670734405517578,0.036128997802734375,0.01656484603881836,0.00674891471862793,-0.013772964477539062,0.0023107528686523438,-0.004352569580078125,0.001644134521484375,0.003993988037109375,0.01825714111328125,0.012478351593017578,0.00908660888671875,0.009018898010253906,0.0026879310607910156,-0.0007171630859375,0.04218292236328125,-0.00258636474609375,-0.0075841546058654785,-0.0029331445693969727,-0.007187962532043457,-0.003221273422241211,-0.004269123077392578,0.00273895263671875,-0.04039764404296875,0.016267776489257812,0.017835378646850586,0.0319366455078125,0.023279190063476562,-0.008258819580078125,-0.0006160736083984375,0.03899955749511719,0.020725250244140625,-0.013138771057128906,0.006677031517028809,0.006755828857421875,-0.020198345184326172,0.009278297424316406,-0.022789716720581055,-0.004502296447753906,-0.01996135711669922,0.029668807983398438,0.023926377296447754,-0.007825851440429688,0.027767181396484375,-0.011407852172851562,0.015226364135742188,-0.031424522399902344,0.0030465126037597656,-0.014542579650878906,-0.018341064453125,0.002925872802734375,0.00046539306640625,-0.024751663208007812,-0.008940696716308594,0.025907516479492188,-0.007537841796875,0.002613067626953125,0.020631790161132812,0.01054292917251587,0.03044891357421875,0.007734775543212891,-0.007664680480957031,-0.013179779052734375,0.01441049575805664,0.004833698272705078,0.002020597457885742,0.005855560302734375,-0.0018285512924194336,0.0139923095703125,-0.027044296264648438,0.003192901611328125,-0.034778594970703125,0.017791748046875,-0.0066224634647369385,0.008411407470703125,0.0017137527465820312,-0.00006186962127685547,-0.014886856079101562,0.0052144527435302734,-0.0014786720275878906,-0.018461227416992188,-0.04895782470703125,0.0076160430908203125,0.0153350830078125,0.03427886962890625,0.003513336181640625,0.01649951934814453,-0.02303600311279297,-0.009775161743164062,-0.0017809867858886719,0.003136873245239258,-0.0077114105224609375,-0.01644134521484375,0.013071060180664062,-0.010477542877197266,0.011406421661376953,0.002091646194458008,0.01175689697265625,0.012221574783325195,-0.012363433837890625,0.0029997825622558594,0.012116432189941406,-0.0047626495361328125,0.00016689300537109375,0.03551959991455078,0.007201671600341797,0.011114120483398438,-0.01018524169921875,0.0110321044921875,-0.024492263793945312,0.004486083984375,0.013619422912597656,0.008311271667480469,0.014752388000488281,0.046459197998046875,0.0063762664794921875,0.01354217529296875,0.03485870361328125,0.03646659851074219,0.01186370849609375,-0.033046722412109375,-0.01435089111328125,-0.022112131118774414,0.016246795654296875,-0.022922515869140625,-0.0011806488037109375,-0.018871307373046875,-0.01584911346435547,0.022504806518554688,0.0061893463134765625,0.012630462646484375,0.03980255126953125,-0.016916275024414062,0.0015468597412109375,0.008334159851074219,-0.043186187744140625,0.013196468353271484,0.0013675689697265625,-0.008810281753540039,0.0033817291259765625,-0.023420333862304688,0.018762588500976562,0.018636703491210938,-0.009860992431640625,0.0012335777282714844,-0.007477760314941406,0.010670185089111328,0.000202178955078125,-0.019802093505859375,-0.023097991943359375,0.021350860595703125,-0.007383361458778381,-0.0074863433837890625,-0.00801849365234375,0.00096893310546875,-0.00327301025390625,-0.05045318603515625,-0.0030531883239746094,-0.006514072418212891,0.002519577741622925,0.004921913146972656,0.019110679626464844,-0.0170745849609375,-0.007849693298339844,-0.015408039093017578,-0.004937529563903809,-0.01512908935546875,0.04047584533691406,0.013519763946533203,0.004108428955078125,0.03105020523071289,0.054172515869140625,-0.005817413330078125,0.01626420021057129,0.01290130615234375,-0.0224761962890625,-0.029872894287109375,-0.008163928985595703,0.013612747192382812,0.007869720458984375,-0.01591813564300537,-0.010005950927734375,0.015138626098632812,-0.002076759934425354,0.020946502685546875,0.0005998611450195312,0.030853271484375,0.01306915283203125,0.021817684173583984,-0.0002288818359375,-0.004387855529785156,0.010220170021057129,-0.014397621154785156,-0.0003261566162109375,0.017181396484375,-0.005928099155426025,-0.015947341918945312,0.0002765655517578125,0.009938716888427734,0.024812698364257812,0.00128173828125,0.0067043304443359375,-0.0019674301147460938,-0.015000343322753906,0.0017549693584442139,-0.003283977508544922,-0.006786346435546875,-0.019143104553222656,0.017856597900390625,0.017751693725585938,-0.041980743408203125,0.008070707321166992,-0.004060149192810059,0.012944698333740234,0.0018754005432128906,0.00983428955078125,0.015464305877685547,-0.02191162109375,-0.04315185546875,-0.011224746704101562,0.0060193538665771484,-0.014934182167053223,0.0077135562896728516,-0.01781177520751953,-0.012964248657226562,-0.023914337158203125,-0.018342971801757812,-0.00267791748046875,0.0005092620849609375,-0.011952459812164307,-0.006848335266113281,-0.02891826629638672,-0.01262962818145752,-0.0012683868408203125,-0.041629791259765625,-0.007033109664916992,0.014697849750518799,0.02813267707824707,-0.019969940185546875,0.013624191284179688,0.020941734313964844,0.028916358947753906,0.01287984848022461,-0.0028486251831054688,-0.00493621826171875,0.007391929626464844,-0.005214214324951172,0.02706623077392578,-0.014042258262634277,0.0023651123046875,0.016432762145996094,0.008610725402832031,0.0056411027908325195,0.022918701171875,-0.0117034912109375,0.018279075622558594,0.017839431762695312,-0.0022039413452148438,0.012465476989746094,-0.022377967834472656,-0.007831573486328125,0.028259873390197754,0.014179229736328125,0.010362625122070312,-0.007918357849121094,-0.002402007579803467,-0.0004825592041015625,0.0062465667724609375,-0.0008120536804199219,0.039936065673828125,-0.013107895851135254,0.0010170936584472656,-0.003047943115234375,0.010544061660766602,-0.0030755996704101562,-0.025849103927612305,-0.010732650756835938,-0.01465606689453125,-0.004939556121826172,-0.01777935028076172,0.0039215087890625,-0.028255462646484375,-0.01673126220703125,0.023319244384765625,0.027498245239257812,0.04931640625,-0.007958054542541504,0.007862091064453125,0.007472991943359375,-0.02761077880859375,-0.010347127914428711,0.0097808837890625,0.00905609130859375,0.002376556396484375,-0.013995170593261719,-0.037250518798828125,0.021175384521484375,-0.024188995361328125,0.007935047149658203,0.00042724609375,0.015501976013183594,-0.0034143924713134766,0.02068328857421875,-0.0322418212890625,-0.019559860229492188,0.004485011100769043,-0.016287803649902344,0.005614757537841797,0.008584976196289062,-0.0239105224609375,-0.018040582537651062,-0.03662109375,0.0032792091369628906,-0.009837508201599121,-0.007625102996826172,0.03132438659667969,-0.0045490264892578125,0.010850191116333008,-0.021097183227539062,0.008755207061767578,-0.0021371841430664062,0.015928268432617188,0.012967467308044434,-0.021305084228515625,0.017067909240722656,0.012088298797607422,-0.017396926879882812,0.01512908935546875,-0.021584510803222656,0.013494908809661865,-0.011016130447387695,-0.005206197500228882,-0.018878936767578125,0.02294445037841797,0.027493000030517578,-0.006791114807128906,-0.00170135498046875,0.017303466796875,-0.0361328125,0.0024857521057128906,-0.002435445785522461,0.023715972900390625,0.030936062335968018,-0.009298086166381836,-0.002009153366088867,-0.0052165985107421875,0.01374053955078125,-0.014021873474121094,0.01277470588684082,-0.020832061767578125,-0.006807804107666016,-0.027975082397460938,0.010005950927734375,-0.03504180908203125,0.006230831146240234,-0.020915985107421875,0.0071489810943603516,-0.007068634033203125,0.006247520446777344,0.01184844970703125,0.011506080627441406,-0.009983062744140625,-0.003406524658203125,-0.019430160522460938,0.011304855346679688,-0.0033006668090820312,-0.024302959442138672,0.017772197723388672,-0.03141975402832031,0.007048130035400391,0.020715713500976562,-0.012560844421386719,-0.0161285400390625,-0.018296241760253906,0.020160675048828125,0.02417922019958496,0.004185676574707031,0.0142974853515625,0.01526641845703125,-0.005260467529296875,-0.038455963134765625,0.0235135555267334,-0.012950897216796875,-0.019367218017578125,-0.021886825561523438,0.00313568115234375,0.0030488967895507812,0.024570465087890625,-0.0036351680755615234,0.0065555572509765625,0.0324859619140625,-0.0024251937866210938,-0.0029175281524658203,0.03600311279296875,-0.02771472930908203,0.044263362884521484,-0.016674041748046875,0.0027513504028320312,-0.005761146545410156,0.00794219970703125,0.0058193206787109375,-0.023189544677734375,-0.005997896194458008,-0.026912212371826172,-0.00792694091796875,0.007104396820068359,-0.022975921630859375,0.022966384887695312,-0.011808395385742188,0.017406463623046875,0.01082611083984375,0.013742208480834961,0.004108428955078125,-0.010831832885742188,0.02187347412109375,-0.004651069641113281,-0.02426910400390625,-0.020263671875,0.0034837722778320312,-0.017427444458007812,-0.01552891731262207,-0.007451057434082031,0.009335517883300781,0.0010988712310791016,0.0023107528686523438,0.013110160827636719,0.04714202880859375,0.019105911254882812,0.029010772705078125,0.015825271606445312,0.0073299407958984375,0.0115203857421875,-0.016527175903320312,-0.02292865514755249,0.00042939186096191406,0.0296630859375,-0.000942230224609375,-0.0010347366333007812,0.019899368286132812,-0.017772793769836426,-0.022586822509765625,-0.00677490234375,0.004417896270751953,-0.019364356994628906,-0.022399425506591797,0.006656646728515625,0.01659679412841797,-0.023227691650390625,0.021284103393554688,-0.0007677078247070312,-0.020326614379882812,-0.014209747314453125,-0.01275634765625,-0.023122131824493408,0.0008492469787597656,-0.01960134506225586,-0.02972412109375,-0.006860196590423584,-0.0008940696716308594,0.04840087890625,-0.017180442810058594,0.0019321441650390625,-0.01644134521484375,-0.0048961639404296875,-0.005269050598144531,-0.013040542602539062,0.008395195007324219,0.006029725074768066,0.023755669593811035,-0.008032798767089844,0.010899364948272705,0.013362884521484375,-0.026454925537109375,0.003951936960220337,-0.021045684814453125,-0.015771865844726562,0.0231475830078125,-0.029165267944335938,-0.0022974014282226562,-0.007033586502075195,0.008241653442382812,-0.012084007263183594,-0.0107269287109375,-0.009366989135742188,-0.002719879150390625,-0.005873680114746094,-0.004998207092285156,-0.0003662109375,-0.026948928833007812,0.031993865966796875,-0.025508880615234375,0.0519561767578125,0.0000667572021484375,-0.007905960083007812,-0.03220367431640625,-0.00836801528930664,-0.007869482040405273,0.017225265502929688,-0.035968780517578125,0.024688720703125,-0.0010194778442382812,0.007033348083496094,-0.00936126708984375,-0.00032806396484375,-0.018140792846679688,-0.0030670166015625,-0.00013065338134765625,-0.012921333312988281,0.002864360809326172,0.02057969570159912,0.0087127685546875,0.010410308837890625,-0.0003848075866699219,0.005535125732421875,-0.03342151641845703,-0.00284576416015625,0.017289161682128906,0.02434539794921875,-0.0035431385040283203,0.020455002784729004,0.016574859619140625,-0.019145965576171875,0.014417171478271484,0.010037422180175781,-0.01385509967803955,-0.020748138427734375,0.027624130249023438,-0.018476009368896484,0.00870513916015625,0.00740814208984375,0.004199028015136719,-0.009980201721191406,-0.041900634765625,-0.038153648376464844,0.004982948303222656,-0.00757598876953125,0.0028977394104003906,0.0015020370483398438,-0.008044242858886719,-0.012212514877319336,-0.009002685546875,-0.009159088134765625,-0.01213836669921875,-0.019840240478515625,0.0112486332654953,-0.0346527099609375,0.02825164794921875,0.004944801330566406,0.017763137817382812,0.0036334991455078125,-0.00782012939453125,-0.02101278305053711,0.007053375244140625,0.004183769226074219,-0.00261688232421875,0.0010809898376464844,0.018781661987304688,0.020746469497680664,0.0051610469818115234,-0.023183822631835938,-0.022640228271484375,0.0073986053466796875,0.009684920310974121,0.004950523376464844,-0.013257980346679688,0.03165626525878906,0.0081329345703125,0.013475418090820312,0.0055694580078125,0.03699684143066406,0.025867462158203125,-0.009598731994628906,-0.010219573974609375,-0.006028175354003906,0.03516507148742676,0.029249191284179688,0.022864937782287598,-0.0036182403564453125,-0.027063369750976562,-0.00665283203125,-0.008031845092773438,-0.014078140258789062,-0.011250078678131104,-0.013495206832885742,0.004352569580078125,-0.012280464172363281,-0.0186004638671875,0.012902259826660156,0.005423903465270996,-0.008781671524047852,-0.020849227905273438,-0.009944915771484375,0.0225677490234375,0.01090240478515625,0.014336436986923218,0.02898406982421875,0.024335384368896484,-0.043773651123046875,0.008069515228271484,-0.004877567291259766,0.008295059204101562,-0.015966415405273438,0.00356292724609375,-0.011985212564468384,-0.021459579467773438,0.007671356201171875,0.006745338439941406,-0.016894817352294922,-0.0011348724365234375,-0.00370025634765625,-0.012965202331542969,0.006458282470703125,0.0167083740234375,-0.00191497802734375,-0.004090487957000732,0.01709747314453125,0.004047870635986328,-0.0015459060668945312,-0.017927169799804688,-0.011654853820800781,0.020573139190673828,0.03529930114746094,0.013696670532226562,-0.005130290985107422,-0.004982471466064453,-0.013752460479736328,-0.023265838623046875,0.0045948028564453125,0.019138336181640625,0.008185625076293945,0.016518354415893555,0.023659706115722656,-0.0009086132049560547,-0.007733345031738281,-0.000621795654296875,0.001605987548828125,-0.010295867919921875,-0.010781288146972656,0.024916648864746094,0.0038330554962158203,-0.0011196136474609375,-0.0009768009185791016,-0.004784584045410156,-0.009212493896484375,-0.016958236694335938,0.027416229248046875,-0.01259756088256836,0.007937908172607422,0.008712291717529297,0.012671470642089844,0.005016326904296875,-0.0017042160034179688,-0.019853591918945312,-0.021303653717041016,0.00078582763671875,0.0345916748046875,0.005578517913818359,-0.01602959632873535,0.02281951904296875,0.008701801300048828,-0.0194549560546875,-0.018331527709960938,-0.00145721435546875,0.017847061157226562,-0.0016632080078125,0.0019140243530273438,-0.010118484497070312,-0.00003361701965332031,-0.019842326641082764,0.006102234125137329,-0.02023601531982422,-0.014788627624511719,0.01056051254272461,-0.0007123947143554688,-0.030268430709838867,-0.010068893432617188,0.01397716999053955,0.0090789794921875,-0.015488147735595703,-0.03184318542480469,0.000957489013671875,-0.016417503356933594,-0.0012082159519195557,0.031581878662109375,-0.003924369812011719,-0.03434181213378906,-0.00199127197265625,0.026735305786132812,0.010692596435546875,0.014389991760253906,0.024695873260498047,-0.010166168212890625,-0.025803565979003906,0.019855499267578125,0.007356166839599609,0.00200653076171875,0.004543304443359375,-0.02131366729736328,0.009784698486328125,-0.0007410049438476562,0.0015878677368164062,0.012359619140625,-0.009090423583984375,0.027805328369140625,-0.029253005981445312,-0.013429641723632812,-0.0191497802734375,0.0207366943359375,0.015712738037109375,0.01129770278930664,0.0070590972900390625,0.052001953125,0.018505096435546875,0.012189865112304688,-0.022455215454101562,-0.006117820739746094,0.00644683837890625,-0.010872960090637207,0.024570465087890625,-0.044330596923828125,-0.024204254150390625,-0.0200042724609375,-0.014544963836669922,0.02239990234375,-0.023189544677734375,-0.008693695068359375,-0.004992961883544922,0.010601043701171875,0.01981377601623535,0.011806964874267578,-0.01589524745941162,0.010772466659545898,0.026300430297851562,0.006534576416015625,-0.019054412841796875,-0.043548583984375,0.015831947326660156,0.00054931640625,0.006204128265380859,-0.0093536376953125,-0.016933441162109375,0.0055446624755859375,-0.017252445220947266,0.0005619525909423828,-0.002444028854370117,-0.01882171630859375,-0.025396347045898438,0.0092010498046875,-0.04595184326171875,0.01120138168334961,-0.0021228790283203125,-0.017965316772460938,-0.009843826293945312,-0.03500652313232422,0.01142120361328125,-0.012362480163574219,-0.010705947875976562,-0.010977983474731445,0.01877307891845703,-0.013124465942382812,0.0017638206481933594,-0.011728405952453613,-0.00019741058349609375,0.013208389282226562,-0.01964569091796875,0.047976016998291016,-0.0044078826904296875,0.00855875015258789,0.009824752807617188,0.0009918212890625,0.011437416076660156,0.013042449951171875,0.0078029632568359375,-0.02964019775390625,-0.010420262813568115,0.0032444000244140625,0.027982711791992188,-0.006988584995269775,0.01195991039276123,-0.03798389434814453,0.022722244262695312,0.0058612823486328125,0.014232635498046875,0.012263298034667969,-0.02658843994140625,0.0013155937194824219,0.0056247711181640625,-0.0434722900390625,0.017958879470825195,0.020740270614624023,-0.022977828979492188,0.021724700927734375,-0.024759292602539062,0.025125503540039062,0.009482383728027344,-0.010997772216796875,0.022123336791992188,0.0013804435729980469,0.0036077499389648438,-0.00286102294921875,-0.0255889892578125,-0.008465290069580078,-0.001964569091796875,0.015351295471191406,0.004029273986816406,-0.002907097339630127,0.012477397918701172,0.014642715454101562,-0.013310432434082031,-0.0283050537109375,0.0030984878540039062,0.007604122161865234,-0.02023792266845703,-0.0023005008697509766,-0.02260589599609375,-0.012882232666015625,-0.020160675048828125,0.024152755737304688,0.024440765380859375,-0.017038345336914062,-0.00025272369384765625,0.03184032440185547,0.012683868408203125,-0.0032466650009155273,-0.015590667724609375,0.007255434989929199,-0.022096633911132812,-0.043609619140625,0.004124164581298828,-0.015163064002990723,0.019342899322509766,0.032405853271484375,0.002512693405151367,0.012612342834472656,0.008920669555664062,-0.011710166931152344,0.004619479179382324,0.00339508056640625,-0.013296842575073242,0.01153564453125,-0.004476815462112427,0.007638275623321533,0.028653621673583984,0.0026903152465820312,0.02442169189453125,0.0036079883575439453,-0.004741668701171875,0.0007185935974121094,-0.02651071548461914,-0.023145675659179688,0.0074405670166015625,-0.010251998901367188,-0.00798797607421875,-0.007457733154296875,0.0030107498168945312,-0.00382232666015625,-0.011152267456054688,-0.0142059326171875,-0.005826473236083984,0.026821136474609375,-0.030930519104003906,-0.01899433135986328,-0.02466869354248047,-0.01697540283203125,-0.0020122528076171875,0.016819000244140625,-0.012147784233093262,-0.00452423095703125,-0.013282418251037598,0.011698484420776367,-0.011749267578125,0.005962371826171875,0.020397186279296875,0.0018553733825683594,0.026247024536132812,-0.021659374237060547,-0.0043926239013671875,-0.022314071655273438,0.0032558441162109375,-0.01929950714111328,0.0070819854736328125,-0.031322479248046875,-0.0072460174560546875,0.02514171600341797,0.023197293281555176,0.017496347427368164,0.016412734985351562,0.015550613403320312,0.029481887817382812,0.0024938583374023438,-0.03358268737792969,0.009153604507446289,-0.0059206485748291016,-0.018990516662597656,-0.011719703674316406,0.02089691162109375,-0.02517986297607422,-0.03409004211425781,0.007758140563964844,-0.019727468490600586,0.0066165924072265625,0.024572372436523438,-0.015870749950408936,0.020519495010375977,0.008495330810546875,-0.01471710205078125,-0.004069328308105469,-0.012205123901367188,-0.01880168914794922,0.0022734403610229492,-0.009032249450683594,-0.01767730712890625,-0.011598587036132812,0.015825271606445312,0.009517669677734375,-0.0142059326171875,0.0017118453979492188,-0.012073516845703125,0.017994403839111328,0.014819145202636719,0.017164230346679688,-0.01837635040283203,-0.021579742431640625,-0.004968404769897461,0.013225555419921875,-0.02767181396484375,0.0015041828155517578,0.0200347900390625,-0.014980196952819824,-0.007397174835205078,0.009478569030761719,0.008846282958984375,-0.0443115234375,-0.021879196166992188,-0.006868839263916016,0.01274871826171875,-0.011800765991210938,-0.021856307983398438,0.001911163330078125,0.01044464111328125,-0.0046634674072265625,-0.002880096435546875,0.006512641906738281,-0.011351346969604492,-0.01728057861328125,-0.014253616333007812,-0.013347625732421875,-0.002254486083984375,0.02197885513305664,-0.03180694580078125,0.01308584213256836,-0.0286712646484375,0.014621734619140625,0.004803180694580078,-0.0016193389892578125,-0.030719757080078125,-0.0024809837341308594,0.01842975616455078,0.003505706787109375,0.008195877075195312,-0.0056934356689453125,-0.01154327392578125,0.017331600189208984,-0.009812355041503906,-0.008294105529785156,0.002582550048828125,0.041065216064453125,0.02294921875,0.011183738708496094,-0.01893782615661621,0.010000228881835938,0.006923675537109375,-0.02275848388671875,-0.009257316589355469,0.029851913452148438,-0.00969696044921875,-0.025770187377929688,0.0031042098999023438,0.013427734375,0.005124151706695557,0.00091552734375,-0.008762836456298828,-0.0047969818115234375,-0.01312565803527832,0.0071773529052734375,-0.007502555847167969,0.0018901824951171875,0.0009469985961914062,-0.009815216064453125,-0.01870870590209961,0.02675914764404297,0.01828479766845703,-0.0006215572357177734,-0.025432586669921875,-0.00489044189453125,0.013131141662597656,0.00298309326171875,-0.004065036773681641,0.018266499042510986,-0.02120208740234375,-0.007919788360595703,0.0031414031982421875,-0.028224945068359375,0.016166329383850098,0.03076648712158203,-0.0521693229675293,0.012002825736999512,0.0062749385833740234,-0.022798538208007812,-0.00193023681640625,0.026035308837890625,-0.016458511352539062,0.030635356903076172,0.009813308715820312,0.012674808502197266,-0.01869058609008789,-0.018732547760009766,-0.019260406494140625,-0.021635055541992188,-0.01958942413330078,0.005859375,-0.01403498649597168,-0.010931015014648438,0.005549430847167969,-0.004682660102844238,-0.01435995101928711,0.018383026123046875,-0.019127845764160156,0.03119659423828125,-0.004809856414794922,0.008137226104736328,0.023308873176574707,0.030050992965698242,-0.01059722900390625,0.0044727325439453125,-0.0158233642578125,-0.007042407989501953,-0.00787973403930664,0.001673579216003418,-0.015542984008789062,0.013258934020996094,0.00954437255859375,0.0653533935546875,0.010868072509765625,0.02274608612060547,0.015302658081054688,-0.01785755157470703,-0.008780479431152344,-0.0075531005859375,-0.014470100402832031,0.003185272216796875,0.014212608337402344,-0.01470184326171875,0.007844924926757812,0.0058155059814453125,-0.019008636474609375,-0.007155418395996094,0.011980444192886353,0.03687477111816406,0.00979304313659668,-0.0086517333984375,0.01589369773864746,-0.02635955810546875,0.01401519775390625,0.004192352294921875,0.031229019165039062,0.034450531005859375,-0.03583335876464844,-0.014777660369873047,-0.0046224892139434814,-0.01515960693359375,-0.0070400238037109375,0.042140960693359375,-0.014858245849609375,0.0026659369468688965,0.017610549926757812,-0.02205657958984375,-0.02759552001953125,-0.03662109375,-0.017461776733398438,-0.039302825927734375,-0.00030803680419921875,-0.002872288227081299,0.001255035400390625,0.023782730102539062,0.01866912841796875,0.00894927978515625,-0.02667236328125,0.0055694580078125,0.00995635986328125,-0.002247333526611328,0.003208160400390625,-0.031032562255859375,0.00023794174194335938,0.021986931562423706,-0.0189666748046875,0.010197639465332031,0.0068035125732421875,-0.007573127746582031,-0.013179779052734375,0.0068416595458984375,-0.007094383239746094,-0.007396697998046875,0.02311992645263672,-0.023059844970703125,0.03047943115234375,0.0030231475830078125,0.0030231475830078125,-0.000011444091796875,-0.024669647216796875,-0.00347137451171875,-0.012431144714355469,0.037899017333984375,0.006854057312011719,0.04059600830078125,-0.01750946044921875,-0.020485877990722656,0.007534027099609375,-0.0014934539794921875,-0.017330169677734375,0.010784149169921875,0.0046863555908203125,-0.00042057037353515625,0.012380599975585938,-0.013477325439453125,0.012972831726074219,-0.005650520324707031,0.006976127624511719,0.008848190307617188,-0.03168678283691406,0.0060863494873046875,-0.012675106525421143,-0.00452423095703125,0.010771751403808594,0.019414424896240234,0.00414276123046875,-0.024505615234375,-0.02331829071044922,-0.006413936614990234,-0.02246570587158203,-0.011936187744140625,-0.008569717407226562,-0.04334449768066406,-0.003168821334838867,-0.05678820610046387,-0.02024078369140625,-0.017319202423095703,0.031622886657714844,-0.0551910400390625,-0.0064067840576171875,-0.008914470672607422,0.013456344604492188,0.028057098388671875,-0.002521514892578125,0.021375656127929688,0.0002593994140625,-0.011912822723388672,0.018951416015625,-0.015659332275390625,-0.005734443664550781,-0.01125335693359375,-0.0048770904541015625,0.00754779577255249,-0.010206222534179688,-0.0027179718017578125,-0.0022835731506347656,0.0066280364990234375,0.0049991607666015625,0.0073337554931640625,-0.0026023387908935547,0.0042896270751953125,-0.008472442626953125,-0.006000518798828125,0.005978986620903015,0.04144096374511719,0.025770187377929688,-0.017887115478515625,-0.019112110137939453,-0.01268148422241211,0.005904197692871094,-0.014520764350891113,0.0240478515625,0.0022497177124023438,0.002758026123046875,-0.0002384185791015625,-0.01725947856903076,-0.0068149566650390625,0.00356292724609375,0.01715397834777832,-0.0022983551025390625,0.024110794067382812,0.0071059465408325195,-0.007490992546081543,0.0064487457275390625,-0.026391029357910156,-0.008091926574707031,0.0054264068603515625,0.008389472961425781,-0.002796173095703125,-0.011179924011230469,-0.0301666259765625,0.01064610481262207,0.022630691528320312,0.002849578857421875,-0.017267227172851562,0.0022830963134765625,-0.016002655029296875,0.009270191192626953,-0.0023946762084960938,-0.016635894775390625,0.014682769775390625,0.030345916748046875,0.025140762329101562,-0.013407230377197266,0.016996145248413086,0.0059642791748046875,0.006434917449951172,0.01766347885131836,-0.03404998779296875,0.013857841491699219,-0.007364749908447266,0.027521371841430664,0.0185086727142334,0.009374618530273438,0.0041637420654296875,-0.001495361328125,0.032068848609924316,0.012919425964355469,-0.0156632661819458,0.0047855377197265625,-0.007779598236083984,0.04093170166015625,-0.02104663848876953,-0.024135589599609375,-0.0039272308349609375,-0.0031518936157226562,-0.028255462646484375,-0.020275592803955078,0.013723134994506836,0.0000514984130859375,0.011342048645019531,-0.009977340698242188,-0.006821915507316589,-0.0029325485229492188,0.025970458984375,0.018266677856445312,0.007988929748535156,0.0057525634765625,0.024163246154785156,0.003002166748046875,-0.03264617919921875,0.008662700653076172,-0.00856637954711914,-0.00021696090698242188,0.009143829345703125,-0.013262271881103516,0.03429412841796875,0.012668609619140625,-0.021894454956054688,0.00560152530670166,-0.02027130126953125,-0.011510848999023438,-0.006572723388671875,-0.003208160400390625,0.0321803092956543,0.0069119930267333984,0.028016090393066406,-0.021007537841796875,0.013442039489746094,0.0347137451171875,0.009145975112915039,0.013235092163085938,-0.0004239082336425781,0.019448280334472656,-0.005435943603515625,-0.013116836547851562,-0.002476811408996582,0.009334564208984375,0.005548834800720215,-0.03663831949234009,0.005269050598144531,-0.017805099487304688,0.0062427520751953125,-0.020352840423583984,0.01363372802734375,0.012795448303222656,-0.01770305633544922,0.010586202144622803,-0.004390239715576172,0.006525993347167969,0.008387565612792969,0.013690412044525146,0.015453338623046875,0.016832351684570312,0.0016205310821533203,-0.008472442626953125,-0.013593673706054688,-0.0007975101470947266,0.00005844235420227051,-0.01666402816772461,0.032901763916015625,-0.001873016357421875,0.01458740234375,-0.013909339904785156,-0.0012216567993164062,0.006198406219482422,-0.011687278747558594,0.008055806159973145,-0.004341334104537964,-0.01659393310546875,0.0040130615234375,0.011870980262756348,0.015612363815307617,-0.004800811409950256,0.01993560791015625,0.0026276111602783203,-0.03765273094177246,-0.014386177062988281,0.005565643310546875,0.01947641372680664,0.027099609375,-0.008671998977661133,-0.0063707828521728516,0.017267227172851562,0.0048940181732177734,0.006175994873046875,-0.019133567810058594,-0.01977825164794922,-0.01381683349609375,-0.01605987548828125,-0.024147987365722656,0.010377883911132812,-0.0238189697265625,-0.0001201629638671875,-0.00151824951171875,-0.035950422286987305,0.0316925048828125,-0.002849578857421875,-0.0216064453125,0.020771026611328125,-0.04282665252685547,-0.0012485980987548828,-0.004303932189941406,-0.0009965896606445312,0.03778266906738281,0.009292125701904297,0.012995719909667969,-0.019092559814453125,-0.0056781768798828125,-0.014499664306640625,0.024135589599609375,0.010506153106689453,-0.018766403198242188,0.0014905929565429688,-0.0062427520751953125,-0.011827349662780762,0.010950088500976562,0.010059237480163574,0.0020339488983154297,0.03912353515625,0.009624004364013672,-0.017357468605041504,-0.002869844436645508,0.01861572265625,-0.02283334732055664,0.008321762084960938,-0.0009095668792724609,-0.0020166486501693726,0.009437084197998047,0.024478912353515625,0.0069217681884765625,0.026760101318359375,-0.019593238830566406,-0.042789459228515625,0.04044198989868164,0.014730453491210938,-0.025165557861328125,-0.006939888000488281,-0.01203155517578125,0.0034481287002563477,-0.015842437744140625,-0.025133132934570312,-0.020143508911132812,0.0173797607421875,-0.00859832763671875,0.01553964614868164,-0.017887115478515625,-0.024831771850585938,0.010150909423828125,-0.021881103515625,-0.0167999267578125,0.011527061462402344,-0.0034637451171875,-0.008426189422607422,-0.024639129638671875,-0.029087066650390625,-0.012368202209472656,0.01458740234375,-0.0134429931640625,-0.024379730224609375,-0.02133941650390625,-0.008868217468261719,0.011730670928955078,0.002190113067626953,-0.003635406494140625,-0.000522613525390625,0.0004253387451171875,-0.02270030975341797,-0.010772228240966797,-0.017958641052246094,0.017289161682128906,0.006549239158630371,-0.017750918865203857,0.008399009704589844,0.0003490447998046875,0.0046291351318359375,-0.015466690063476562,-0.00736236572265625,0.01204681396484375,0.017610549926757812,0.013065338134765625,0.01569366455078125,0.0016980171203613281,0.025362133979797363,0.01245570182800293,0.02231121063232422,-0.016411781311035156,0.009354591369628906,0.0007534027099609375,-0.0071258544921875,-0.033690452575683594,-0.018520355224609375,0.018801212310791016,-0.012007713317871094,-0.030857086181640625,-0.005173683166503906,0.013844490051269531,0.003353297710418701,-0.019059181213378906,-0.016803741455078125,-0.002732515335083008,0.016240954399108887,0.0010290145874023438,0.026058197021484375,-0.0008764266967773438,0.004401206970214844,-0.00554656982421875,-0.011745452880859375,-0.0011081695556640625,0.0005931854248046875,-0.00217437744140625,0.010144233703613281,-0.02487945556640625,-0.012645721435546875,0.009256482124328613,-0.004539966583251953,-0.01030731201171875,0.017576277256011963,0.02353668212890625,0.006230354309082031,0.006053924560546875,0.011409759521484375,-0.011490345001220703,-0.01616954803466797,0.009822845458984375,-0.012165069580078125]},"usage":{"image":{"measure":1,"weight":1},"total_usage":5,"audio":{"measure":1,"weight":2},"text":{"measure":2,"weight":1}},"request_id":"2eeb465c-9081-9cd6-a650-74fbd54ebe89"}
```

### 支持拦截API调用

通过`DashScopeClient.Builder`设置，可实现AOP的功能，限流控制、失败重试等功能均基于此实现。

示例代码

```java
DashScopeClient client = DashScopeClient.newBuilder()
    .ak(AK)
    .executor(executor)
    .appendInterceptors(List.of(
        new Interceptor() {

            @Override
            public CompletableFuture<?> handle(InvocationContext context, ApiRequest<?> request, OpHandler opHandler) {
                return opHandler.handle(request);
            }

        }
    ))
    .build();
```

### 支持无感使用临时空间

在`对话`、`多模态向量计算`和`文档分析插件`等请求中如果需要解析图片、音频、文档等内容，不再需要提前上传到OSS转换为外网可访问的URL连接。这样极不方便也不安全。

通过灵积平台提供的[临时空间](https://help.aliyun.com/zh/dashscope/developer-reference/guidance-of-temporary-storage-space)
可以很好解决这个问题，但操作起来需要调用额外的api且需要对url进行拼接和替换，略为繁琐。

dashscope4j帮你封装了这个繁琐的操作，你只需要设置内容的时候将本地文件直接传入Content，框架会自动识别并帮你完成临时空间上传和转换连接操作。并自带一个缓存避免重复上传。

```java
final var request = ChatRequest.newBuilder()
    .model(ChatModel.QWEN_VL_MAX)
    .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
    .messages(List.of(
        Message.ofUser(List.of(
            Content.ofImage(new File("./document/image/image-002.jpeg").toURI()),
            Content.ofText("图片中一共多少辆自行车?")
        ))
    ))
    .build();
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
