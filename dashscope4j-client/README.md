# Dashscope4j Client

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j-client)

> 阿里云百炼平台（灵积）的 Java SDK，以拦截器链设计和类型安全为特点。

## 📋 概述

Dashscope4j Client 是一个面向阿里云灵积平台的 Java 客户端库，提供对话、向量化、音频处理等 AI 能力访问。框架采用响应式编程模型和拦截器链机制，支持异步、流式、任务和实时四种调用模式。

### 核心特性

- **拦截器链机制** - 在请求生命周期中注入自定义逻辑，实现关注点分离
- **响应式编程模型** - 基于 CompletionStage 和 Reactor 的非阻塞 I/O
- **类型安全设计** - 泛型贯穿全程，编译时检查杜绝运行时错误
- **多协议兼容** - 内置 OpenAI 格式转换、响应模式桥接等兼容层
- **基础操作接口** - 文件管理、存储上传、Token 计算等辅助功能
- **分布式追踪** - 可选集成 OpenTelemetry，支持链路追踪

### 适用场景

- ✅ Java 应用集成灵积平台 AI 能力
- ✅ 需要深度定制请求处理流程的场景
- ✅ 高并发对话服务或批量数据处理
- ❌ 简单脚本或原型开发（可能过度设计）
- ❌ 团队缺乏响应式编程经验

---

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-client</artifactId>
    <version>3.2.0</version>
</dependency>
```

### 2. 创建客户端

```java
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

DashscopeClient client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .build();
```

### 3. 发起对话

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

// 构建请求
var request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("你好！"))
        .build())
    .build();

// 异步调用
String reply = client.async(request)
    .toCompletableFuture().join()
    .output().best().message().text();

System.out.println(reply);
```

### 4. 流式输出

```java
import reactor.core.publisher.Flux;

// 流式调用
client.flow(request).subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    System.out.print(delta); // 逐字输出
});

// 或使用 Reactor
Flux.from(client.flow(request))
    .map(resp -> resp.output().best().message().text())
    .doOnNext(System.out::print)
    .blockLast();
```

---

## 🏗️ 架构设计

### 核心组件

```
┌──────────────────────────────────────┐
│       DashscopeClient Interface      │  ← 统一入口
├──────────────────────────────────────┤
│  async() / flow() / task() / realtime() │  ← 四种调用模式
├──────────────────────────────────────┤
│        Interceptor Chain             │  ← 拦截器链
│  User → Request → Model → Client → System │
├──────────────────────────────────────┤
│         BaseOp Interface             │  ← 基础操作
│  store() / files() / tokenizer()     │
├──────────────────────────────────────┤
│    HTTP/WebSocket Infrastructure     │  ← OkHttp + Reactor
└──────────────────────────────────────┘
```

### 拦截器执行顺序

拦截器链是框架的核心设计，允许在请求生命周期的不同阶段注入逻辑：

```
用户调用 async/flow/task
    ↓
[1] 调用级拦截器    ← client.async(request, interceptors) 传入
    ↓
[2] 请求级拦截器    ← request.interceptors(...) 配置
    ↓
[3] 模型级拦截器    ← ChatModel/AudioModel 内置拦截器
    ↓
[4] 客户端级拦截器  ← client.newBuilder().interceptors(...) 配置
    ↓
[5] 系统级拦截器    ← Bridge/IncrementalOutputOnly/GeneralAigc
    ↓
发送 HTTP/WebSocket 请求
```

**拦截器合并原则：** 越靠近用户的拦截器越先执行。

---

## 💡 核心功能

### 1. 四种调用模式

#### Async - 异步一次性响应

适用于常规对话、向量化等场景：

```java
CompletionStage<AigcResponse<ChatModel.Output>> future = client.async(request);

future.thenAccept(response -> {
    String text = response.output().best().message().text();
    System.out.println(text);
});

// 或同步等待
String text = client.async(request)
    .toCompletableFuture().join()
    .output().best().message().text();
```

#### Flow - 流式增量输出

适用于长文本生成、实时翻译等场景：

```java
Publisher<AigcResponse<ChatModel.Output>> publisher = client.flow(request);

// Reactor 风格
Flux.from(publisher)
    .map(resp -> resp.output().best().message().text())
    .doOnNext(System.out::print)
    .blockLast();

// 或直接订阅
publisher.subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    System.out.print(delta);
});
```

#### Task - 长时间任务轮询

适用于图片生成、批量数据处理等异步任务：

