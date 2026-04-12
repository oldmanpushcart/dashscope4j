# Dashscope4j Client

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j-client)

> 阿里云百炼平台（灵积）的**轻量级、响应式** Java SDK，以优雅的拦截器链设计和完整的类型安全著称。

## 🎯 为什么选择 Dashscope4j?

### vs 官方 SDK

| 维度 | Dashscope4j-Client | 官方 SDK |
|------|-------------------|----------|
| **API 风格** | 响应式/链式调用 | 命令式同步调用 |
| **代码量** | ~5,000 行（精简） | ~20,000 行（臃肿） |
| **依赖数量** | 轻量 (~30 JARs) | 重量 (~80 JARs) |
| **扩展性** | ⭐⭐⭐⭐⭐ 拦截器链机制 | ⭐⭐ 有限的回调 |
| **类型安全** | 编译时泛型检查 | 运行时类型转换 |
| **学习曲线** | 中等（需理解响应式） | 低（传统同步风格） |
| **OpenAI 兼容** | ✅ 内置适配层 | ❌ 需手动转换 |

### 三大核心优势

#### 1️⃣ **工业级拦截器链** 🏆
```java
// 自动处理工具调用循环，用户无感知
ToolCallInterceptor → 递归执行工具调用
UploadFilesInterceptor → 大文件自动上传
InlineFilesInterceptor → 小文件 Base64 内联
CompatOpenAiInterceptor → OpenAI 格式透明转换
```
**优势**：将 ToolCall 自动执行、文件上传/内联、OpenAI 兼容等复杂逻辑封装在拦截器中，业务代码保持简洁；同时支持在任意层级注入自定义拦截器，实现日志、监控、重试等横切关注点。

#### 2️⃣ **响应式编程模型**
- `async()` - CompletableFuture 异步调用
- `flow()` - Reactor Flux 流式输出（支持背压）
- `task()` - 长时间任务轮询
- `realtime()` - WebSocket 双向通信

**优势**：非阻塞 I/O，高并发场景吞吐量显著提升；流式输出首字延迟更低，用户体验更好。

#### 3️⃣ **极致的类型安全**
```java
// 编译时检查，杜绝运行时 ClassCastException
AigcRequest<ChatModel.Input, ChatModel.Output> request = ...
ChatModel.Output output = client.async(request).join();
String text = output.best().message().text(); // ✅ 无需类型转换
```

### 👥 适用人群

✅ **推荐使用**：
- 中大型 Java 应用，需要深度定制
- 高并发对话服务（客服机器人、智能助手）
- 微服务架构中的 AI 网关层
- 团队具备响应式编程经验

❌ **不推荐**：
- 简单脚本/原型开发（过度设计）
- 团队无 CompletableFuture/Reactor 经验
- 对依赖体积极度敏感的项目

## ⚡ 5 分钟快速开始

### ① 添加依赖

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-client</artifactId>
    <version>3.2.0</version>
</dependency>
```

### ② 创建客户端

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")  // 从环境变量读取: System.getenv("DASHSCOPE_API_KEY")
    .build();
```

### ③ 发起对话

```java
// 最简示例：3 行代码完成对话
var request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("你好！"))
        .build())
    .build();

String reply = client.async(request)
    .toCompletableFuture().join()
    .output().best().message().text();

System.out.println(reply); // 输出: 你好！有什么可以帮助你的吗？
```

### ④ 流式输出

```java
// 实时打字机效果
client.flow(request).subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    System.out.print(delta); // 逐字输出
});
```

### ⑤ 完成

Client 是**无状态**的，无需手动销毁，可直接复用或等待 GC 回收。

## ✨ 核心亮点深度解析

### 1. 强大的拦截器机制 🏆

拦截器链是 Dashscope4j 的**灵魂设计**，允许你在请求生命周期中注入自定义逻辑。

#### 拦截器执行顺序

```
用户调用 async/flow/task
    ↓
[1] 调用级拦截器    ← 通过 client.async(request, interceptors) 传入
    ↓
[2] 请求级拦截器    ← 通过 request.interceptors(...) 配置
    ↓
[3] 模型级拦截器    ← ChatModel 内置（ToolCall/FileUpload/Compat...）
    ↓
[4] 客户端级拦截器  ← 通过 client.newBuilder().interceptors(...) 配置
    ↓
[5] 系统级拦截器    ← Bridge/IncrementalOutputOnly/GeneralAigc
    ↓
发送 HTTP 请求
```

#### 实战：日志拦截器

