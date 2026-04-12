# Dashscope4j Client

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j-client)

阿里云百炼平台（灵积）官方 Java SDK 的轻量级替代方案，提供响应式、链式 API 风格的客户端实现。

## ✨ 特性

### 核心能力
- **响应式编程**: 支持异步(async)、流式(flow)、任务(task)三种调用模式
- **多模态对话**: 文本、图像、音频的统一编码风格
- **实时通信**: WebSocket 实时双向数据流
- **拦截器机制**: 支持请求级别和全局级别的拦截器链
- **OpenTelemetry**: 可选的分布式追踪支持

### 支持的 API
- **对话(Chat)**: Qwen 系列模型的文本/多模态对话，支持 Function Call
- **向量(Embedding)**: 文本向量化，支持相似度计算
- **音频(Audio)**: 语音识别(ASR)、语音合成(TTS)、音频转录
- **基础服务(Base)**: Tokenizer、文件管理、临时存储空间

### 技术优势
- 基于 JDK 17+ 构建，模块化设计
- 轻量级依赖，仅依赖 OkHttp3、Jackson、Reactor
- 完整的类型安全和编译时检查
- 兼容 OpenAI API 格式

## 🚀 快速开始

### 前置要求
- JDK 17 或更高版本
- 阿里云百炼账号和 API Key

### Maven 依赖
```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-client</artifactId>
    <version>3.2.0</version>
</dependency>
```

### 基础示例

#### 1. 创建客户端
```java
DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")  // 替换为你的 API Key
    .build();
```

#### 2. 简单对话
```java
AigcRequest<ChatModel.Input, ChatModel.Output> request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("你好！"))
        .build())
    .build();

String text = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .best()
    .message()
    .text();
```

#### 3. 流式对话
```java
AigcRequest<ChatModel.Input, ChatModel.Output> request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("请写一首诗"))
        .build())
    .parameters(Map.of("incremental_output", true))
    .build();

client.flow(request).subscribe(chunk -> {
    String text = chunk.output().best().message().text();
});
```

#### 4. 销毁客户端
```java
client.shutdown();
```

## 📖 使用指南

### 对话 API (Chat)

#### 文本对话
```java
AigcRequest<ChatModel.Input, ChatModel.Output> request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("请介绍一下人工智能"))
        .build())
    .build();

String text = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .best()
    .message()
    .text();
```

#### 多模态对话（图文）
```java
AigcRequest<ChatModel.Input, ChatModel.Output> request = AigcRequest.newBuilder(ChatModel.QWEN_VL_MAX)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user(List.of(
            Content.text("这张图片是什么？"),
            Content.image(new File("image.jpg").toURI())
        )))
        .uploadEnabled(true)
        .build())
    .build();

String text = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .best()
    .message()
    .text();
```

#### Function Call
```java
FunctionTool weatherTool = FunctionTool.newBuilder()
    .name("get_weather")
    .description("获取指定城市的天气")
    .addParameter("city", "string", "城市名称", true)
    .executor((args) -> "{\"temperature\": 25, \"condition\": \"sunny\"}")
    .build();

AigcRequest<ChatModel.Input, ChatModel.Output> request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("杭州今天天气怎么样？"))
        .build())
    .parameters(Map.of("tools", List.of(weatherTool)))
    .build();

String text = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .best()
    .message()
    .text();
```

### 向量 API (Embedding)

#### 文本向量化
```java
AigcRequest<TextEmbeddingModel.Input, TextEmbeddingModel.Output> request = AigcRequest.newBuilder(TextEmbeddingModel.TEXT_EMBEDDING_V4)
    .input(TextEmbeddingModel.Input.newBuilder()
        .texts(List.of("人工智能是未来的发展方向", "机器学习是人工智能的重要分支"))
        .build())
    .build();

List<Float> vector = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .embeddings()
    .get(0)
    .embedding();
```

#### 多模态向量化（图文）
```java
AigcRequest<MmEmbeddingModel.Input, MmEmbeddingModel.Output> request = AigcRequest.newBuilder(MmEmbeddingModel.QWEN3_VL_EMBEDDING)
    .input(MmEmbeddingModel.Input.newBuilder()
        .contents(List.of(MmContent.Complex.newBuilder()
            .text("这是一张图片")
            .image(new File("image.jpg").toURI())
            .build()))
        .uploadEnabled(true)
        .build())
    .build();

List<Float> vector = client.async(request)
    .toCompletableFuture()
    .join()
    .output()
    .embeddings()
    .get(0)
    .embedding();
```

### 音频 API (Audio)

#### 语音识别 (ASR) - 实时模式
```java
FunAsrSession session = FunAsrSession.newBuilder()
    .model(FunAsrModel.FUN_ASR_REALTIME)
    .build();

StringBuilder transcript = new StringBuilder();
CompletableFuture<Void> completeF = new CompletableFuture<>();

client.realtime(session, new Realtime.Handler<>() {
    @Override
    public void onOpen(Realtime.Emitter<FunAsrModel.In> emitter) {
        emitter.binary(audioBuffers).close();
    }

    @Override
    public void onData(FunAsrModel.Out output) {
        if (output.output().sentence().end()) {
            transcript.append(output.output().sentence().text());
        }
    }

    @Override
    public void onBinary(ByteBuffer buffer) {}

    @Override
    public void onClosed(Throwable ex) {
        if (ex != null) {
            completeF.completeExceptionally(ex);
        } else {
            completeF.complete(null);
        }
    }
});

completeF.join();
String result = transcript.toString();
```

#### 语音合成 (TTS)

> TTS 功能需要通过 Agent 模块或自定义实现，Client 模块提供基础的实时通信能力。

### 实时 API (Realtime)