```java
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import java.time.Duration;

// 提交任务
CompletionStage<? extends Task.Half<Output>> half = client.task(request);

// 等待完成（支持超时控制）
Output result = half.thenCompose(h -> 
    h.waitingFor(Task.WaitStrategies.until(
        Duration.ofSeconds(5),   // 每 5 秒轮询一次
        Duration.ofMinutes(10)   // 最多等待 10 分钟
    ))
).toCompletableFuture().join();
```

#### Realtime - WebSocket 双向通信

适用于实时语音识别、语音对话等场景：

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

// 创建会话
var session = QwenAsrRealtimeSession.newBuilder()
    .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
    .sampleRate(16000)
    .build();

// 建立连接
client.realtime(session, new Realtime.Handler<>() {
    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        // 连接建立后发送音频数据
        emitter.binary(audioBuffer).close();
    }

    @Override
    public void onData(ServerEvent output) {
        // 接收识别结果
        System.out.println(output.transcription().text());
    }

    @Override
    public void onClosed(Throwable ex) {
        if (ex != null) ex.printStackTrace();
    }
});
```

---

### 2. 拦截器机制

拦截器允许在请求处理过程中注入自定义逻辑，是实现扩展性的核心机制。

#### 内置拦截器

| 拦截器 | 功能 | 说明 |
|--------|------|------|
| `BridgeInterceptor` | 响应模式桥接 | 自动在 async/flow/task 之间转换 |
| `IncrementalOutputOnlyInterceptor` | 增量输出强制 | 确保流式输出启用增量模式 |
| `GeneralAigcInterceptor` | 通用模型处理 | 处理文件上传和内联编码 |
| `CompatOpenAiInterceptor` | OpenAI 兼容 | 透明转换 OpenAI 格式请求 |
| `UploadMmContentInterceptor` | 多模态上传 | 自动上传本地文件到云端存储 |

#### 自定义拦截器示例

**日志拦截器：**

```java
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

Interceptor loggingInterceptor = new Interceptor() {
    private static final Logger log = LoggerFactory.getLogger("api-logger");
    
    @Override
    public CompletionStage<?> intercept(Chain chain) {
        long start = System.currentTimeMillis();
        
        return chain.proceed()
            .whenComplete((response, error) -> {
                long cost = System.currentTimeMillis() - start;
                if (error != null) {
                    log.error("请求失败 [{}ms]: {}", cost, error.getMessage());
                } else {
                    log.info("请求成功 [{}ms]", cost);
                }
            });
    }
};

// 应用到客户端
DashscopeClient client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .interceptors(List.of(loggingInterceptor))
    .build();
```

**聊天专用拦截器：**

对于聊天模型，可以使用 `ChatInterceptor` 简化开发：

```java
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

ChatInterceptor messageInterceptor = new ChatInterceptor() {
    @Override
    public CompletionStage<?> intercept(Chain chain, 
                                       AigcRequest<Input, Output> request) {
        // 仅处理聊天模型请求
        // 可以在这里修改消息、添加工具等
        
        return chain.proceed(request);
    }
};
```

#### 拦截器应用场景

- 🔒 **敏感信息脱敏** - 过滤手机号、身份证等隐私数据
- 📊 **监控指标收集** - 记录响应时间、Token 消耗
- 🔄 **请求重试逻辑** - 网络异常时自动重试
- 💾 **对话审计日志** - 合规要求的完整审计追踪
- 🔧 **工具调用循环** - Agent 框架中的 ReAct 推理

---

### 3. 基础操作接口

通过 `client.base()` 访问辅助功能：

#### 文件存储上传

自动将本地文件上传到灵积临时存储空间：

```java
import java.nio.file.Path;
import java.net.URI;

// 上传文件（返回 oss:// URI）
URI ossUri = client.base().store()
    .upload(Path.of("./image.png").toUri(), ChatModel.QWEN_VL_PLUS)
    .toCompletableFuture().join();

System.out.println(ossUri); // oss://bucket/path/to/file.png

// 在多模态请求中使用
var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user()
            .addText("描述这张图片")
            .addImage(ossUri)  // 使用上传后的 URI
            .build())
        .build())
    .build();
```

**自动上传机制：**

部分拦截器（如 `UploadMmContentInterceptor`）会自动检测本地文件并上传：

```java
// 启用上传功能
var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_PLUS)
    .input(ChatModel.Input.newBuilder()
        .uploadEnabled(true)  // 启用自动上传
        .addMessage(Message.user()
            .addImage(Path.of("./local-image.png").toUri())  // 本地文件
            .build())
        .build())
    .build();