```java
Interceptor loggingInterceptor = new Interceptor() {
    @Override
    public CompletionStage<?> intercept(Chain chain) {
        long start = System.currentTimeMillis();
        
        return chain.proceed()
            .whenComplete((response, error) -> {
                long cost = System.currentTimeMillis() - start;
                if (error != null) {
                    log.error("请求失败 [{}ms]: {}", cost, error.getMessage());
                } else {
                    log.info("请求成功 [{}ms]: {}", cost, response);
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

**拦截器的核心价值**：
- 🔒 **隔离复杂逻辑**：ToolCall 循环、文件上传、格式兼容等内部细节对用户透明
- 🔌 **灵活扩展**：可在调用级、请求级、模型级、客户端级任意位置注入自定义逻辑
- 🎯 **职责分离**：每个拦截器专注单一功能，易于测试和维护

---

### 2. 响应式编程模型

四种调用模式覆盖所有场景：

#### Async - 异步一次性响应

```java
CompletionStage<AigcResponse<ChatModel.Output>> future = client.async(request);

future.thenAccept(response -> {
    String text = response.output().best().message().text();
    System.out.println(text);
});
```

**适用场景**：常规对话、向量化、文件上传

#### Flow - 流式增量输出

```java
Publisher<AigcResponse<ChatModel.Output>> publisher = client.flow(request);

// Reactor 风格
Flux.from(publisher)
    .map(resp -> resp.output().best().message().text())
    .doOnNext(System.out::print)  // 逐字打印
    .blockLast();

// 或直接订阅
publisher.subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    System.out.print(delta);
});
```

**适用场景**：长文本生成、实时翻译、打字机效果

#### Task - 长时间任务轮询

```java
// 提交任务（如批量向量化、图片生成）
CompletionStage<? extends Task.Half<Output>> half = client.task(request);

// 等待任务完成（支持超时控制）
Output result = half.thenCompose(h -> 
    h.waitingFor(Task.WaitStrategies.until(
        Duration.ofSeconds(5),   // 每 5 秒轮询一次
        Duration.ofMinutes(10)   // 最多等待 10 分钟
    ))
).toCompletableFuture().join();
```

**适用场景**：WAN 图片生成、批量数据处理

#### Realtime - WebSocket 双向通信

```java
FunAsrSession session = FunAsrSession.newBuilder()
    .model(FunAsrModel.FUN_ASR_REALTIME)
    .build();

client.realtime(session, new Realtime.Handler<>() {
    @Override
    public void onOpen(Realtime.Emitter<FunAsrModel.In> emitter) {
        // 连接建立后发送音频数据
        emitter.binary(audioBuffer).close();
    }

    @Override
    public void onData(FunAsrModel.Out output) {
        // 接收识别结果
        System.out.println(output.output().sentence().text());
    }

    @Override
    public void onClosed(Throwable ex) {
        if (ex != null) ex.printStackTrace();
    }
});
```

**适用场景**：实时语音识别、语音对话、音视频流处理

---

### 3. 类型安全的设计

#### 泛型贯穿全程

```java
// ✅ 编译时检查输入输出类型
AigcRequest<ChatModel.Input, ChatModel.Output> request = ...

// ✅ 无需类型转换，IDE 自动补全
ChatModel.Output output = client.async(request).join();
String text = output.best().message().text();

// ❌ 官方 SDK 需要强制转换
Object result = officialClient.call(...);
ChatOutput output = (ChatOutput) result; // 运行时可能 ClassCastException
```

#### Record 不可变对象

```java
// 线程安全的 DTO
public record ChatModel(String name, String path, Set<String> tags) {}

// Builder 提供流畅 API
ChatModel.Input input = ChatModel.Input.newBuilder()
    .addMessage(Message.user("你好"))
    .uploadEnabled(true)
    .build(); // 返回不可变对象
```

**优势**：编译时类型检查杜绝运行时 ClassCastException；不可变对象天然线程安全，适合高并发场景。

## 🔧 高级特性

### OpenTelemetry 分布式追踪

启用后，所有 API 调用自动生成 Span，支持链路追踪。

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")
    .traceable(true)  // 开启追踪
    .build();
```

**添加依赖**：

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

**查看追踪数据**：
- Jaeger UI: http://localhost:16686
- Zipkin UI: http://localhost:9411
- Grafana Tempo / Prometheus

---

### 自定义 OkHttpClient

深度定制 HTTP 客户端行为：

```java
OkHttpClient http = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))  // 连接池调优
    .addInterceptor(chain -> {  // OkHttp 层拦截器
        Request request = chain.request()
            .newBuilder()
            .addHeader("X-Custom-Header", "value")
            .build();
        return chain.proceed(request);
    })
    .build();

DashscopeClient client = DashscopeClient.newBuilder()
    .ak("your-api-key")
    .http(http)
    .build();
```