实时 API 支持 WebSocket 双向通信，适用于语音对话、实时交互等场景。

```java
OmniRealtimeSession session = OmniRealtimeSession.newBuilder()
    .model(OmniRealtimeModel.QWEN3_OMNI_FLASH_REALTIME)
    .build();

CompletableFuture<String> responseF = new CompletableFuture<>();

RealtimeConnector connector = RealtimeConnector.newBuilder()
    .retryStrategy((attempt, ex) -> Duration.ofSeconds(1))
    .connectionFactory(() -> client.realtime(session, new SimpleOmniRealtimeHandler() {
        @Override
        public void onResponseTextDelta(String responseId, String delta) {}

        @Override
        public void onResponseAudioDelta(String responseId, ByteBuffer delta) {}

        @Override
        public void onResponseCreated(String responseId) {}

        @Override
        public void onResponseFinished(String responseId, ServerEvent.Status status) {
            responseF.complete("完成");
        }

        @Override
        public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
            emitter.binary(audioBuffers).close();
        }

        @Override
        public void onClosed(Throwable ex) {
            if (ex != null) {
                responseF.completeExceptionally(ex);
            }
        }
    }))
    .build();

connector.connect();
responseF.join();
```

### 基础服务 (Base)

#### Tokenizer 计算
```java
List<Integer> tokens = client.base().tokenizer().local()
    .encode("你好世界")
    .toCompletableFuture()
    .join();

List<Integer> remoteTokens = client.base().tokenizer().remote(ChatModel.QWEN_PLUS)
    .encode("你好世界")
    .toCompletableFuture()
    .join();
```

#### 文件管理
```java
FilesOp filesOp = client.base().files();

FileMeta meta = filesOp.create(
    new File("document.pdf").toURI(),
    "document.pdf",
    Purpose.FILE_EXTRACT
).toCompletableFuture().join();

String fileId = meta.identity();

FileMeta detail = filesOp.detail(fileId)
    .toCompletableFuture()
    .join();

boolean deleted = filesOp.delete(fileId)
    .toCompletableFuture()
    .join();
```

### 高级特性

#### 拦截器

拦截器允许你在请求前后执行自定义逻辑，适用于日志记录、监控、重试等场景。

```java
// 创建日志拦截器
Interceptor loggingInterceptor = new Interceptor() {
    @Override
    public CompletionStage<?> intercept(Chain chain) {
        System.out.println("Request: " + chain.request());
        return chain.proceed()
            .whenComplete((response, error) -> {
                if (error != null) {
                    System.err.println("Error: " + error.getMessage());
                } else {
                    System.out.println("Response: " + response);
                }
            });
    }
};

// 应用到客户端
DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")
    .interceptors(List.of(loggingInterceptor))
    .build();
```

#### OpenTelemetry 追踪

启用 OpenTelemetry 后，所有 API 调用会自动生成追踪信息。

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")
    .traceable(true)
    .build();
```

需要添加 OpenTelemetry 依赖并配置 TracerProvider：

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
    <version>1.32.0</version>
</dependency>
```

#### 自定义 OkHttpClient

```java
OkHttpClient http = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(new CustomInterceptor())
    .build();

DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")
    .http(http)
    .build();
```

#### 自定义主机地址

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .host("custom-host.example.com")
    .ak("your-api-key")
    .build();
```

## 🔧 配置说明

### 客户端配置项

| 配置项 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| host | API 主机地址 | dashscope.aliyuncs.com | 否 |
| ak | API Key | - | 是 |
| http | OkHttpClient 实例 | 自动创建默认实例 | 否 |
| traceable | 启用 OpenTelemetry 追踪 | false | 否 |
| interceptors | 拦截器列表 | 空列表 | 否 |

### 环境变量

- `DASHSCOPE_API_KEY`: API Key（可选，优先级低于代码配置）

### 线程模型

- **Async**: 使用 CompletableFuture，在内部线程池执行
- **Flow**: 基于 Reactor Flux，支持背压
- **Task**: 异步任务轮询，适用于长时间运行的任务

## 🧪 测试

运行单元测试：

```bash
mvn test
```

测试用例位于 `src/test/java` 目录，包含：
- 单元测试：验证各个组件的功能
- 集成测试：与真实 API 交互（需要配置 API Key）
- 示例代码：展示各种使用场景

### 配置测试环境

在 `src/test/resources` 中创建 `application.properties`：

```properties
dashscope.api.key=your-test-api-key
```

## 📚 API 文档

- [JavaDoc](https://javadoc.io/doc/io.github.oldmanpushcart/dashscope4j-client)
- [Wiki](https://github.com/oldmanpushcart/dashscope4j/wiki)
- [示例代码](src/test/java/io/github/oldmanpushcart/dashscope4j/client)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 开发环境设置

1. Fork 本仓库
2. 克隆到本地：`git clone https://github.com/your-username/dashscope4j.git`
3. 导入到 IDE（推荐 IntelliJ IDEA）
4. 运行测试：`mvn test`
5. 提交 PR

### 代码规范

- 遵循 Java 代码规范
- 添加必要的注释和 JavaDoc
- 编写单元测试
- 保持代码简洁清晰

## 📄 许可证

Apache License 2.0

## 🔗 相关链接

- [阿里云百炼平台](https://dashscope.aliyun.com)
- [帮助文档](https://help.aliyun.com/zh/dashscope/)
- [GitHub 仓库](https://github.com/oldmanpushcart/dashscope4j)
- [Dashscope4j Agent](../dashscope4j-agent) - 智能体框架
- [Issue 反馈](https://github.com/oldmanpushcart/dashscope4j/issues)

## 📝 更新日志

详见 [Releases](https://github.com/oldmanpushcart/dashscope4j/releases)