// 拦截器会自动上传文件并替换为 oss:// URI
```

#### 文件管理

管理已上传的文件元数据：

```java
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import reactor.core.publisher.Flux;

// 创建文件记录
FileMeta meta = client.base().files()
    .create(Path.of("./document.pdf").toUri(), "document.pdf", Purpose.FILE_SEARCH)
    .toCompletableFuture().join();

System.out.println(meta.identity()); // 文件 ID
System.out.println(meta.toURI());    // fileid://xxx

// 查询文件详情
FileMeta detail = client.base().files()
    .detail(meta.identity())
    .toCompletableFuture().join();

// 删除文件
boolean deleted = client.base().files()
    .delete(meta.identity())
    .toCompletableFuture().join();

// 流式列出所有文件
Flux.from(client.base().files().flow(10))  // 每批 10 个
    .doOnNext(file -> System.out.println(file.name()))
    .blockLast();
```

#### Token 计算

估算文本的 Token 数量：

```java
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;

// 远程 Tokenizer（调用 API）
Tokenizer remote = client.base().tokenizer().remote(ChatModel.QWEN_PLUS);
int tokens = remote.countTokens("你好，世界！");

// 本地 Tokenizer（无需网络）
Tokenizer local = client.base().tokenizer().local(ChatModel.QWEN_PLUS);
int tokens = local.countTokens("你好，世界！");
```

---

### 4. 支持的模型

#### 对话模型 (Chat)

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;

ChatModel.QWEN_FLASH      // 通义千问 Flash（快速）
ChatModel.QWEN_PLUS       // 通义千问 Plus（均衡）
ChatModel.QWEN_MAX        // 通义千问 Max（强大）
ChatModel.QWEN_VL_PLUS    // 视觉理解 Plus
ChatModel.QWEN_VL_MAX     // 视觉理解 Max
ChatModel.QWQ_PLUS        // 推理增强 Plus
ChatModel.QVQ_MAX         // 视觉推理 Max
```

**多模态输入示例：**

```java
var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user()
            .addText("这张图片是什么内容？")
            .addImage(URI.create("https://example.com/image.jpg"))
            .addImage(Path.of("./local-image.png").toUri())
            .build())
        .build())
    .build();
```

#### 向量模型 (Embedding)

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.EmbeddingModel;

EmbeddingModel.TEXT_EMBEDDING_V4      // 文本向量化
EmbeddingModel.QWEN3_VL_EMBEDDING     // 多模态向量化
```

**文本向量化：**

```java
var request = AigcRequest.newBuilder(EmbeddingModel.TEXT_EMBEDDING_V4)
    .input(EmbeddingModel.Input.newBuilder()
        .addText("这是一段测试文本")
        .build())
    .build();

float[] vector = client.async(request)
    .toCompletableFuture().join()
    .output().embeddings().get(0).embedding();
```

**多模态向量化：**

```java
var request = AigcRequest.newBuilder(EmbeddingModel.QWEN3_VL_EMBEDDING)
    .input(MmEmbeddingModel.Input.newBuilder()
        .addContent(MmContent.Image.newBuilder()
            .uri(URI.create("https://example.com/image.jpg"))
            .build())
        .build())
    .build();
```

#### 音频模型 (Audio)

**实时语音识别 (ASR)：**

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.*;

var session = QwenAsrRealtimeSession.newBuilder()
    .model(QwenAsrRealtimeModel.QWEN3_ASR_FLASH_REALTIME)
    .sampleRate(16000)
    .inputAudioFormat(InputAudioFormat.PCM_16BIT)
    .turnDetection(TurnDetection.ServerVad.newBuilder().build())
    .build();

client.realtime(session, handler);
```

**语音合成 (TTS)：**

```java
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.*;

var session = CosyVoiceSession.newBuilder()
    .model(CosyVoiceModel.COSYVOICE_V3_PLUS)
    .voice("longxiaochun")
    .build();

client.realtime(session, handler);
```

#### 通用模型 (GeneralAigcModel)

对于未在 SDK 中预定义的新模型，可使用通用模型快速接入：

```java
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;

var customModel = new GeneralAigcModel(
    "qwen-turbo",                                    // 模型名称
    "/api/v1/services/aigc/text-generation/generation"  // API 路径
);

var request = AigcRequest.newBuilder(customModel)
    .input(Map.of("messages", List.of(...)))
    .build();
```

**优势：**
- ✅ 无需等待 SDK 更新，立即使用新模型
- ✅ 自动继承拦截器链、类型安全等核心能力
- ✅ 支持自定义参数和标签