**调优建议**：
- 高并发场景：增大 `connectionPool` 大小
- 大文件上传：增加 `writeTimeout`
- 长文本生成：增加 `readTimeout`

---

### 自定义主机地址

支持私有化部署或代理转发：

```java
DashscopeClient client = DashscopeClient.newBuilder()
    .host("custom-host.example.com")  // 自定义域名
    .ak("your-api-key")
    .build();
```

**应用场景**：
- 企业内部代理网关
- 多区域部署（新加坡/美西节点）
- 测试环境 Mock 服务

---

### 性能调优建议

#### 1. 复用 DashscopeClient 实例

```java
// ✅ 推荐：单例模式
private static final DashscopeClient CLIENT = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .build();

// ❌ 避免：频繁创建销毁
for (int i = 0; i < 1000; i++) {
    DashscopeClient client = DashscopeClient.newBuilder().ak("...").build();
    // ...
}
```

#### 2. 合理选择调用模式

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 短文本对话 | `async()` | 低延迟，简单直观 |
| 长文本生成 | `flow()` | 首字更快，用户体验好 |
| 图片生成 | `task()` | 异步轮询，不阻塞线程 |
| 语音识别 | `realtime()` | 双向流式，实时交互 |

#### 3. 批量操作优化

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

#### 4. 内存管理

```java
// 流式处理大响应时，及时释放累积数据
client.flow(request).subscribe(chunk -> {
    String delta = chunk.output().best().message().text();
    process(delta);  // 立即处理
    // 不要累积所有 chunk，避免 OOM
});
```

## 🏗️ 架构设计

### 分层架构图

```
┌─────────────────────────────────────────┐
│         用户层 (User Layer)              │
│   ChatModel / EmbeddingModel / Audio    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      API 抽象层 (API Abstraction)        │
│  AigcRequest / ApiRequest / Interceptor │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│    核心接口层 (Core Interface)           │
│       DashscopeClient                   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│     内部实现层 (Internal Implementation)  │
│  AsyncApi / FlowApi / TaskApi / Realtime│
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│   基础设施层 (Infrastructure Layer)      │
│  OkHttp3 / Jackson / Reactor / OTEL    │
└─────────────────────────────────────────┘
```

### 设计模式应用

| 模式 | 应用场景 | 优势 |
|------|---------|------|
| **拦截器链** | 请求生命周期管理 | 职责分离，灵活扩展 |
| **Builder** | 复杂对象构建 | 流畅 API，不可变对象 |
| **策略模式** | Task 等待策略 | 算法可替换 |
| **累加器** | 流式数据合并 | 优雅的增量聚合 |
| **工厂模式** | Model 实例创建 | 统一初始化逻辑 |

### 技术栈

- **JDK 17+** - Record、Pattern Matching、Sealed Classes
- **OkHttp3** - HTTP/WebSocket 客户端
- **Jackson** - JSON 序列化/反序列化
- **Reactor Core** - 响应式流（Flow API）
- **OpenTelemetry** - 分布式追踪（可选）
- **SLF4J** - 日志门面

## 📊 性能与兼容性

### 支持的模型列表

#### 对话模型 (Chat)
- `QWEN_FLASH` / `QWEN_PLUS` / `QWEN_MAX` - 通义千问系列
- `QWEN_VL_PLUS` / `QWEN_VL_MAX` - 视觉理解
- `QWQ_PLUS` / `QVQ_MAX` - 推理增强
- `QWEN3_OMNI_FLASH` - 多模态实时对话
- `QWEN_IMAGE_MAX` - 图像生成
- `WAN_T2I` - 文生图（异步任务）

#### 向量模型 (Embedding)
- `TEXT_EMBEDDING_V4` - 文本向量化
- `QWEN3_VL_EMBEDDING` - 多模态向量化

#### 音频模型 (Audio)
- `FUN_ASR_REALTIME` - 实时语音识别
- `QWEN_TTS` - 语音合成（通过 Agent 模块）

#### 通用模型 (GeneralAigcModel)

对于未在 SDK 中预定义的通义千问新模型，可使用 `GeneralAigcModel` 快速整合：

```java
// 快速接入任意通义千问模型
AigcModel<Input, Output> customModel = new GeneralAigcModel(
    "qwen-turbo",                    // 模型名称
    "/api/v1/services/aigc/text-generation/generation"  // API 路径
);

var request = AigcRequest.newBuilder(customModel)
    .input(...)
    .build();
```

**优势**：
- ✅ 无需等待 SDK 更新，立即使用新模型
- ✅ 自动继承拦截器链、类型安全等核心能力
- ✅ 支持自定义 tags 和 parameters

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
