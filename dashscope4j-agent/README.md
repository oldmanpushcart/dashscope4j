# Dashscope4j Agent

> 🤖 为 Java 开发者打造的智能体框架 | 支持 ReAct 推理 · MCP 协议 · Skill 技能系统

[![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j-agent)](https://central.sonatype.com/artifact/io.github.oldmanpushcart/dashscope4j-agent)
[![JDK](https://img.shields.io/badge/JDK-17+-blue)](https://openjdk.org/)
[![License](https://img.shields.io/github/license/oldmanpushcart/dashscope4j)](LICENSE)

---

## ✨ 为什么选择 Dashscope4j Agent？

### 🎯 核心价值

- **开箱即用的 ReAct 智能体** - 内置思维链推理引擎，自动规划工具调用序列
- **企业级工具生态** - 支持 MCP 协议、Skill 技能包、自定义工具加载器
- **生产级记忆管理** - 智能压缩、LRU 缓存、多存储后端（文件/内存）
- **完全异步非阻塞** - 基于 CompletionStage + Reactor，高并发场景性能卓越

### 🆚 与官方 SDK 的差异

| 特性 | 官方灵积 SDK | Dashscope4j Agent |
|------|------------|------------------|
| Agent 框架 | ❌ 不提供 | ✅ 完整 ReAct 实现 |
| 工具管理 | ⚠️ 基础 Function Call | ✅ 动态发现 + MCP + Skills |
| 记忆系统 | ❌ 无 | ✅ 智能压缩 + 多后端 |
| 协议支持 | 仅灵积 API | ✅ MCP + Anthropic Skills |
| 扩展性 | 低 | 🔧 插件化架构 |

### 👥 适用人群

- ✅ **Java 后端开发者** - 想在现有系统中集成 AI Agent 能力
- ✅ **企业应用架构师** - 需要可维护、可扩展的 Agent 框架
- ✅ **AI 工程化团队** - 追求标准化协议（MCP/Skills）和生态兼容
- ❌ **Python 数据科学家** - 建议使用 LangChain/LlamaIndex
- ❌ **快速原型验证** - 建议使用 Streamlit + OpenAI SDK

---

## 🚀 5 分钟快速开始

### 1️⃣ 添加依赖

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-agent</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 2️⃣ 创建第一个 Agent

```java
// 初始化 DashScope 客户端
var client = DashscopeClient.newBuilder()
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .build();

// 创建工具箱（管理可用工具）
var toolbox = HashMapToolbox.newBuilder()
    .indexer(HashMapToolIndexer.newBuilder()
        .client(client)
        .model(ChatModel.QWEN_FLASH)
        .build())
    .loaders(List.of(
        FileOpsToolLoader.INSTANCE,      // 文件操作工具
        SystemToolLoader.INSTANCE         // 系统命令工具
    ))
    .build();

// 创建 ReAct Agent
var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .toolbox(toolbox)
    .introduction("你是一个智能助手")
    .build();

// 对话
var response = agent.async("session-001", Message.user("今天天气如何？"))
    .toCompletableFuture()
    .join();

System.out.println(response.text());
```

### 3️⃣ 运行效果

```
用户: 查询杭州的天气并生成一张天气图

Agent Thought: 我需要先获取天气数据...
Agent Action: search_tools
Agent Action Input: {"intent": "天气预报"}
Observation: ["get_weather"]

Agent Action: get_weather
Agent Action Input: {"city": "杭州"}
Observation: "多云，15°C"

Agent Thought: 现在需要生成图片...
Agent Action: search_tools
Agent Action Input: {"intent": "图像生成"}
Observation: ["generate_image"]

Agent Action: generate_image
Agent Action Input: {"prompt": "杭州多云天气，15°C"}
Observation: "图片URL: https://..."

Final Answer: 已为您生成杭州天气图（多云，15°C），查看链接：https://...
```

---

## 💡 核心亮点解析

### 🧠 1. ReAct 推理引擎

**什么是 ReAct？**

ReAct = Reasoning（推理）+ Acting（行动）的循环模式。Agent 自主思考 → 选择工具 → 执行 → 观察结果 → 继续思考，直到得出最终答案。

**工作流程：**

```
┌──────────┐     ┌─────────┐     ┌────────────┐
│ Thought  │────▶│ Action  │────▶│ Observation│
│ (思考)   │     │ (行动)  │     │ (观察结果)  │
└──────────┘     └─────────┘     └────────────┘
       ▲                                  │
       └──────────────────────────────────┘
              循环直到得出 Final Answer
```

**我们的优势：**

- ✅ **动态工具发现** - 每步自动搜索最合适的工具，无需预先配置
- ✅ **流式输出优化** - 实时检测 "Final Answer" 标记，提前返回结果
- ✅ **错误恢复机制** - 工具失败时自动尝试替代方案，永不轻言放弃

**示例代码：**

```java
// ReAct Agent 自动处理复杂任务
agent.async("session-001", Message.user("""
    帮我分析本季度的销售数据，
    找出增长最快的产品，
    并生成一份可视化报告
"""));

// Agent 会自动：
// 1. Thought: 需要先读取销售数据文件
// 2. Action: read_file("sales_q3.xlsx")
// 3. Observation: [数据内容]
// 4. Thought: 需要计算增长率
// 5. Action: execute_script("analyze.py")
// 6. Observation: [分析结果]
// 7. Thought: 需要生成图表
// 8. Action: generate_chart(...)
// 9. Final Answer: [完整报告]
```

---

### 🔧 2. 强大的工具生态系统

#### **三种工具来源**

```
┌─────────────────────────────────────────┐
│          Toolbox (工具箱)                │
├──────────┬──────────┬───────────────────┤
│  Skill   │   MCP    │  Custom Loaders   │
│  技能包  │  协议工具 │  自定义加载器      │
└──────────┴──────────┴───────────────────┘
```

#### **① Skill 技能包**（符合 Anthropic 规范）

Skill 是可复用的能力模块，包含指令、资源和脚本：

```
skills/
├── weekly-report-writer/    # 周报生成器
│   ├── SKILL.md             # 元数据 + 指令
│   ├── assets/              # 模板文件
│   │   └── weekly-report-template.md
│   └── scripts/             # 自动化脚本
│       └── gather_data.sh
└── school-score/            # 学生成绩分析
    ├── SKILL.md
    ├── assets/
    │   ├── scores.xlsx
    │   └── students.txt
    └── scripts/
        └── grep_student.sh
```

**SKILL.md 示例：**

```markdown
---
name: weekly-report-writer
description: 生成本周工作汇报，自动收集数据并格式化
license: MIT
---

# 周报生成器

## 使用方法

调用 `skill$weekly-report-writer` 工具，传入本周工作内容。

## 资源

- 模板文件: `assets/weekly-report-template.md`
- 数据脚本: `scripts/gather_data.sh`
```

**使用 Skill：**

```java
// 加载 Skill 技能包
var skillLoader = SkillToolLoader.newBuilder()
    .providers(List.of(
        FileSkillProvider.newBuilder()
            .scanDir(Path.of("./skills"))
            .syncInterval(Duration.ofSeconds(10))  // 10秒热更新
            .build()
    ))
    .build();

var toolbox = HashMapToolbox.newBuilder()
    .indexer(...)
    .loaders(List.of(skillLoader))
    .build();

// Agent 自动发现并使用 skill$weekly-report-writer
agent.async("session-001", Message.user("帮我生成本周工作汇报"));
```

#### **② MCP 协议工具**（Model Context Protocol）

MCP 是标准化的工具协议，支持连接外部服务：

```java
// 连接高德地图 MCP 服务器
var mcpLoader = McpToolLoader.newBuilder()
    .name("amap")
    .transport(RecoverableMcpClientTransport.newBuilder()
        .transportFactory(mapper ->
            HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                .endpoint("/mcp?key=YOUR_API_KEY")
                .build())
        .build())
    .syncInterval(Duration.ofHours(1))  // 每小时同步工具变更
    .build();

// 所有 MCP 工具自动注册，名称前缀为 "mcp$amap$"
// 例如: mcp$amap$get_location, mcp$amap$get_route
```

**支持的 MCP 能力：**

- 🔧 **Tools** - 可调用的函数工具
- 📝 **Prompts** - 预定义的提示词模板
- 📦 **Resources** - 可访问的资源文件

### ③ 自定义工具加载器

实现自己的 ToolLoader：

```java
public class MyToolLoader implements ToolLoader {
    
    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {
        var myTool = FunctionTool.newBuilder()
            .name("my_custom_tool")
            .description("我的自定义工具")
            .parameterType(MyParams.class)
            .function(params -> {
                // 工具逻辑
                return CompletableFuture.completedStage("result");
            })
            .build();
        
        return toolbox.register("my_custom_tool", myTool);
    }
    
    @Override
    public void close() {
        // 清理资源
    }
}

// 使用
var toolbox = HashMapToolbox.newBuilder()
    .loaders(List.of(new MyToolLoader()))
    .build();
```

#### GUI 自动化工具 (GuiToolkit)

GuiToolkit 提供了屏幕截图和桌面自动化能力，让 Agent 能够操作计算机界面：

```java
// 创建 GUI 工具包
var guiToolkit = GuiToolkit.newBuilder()
    .enableScreenshot(true)    // 启用截图功能
    .enableMouse(true)         // 启用鼠标操作
    .enableKeyboard(true)      // 启用键盘操作
    .enableClipboard(true)     // 启用剪贴板操作
    .build();

// 或者使用选择性功能
var readOnlyGuiToolkit = GuiToolkit.newBuilder()
    .enableScreenshot(true)    // 只启用截图
    .enableMouse(false)        // 禁用鼠标操作
    .enableKeyboard(false)     // 禁用键盘操作
    .enableClipboard(false)    // 禁用剪贴板
    .build();

// 集成到工具箱
var toolbox = HashMapToolbox.newBuilder()
    .indexer(HashMapToolIndexer.newBuilder()
        .client(client)
        .model(ChatModel.QWEN_FLASH)
        .build())
    .loaders(List.of(
        ToolkitLoader.of(guiToolkit),           // GUI 自动化工具
        ToolkitLoader.of(SystemToolkit.create()),  // 系统工具
        ToolkitLoader.of(FileOpsToolkit.newBuilder()
            .workspace(Path.of("./"))
            .build())
    ))
    .build();
```

**可用的 GUI 工具：**

| 工具名称 | 功能 | 使用场景 |
|---------|------|----------|
| `gui$screenshot` | 屏幕截图 | 获取屏幕内容、UI 分析、视觉识别 |
| `gui$mouse$move` | 鼠标移动 | 将鼠标移动到指定位置 |
| `gui$mouse$click` | 鼠标点击 | 左键/右键/双击等点击操作 |
| `gui$mouse$drag` | 鼠标拖拽 | 从一个位置拖拽到另一个位置 |
| `gui$mouse$scroll` | 鼠标滚轮 | 上下滚动页面 |
| `gui$key$press` | 按键操作 | 按下指定的单个按键 |
| `gui$key$type` | 文本输入 | 输入文本字符串 |
| `gui$key$combo` | 组合键 | 执行 Ctrl+C、Alt+Tab 等组合键 |
| `gui$clipboard$get` | 获取剪贴板 | 读取系统剪贴板内容 |
| `gui$clipboard$set` | 设置剪贴板 | 设置系统剪贴板内容 |

**典型应用场景：**

1. **桌面自动化** - 操作应用程序、填写表单、执行重复性任务
2. **UI 测试辅助** - 截图分析界面、自动化点击测试
3. **远程协助** - 帮助用户执行桌面操作
4. **数据录入** - 自动填写表格、输入数据
5. **界面监控** - 定期截图监控应用程序状态

**注意事项：**

- 需要运行在有图形界面的环境中（不能在纯服务器环境）
- 需要用户授权才能访问屏幕和执行桌面操作
- 某些安全软件可能会阻止自动化操作
- 大尺寸截图会消耗较多 token，建议只截取必要区域

---

### 🧠 3. 智能记忆管理

**痛点解决：**

- ❌ **传统方案**：手动拼接历史消息 → Token 超限 → 上下文丢失
- ✅ **我们的方案**：自动压缩 + LRU 淘汰 + 摘要生成

**工作原理：**

```
会话历史超出 maxTokens 时触发压缩：

原始对话 (5000 tokens):
  User: 询问天气... (2000 tokens)
  Assistant: 回答... (3000 tokens)

↓ 自动压缩

压缩后 (800 tokens):
  Summary: "用户询问了杭州天气，得知多云15°C" (300 tokens)
  Recent: [最近2轮对话] (500 tokens)
```

**使用示例：**

```java
var memory = WorkingMemory.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_FLASH)  // 用于生成摘要的模型
    .store(FileMemoryStore.newBuilder()
        .directory(Paths.get("./memory"))
        .build())
    .maxTokens(25_000)      // 最大 25K tokens
    .gcRatio(0.3)           // 压缩后保留 30% 内容
    .maxSessions(100)       // 最多缓存 100 个会话（LRU）
    .build();

var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .memory(memory)  // 注入记忆系统
    .build();

// 长对话自动管理上下文
agent.async("session-001", Message.user("第一轮对话..."));
agent.async("session-001", Message.user("第二轮对话..."));
// ... 第 N 轮对话，自动压缩旧历史
```

**支持的存储后端：**

| 存储器 | 适用场景 | 持久化 |
|--------|---------|--------|
| `HashMapMemoryStore` | 开发测试 | ❌ 内存 |
| `FileMemoryStore` | 单机应用 | ✅ 文件系统 |
| 🔜 `RedisMemoryStore` | 分布式部署 | ✅ Redis |

**记忆拦截器自动工作：**

```java
// MemoryInterceptor 自动在请求生命周期中：
// 1. recall: 召回历史记忆并注入到消息列表
// 2. proceed: 执行原始请求
// 3. remember: 将响应保存到记忆中

// 支持三种模式：ASYNC / FLOW / TASK
```

---

### 🔌 4. 拦截器机制

在请求生命周期中注入自定义逻辑：

```java
// 日志拦截器
class LoggingInterceptor implements ChatInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest request) {
        log.info("📤 Request: {}", request.input().userInputMessage().text());
        
        return chain.proceed(request)
            .thenApply(response -> {
                log.info("📥 Response: {}", 
                    ((AigcResponse<?>) response).output().best().message().text());
                return response;
            });
    }
}

// 使用
var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .interceptors(list -> list.add(new LoggingInterceptor()))
    .build();
```

**典型应用场景：**

- 📊 **监控指标收集** - 记录响应时间、Token 消耗
- 🔒 **敏感信息脱敏** - 过滤手机号、身份证等隐私数据
- 🔄 **请求重试逻辑** - 网络异常时自动重试
- 💾 **对话审计日志** - 合规要求的完整审计追踪

---

## 🏗️ 架构概览

### 分层设计

```
┌─────────────────────────────────────────┐
│         Agent Interface                 │  ← 统一接口
├─────────────────────────────────────────┤
│    ReActAgent / BaseAgent               │  ← 核心实现
├─────────────────────────────────────────┤
│  Memory  │  Toolbox  │  Interceptors   │  ← 核心组件
├─────────────────────────────────────────┤
│  ToolLoaders (Skill/MCP/Custom)        │  ← 工具来源
├─────────────────────────────────────────┤
│  MemoryStore (File/HashMap/Redis)      │  ← 存储后端
└─────────────────────────────────────────┘
```

### 关键设计模式

| 模式 | 应用场景 | 优势 |
|------|---------|------|
| **Builder** | 所有组件构建 | 链式调用，参数清晰 |
| **Strategy** | ToolLoader/MemoryStore | 灵活替换实现 |
| **Chain of Responsibility** | MemoryInterceptor | 解耦请求处理流程 |
| **Observer** | SkillProvider.Updater | 动态通知工具变更 |

### 核心类图

```
Agent (interface)
  ├── name(): String
  ├── description(): String
  ├── introduction(): String
  ├── async(sessionId, message): CompletionStage<AssistantMessage>
  └── flow(sessionId, message): Publisher<AssistantMessage>

BaseAgent (abstract)
  └── ReActAgent
      ├── newReActRequest()      # 重构请求
      ├── processAsync()         # 异步处理
      ├── processFlow()          # 流式处理
      └── unpackingResponse()    # 解包结果

Toolbox (interface)
  ├── lookup(intent): Map<String, Tool>
  ├── lookupByName(name): Tool
  ├── register(name, tool): Void
  └── remove(name): Void

Memory (interface)
  ├── recall(sessionId, instant): List<Message>
  └── remember(sessionId, messages): Void
```

---

## 📚 进阶指南

### 🎓 教程系列

1. **[基础篇]** 创建你的第一个 Agent
2. **[工具篇]** 编写自定义 ToolLoader
3. **[技能篇]** 开发 Skill 技能包
4. **[记忆篇]** 配置持久化存储
5. **[实战篇]** 构建客服机器人

### 🔧 常见场景

#### 场景 1：多工具协同工作流

```java
// 自动规划：查询天气 → 生成图片 → 发送邮件
var response = agent.async("session-001", Message.user("""
    查询北京明天的天气，生成一张天气卡片，
    并通过邮件发送给我
""")).toCompletableFuture().join();

// Agent 执行步骤：
// 1. search_tools(intent="天气预报") → get_weather
// 2. get_weather(city="北京") → "晴，20°C"
// 3. search_tools(intent="图像生成") → generate_image
// 4. generate_image(prompt="北京晴天天气卡片") → image_url
// 5. search_tools(intent="邮件发送") → send_email
// 6. send_email(to="user@example.com", attachment=image_url)
// 7. Final Answer: "已发送到您的邮箱"
```

#### 场景 2：Skill 技能调用

```java
// 加载多个 Skill
var skillLoader = SkillToolLoader.newBuilder()
    .providers(List.of(
        FileSkillProvider.newBuilder()
            .scanDir(Path.of("./skills"))
            .build()
    ))
    .build();

// Agent 自动发现所有 Skill 工具
// - skill$weekly-report-writer
// - skill$school-score
// - skill$data-analyzer

agent.async("session-001", Message.user("帮我生成本周工作汇报"));
// Agent 自动调用 skill$weekly-report-writer
```

#### 场景 3：流式响应

```java
// 实时输出 Agent 思考过程
Flux.from(agent.flow("session-001", Message.user("讲个故事")))
    .doOnNext(msg -> {
        if (msg.reasoningContent() != null) {
            System.out.print("🤔 " + msg.reasoningContent());  // 思考过程
        }
        if (msg.text() != null) {
            System.out.print(msg.text());  // 最终答案
        }
    })
    .blockLast();
```

#### 场景 4：会话管理

```java
// 不同会话独立记忆
agent.async("user-A-session-001", Message.user("我叫张三"));
agent.async("user-B-session-001", Message.user("我叫李四"));

// 各自记住不同的信息
agent.async("user-A-session-001", Message.user("我叫什么？"));
// → "您叫张三"

agent.async("user-B-session-001", Message.user("我叫什么？"));
// → "您叫李四"
```

---

## 🔍 API 参考

### Agent 接口

```java
public interface Agent {
    /**
     * @return Agent 名称
     */
    String name();
    
    /**
     * @return Agent 描述
     */
    String description();
    
    /**
     * @return Agent 介绍（系统提示词）
     */
    String introduction();
    
    /**
     * 异步处理用户消息
     *
     * @param sessionId 会话ID
     * @param inbound   用户消息
     * @return 处理结果
     */
    CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound);
    
    /**
     * 流式处理用户消息
     *
     * @param sessionId 会话ID
     * @param inbound   用户消息
     * @return 处理结果流
     */
    Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound);
}
```

### 核心组件

| 组件 | 接口 | 实现类 | 说明 |
|------|------|--------|------|
| **Agent** | `Agent` | `ReActAgent`, `BaseAgent` | 智能体核心 |
| **工具箱** | `Toolbox` | `HashMapToolbox` | 工具管理器 |
| **记忆** | `Memory` | `WorkingMemory` | 记忆系统 |
| **工具加载器** | `ToolLoader` | `SkillToolLoader`, `McpToolLoader`, `SystemToolLoader` | 工具来源 |
| **记忆存储** | `MemoryStore` | `FileMemoryStore`, `HashMapMemoryStore` | 存储后端 |
| **工具索引** | `ToolIndexer` | `HashMapToolIndexer` | 意图检索 |

### ReActAgent 构建器

```java
ReActAgent.newBuilder()
    .client(DashscopeClient)              // [必需] DashScope 客户端
    .model(ChatModel)                     // [必需] 对话模型
    .toolbox(Toolbox)                     // [必需] 工具箱
    .memory(Memory)                       // [可选] 记忆系统（默认内存存储）
    .introduction(String)                 // [可选] 系统提示词
    .parameters(Map<String, Object>)      // [可选] 模型参数
    .interceptors(List<Interceptor>)      // [可选] 拦截器列表
    .build();
```

---

## 🆚 与其他框架对比

| 特性 | Dashscope4j Agent | LangChain4j | Spring AI |
|------|------------------|-------------|-----------|
| **语言** | Java | Java | Java |
| **Agent 模式** | ReAct | 多种（ReAct/Plan-and-Execute） | 基础 |
| **MCP 支持** | ✅ | ❌ | ❌ |
| **Skill 规范** | ✅ Anthropic | ❌ | ❌ |
| **记忆压缩** | ✅ 智能摘要 | ⚠️ 手动配置 | ❌ |
| **异步模型** | CompletionStage + Reactor | CompletableFuture | Mono |
| **学习曲线** | 中等 | 陡峭 | 平缓 |
| **生态兼容** | 灵积 + MCP | 多提供商（OpenAI/Azure等） | Spring 生态 |
| **适用场景** | 企业级 Java 应用 | 多模型实验 | Spring 项目集成 |

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

### 运行示例

```bash
cd dashscope4j-agent
export DASHSCOPE_API_KEY=your-api-key
mvn test -Dtest=DebugTestCase#debug$1
```

### 调试技巧

**启用详细日志：**

```xml
<!-- src/test/resources/logback-test.xml -->
<configuration>
    <logger name="io.github.oldmanpushcart.dashscope4j.agent" level="DEBUG"/>
</configuration>
```

**查看 ReAct 思考过程：**

```
DEBUG - ReActAgent/function/search_tools >>> {"intent":"天气预报"}
DEBUG - ReActAgent/function/search_tools <<< {"get_weather": {...}}
DEBUG - ReActAgent/function/get_weather >>> {"city":"杭州"}
DEBUG - ReActAgent/function/get_weather <<< "多云，15°C"
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 开发规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 所有公共 API 必须有 JavaDoc
- 新增功能需包含单元测试
- 提交前运行 `mvn clean verify`

### 贡献方向

- 🐛 **Bug 修复** - 提交 Issue 或 PR
- 💡 **功能建议** - 提出新特性想法
- 📖 **文档改进** - 补充示例、修正错误
- 🔧 **性能优化** - 提升运行效率
- 🌍 **国际化** - 翻译文档

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 🙏 致谢

- [阿里云灵积平台](https://dashscope.aliyun.com/) - 提供强大的大模型 API
- [Model Context Protocol](https://modelcontextprotocol.io/) - 标准化的工具协议
- [Anthropic Skills](https://agentskills.io/) - 技能包规范参考
- [Reactor](https://projectreactor.io/) - 响应式编程支持
- [Jackson](https://github.com/FasterXML/jackson) - JSON 处理

---

## 📮 联系方式

- 📧 Email: oldmanpushcart@gmail.com
- 🐛 Issue: [GitHub Issues](https://github.com/oldmanpushcart/dashscope4j/issues)
- 📖 文档: [Wiki](https://github.com/oldmanpushcart/dashscope4j/wiki)
- 💬 讨论: [GitHub Discussions](https://github.com/oldmanpushcart/dashscope4j/discussions)

---

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**

[![Star History Chart](https://api.star-history.com/svg?repos=oldmanpushcart/dashscope4j&type=Date)](https://star-history.com/#oldmanpushcart/dashscope4j&Date)