---

## 🔧 高级配置

### 自定义 OkHttpClient

深度定制 HTTP 客户端行为：

```java
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

OkHttpClient http = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
    .addInterceptor(chain -> {
        Request request = chain.request()
            .newBuilder()
            .addHeader("X-Custom-Header", "value")
            .build();
        return chain.proceed(request);
    })
    .build();

DashscopeClient client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .http(http)
    .build();
```

**调优建议：**
- 高并发场景：增大 `connectionPool` 大小
- 大文件上传：增加 `writeTimeout`
- 长文本生成：增加 `readTimeout`

### 自定义主机地址

支持私有化部署或代理转发：

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .host("custom-host.example.com")
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .build();
```

**应用场景：**
- 企业内部代理网关
- 多区域部署（新加坡/美西节点）
- 测试环境 Mock 服务

### OpenTelemetry 分布式追踪

启用后，所有 API 调用自动生成 Span，支持链路追踪：

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .traceable(true)  // 开启追踪
    .build();
```

**添加依赖：**

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

**查看追踪数据：**
- Jaeger UI: http://localhost:16686
- Zipkin UI: http://localhost:9411
- Grafana Tempo / Prometheus

### 自定义 Executor

指定异步任务的执行线程池：

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService executor = Executors.newFixedThreadPool(10);

DashscopeClient client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .executor(executor)
    .build();

// 注意：需要在应用关闭时手动 shutdown
// executor.shutdown();
```

**默认行为：** 未设置时使用 `ForkJoinPool.commonPool()`

---

## 📊 性能优化

### 1. 复用客户端实例

```java
// ✅ 推荐：单例模式
private static final DashscopeClient CLIENT = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .build();

// ❌ 避免：频繁创建销毁
for (int i = 0; i < 1000; i++) {
    DashscopeClient client = DashscopeClient.newBuilder()
        .ak("...")
        .build();
}
```

### 2. 合理选择调用模式

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 短文本对话 | `async()` | 低延迟，简单直观 |
| 长文本生成 | `flow()` | 首字更快，用户体验好 |
| 图片生成 | `task()` | 异步轮询，不阻塞线程 |
| 语音识别 | `realtime()` | 双向流式，实时交互 |

### 3. 批量操作并行化

```java
// ❌ 串行执行（慢）
for (String text : texts) {
    var request = buildRequest(text);
    client.async(request).join();  // 阻塞等待
}

// ✅ 并行执行（快 5-10 倍）
List<CompletionStage<Output>> futures = texts.stream()
    .map(text -> buildRequest(text))
    .map(request -> client.async(request))
    .toList();

List<Output> results = CompletableFuture.allOf(
        futures.toArray(CompletableFuture[]::new)
    )
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)
        .toList()
    )
    .toCompletableFuture().join();
```

### 4. 流式处理内存管理

```java
// ✅ 及时释放累积数据
client.flow(request).subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    process(delta);  // 立即处理
    // 不要累积所有 chunk，避免 OOM
});
```

---

## 🛠️ 开发指南

### 环境要求

- JDK 17+
- Maven 3.6+
- 阿里云 DashScope API Key

### 本地构建

```bash
git clone https://github.com/oldmanpushcart/dashscope4j.git
cd dashscope4j
mvn clean install
```

### 运行测试

```bash
cd dashscope4j-client
export DASHSCOPE_API_KEY=your-api-key
mvn test
```

### 贡献指南

我们欢迎所有形式的贡献：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

**开发规范：**
- 遵循 Google Java Style Guide
- 所有公共 API 必须有 JavaDoc
- 新增功能需包含单元测试
- 提交前运行 `mvn clean verify`

---

## 📄 许可证

本项目采用 Apache License 2.0 许可证。

---

## 🙏 致谢

- [阿里云百炼平台](https://dashscope.aliyun.com/) - 提供大模型 API 服务
- [OkHttp](https://square.github.io/okhttp/) - HTTP/WebSocket 客户端
- [Reactor](https://projectreactor.io/) - 响应式编程支持
- [Jackson](https://github.com/FasterXML/jackson) - JSON/XML 处理
- [OpenTelemetry](https://opentelemetry.io/) - 分布式追踪

---

## 📮 联系方式

- 📧 Email: oldmanpushcart@gmail.com
- 🐛 Issue: [GitHub Issues](https://github.com/oldmanpushcart/dashscope4j/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/oldmanpushcart/dashscope4j/discussions)

---

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**
