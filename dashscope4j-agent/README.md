# Dashscope4j Agent

> 🤖 为 Java 开发者打造的智能体框架 | 基于插件化架构 · 支持 ReAct 推理 · MCP/Skills 协议

[![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j-agent)](https://central.sonatype.com/artifact/io.github.oldmanpushcart/dashscope4j-agent)
[![JDK](https://img.shields.io/badge/JDK-17+-blue)](https://openjdk.org/)
[![License](https://img.shields.io/github/license/oldmanpushcart/dashscope4j)](LICENSE)

---

## 📋 概述

Dashscope4j Agent 是一个基于阿里云灵积平台的 Java 智能体框架，采用插件化设计，支持 ReAct 推理模式和多种工具协议（MCP、Skills）。框架通过拦截器机制实现关注点分离，提供灵活的扩展能力。

### 核心特性

- **插件化架构** - 基于 `Plugin` 接口的设计，支持功能模块化组合
- **ReAct 推理引擎** - 内置思维链与行动交替的推理循环机制
- **动态工具管理** - 支持意图检索的工具发现系统，按需加载工具
- **多协议支持** - 兼容 Model Context Protocol (MCP) 和 Anthropic Skills 规范
- **会话记忆管理** - 基于拦截器的会话历史自动记录与压缩
- **完全异步设计** - 基于 CompletionStage + Reactor 的非阻塞架构

### 适用场景

- ✅ Java 后端系统集成 AI Agent 能力
- ✅ 企业级应用中需要可扩展的智能体框架
- ✅ 需要标准化协议（MCP/Skills）支持的场景
- ❌ Python 生态的数据科学工作流（建议使用 LangChain）
- ❌ 快速原型验证（建议使用更轻量级的方案）

---

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-agent</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 2. 创建基础 Agent

使用 `SimpleToolboxPlugin` 快速配置工具箱：

```java
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

// 初始化客户端
var client = DashscopeClient.newBuilder()
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .build();

        // 创建 ReAct Agent
        var agent = ReActAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_PLUS)
                .plugins(plugins -> {
                    // 添加工具箱插件
                    plugins.add(SimpleToolboxPlugin.newBuilder()
                            .toolkit(ToolUse.Mode.FIXED, ShellToolkit.create())
                            .build());
                    return plugins;
                })
                .build();

        // 执行对话
        var response = agent.async("session-001", Message.user("查询当前目录下的文件"))
                .toCompletableFuture()
                .join();

System.out.

        println(response.text());
```

### 3. 运行效果

```
用户: 查询当前目录下的文件

Agent Thought: 我需要列出当前目录的文件...
Agent Action: shell$execute
Agent Action Input: {"command": "ls -la"}
Observation: "total 48\ndrwxr-xr-x ..."

Final Answer: 当前目录下包含以下文件：README.md、pom.xml、src/ 目录等。
```

---

## 🏗️ 架构设计

### 核心组件

```
┌──────────────────────────────────────┐
│         Agent Interface              │  ← 统一接口定义
├──────────────────────────────────────┤
│      BaseAgent (抽象基类)             │  ← 核心实现
├──────────────────────────────────────┤
│   ReActAgent / DashscopeAgent        │  ← 典型实现
├──────────────────────────────────────┤
│          Plugin System               │  ← 插件系统
│  ┌──────────┬──────────┬──────────┐  │
│  │ Toolbox  │ Session  │ Custom   │  │
│  │ 工具箱   │ 会话管理 │ 自定义   │  │
│  └──────────┴──────────┴──────────┘  │
├──────────────────────────────────────┤
│       Interceptor Chain              │  ← 拦截器链
│  PREPARATION → INTERACTION           │  │
└──────────────────────────────────────┘
```

### 关键概念

#### 1. Agent 接口

所有智能体的统一抽象，提供异步和流式两种交互方式：

```java
public interface Agent extends AutoCloseable {
    String name();
    String description();
    DashscopeClient client();
    
    // 异步调用
    CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound);
    
    // 流式调用
    Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound);
}
```

#### 2. Plugin 插件系统

插件是功能扩展的核心机制，在 Agent 生命周期中安装和卸载：

```java
public interface Plugin {
    // 安装阶段：返回扩展配置
    CompletionStage<Extension> install(Agent agent);
    
    // 卸载阶段：清理资源
    CompletionStage<Void> uninstall();
    
    interface Extension {
        // 获取指定阶段的拦截器
        List<ChatInterceptor> interceptors(Phases phases);
    }
    
    enum Phases {
        PREPARATION,  // 预处理阶段（请求构建）
        INTERACTION   // 交互阶段（请求执行）
    }
}
```

**内置插件：**

| 插件 | 说明 | 用途 |
|------|------|------|
| `SimpleToolboxPlugin` | 简易工具箱插件 | 快速配置工具集合 |
| `ToolboxPlugin` | 高级工具箱插件 | 精细控制工具箱行为 |
| `SessionPlugin` | 会话管理插件 | 历史记录与记忆压缩 |

#### 3. Toolbox 工具箱

管理工具的注册、索引和检索，支持动态加载：

```java
public interface Toolbox extends AutoCloseable, ToolLookup {
    // 订阅工具加载器
    CompletionStage<ToolSubscription> subscribe(ToolLoader loader);
    
    // 按意图检索工具
    CompletionStage<List<Tool>> lookupByIntent(String intent);
    
    // 按名称精确查找
    Optional<Tool> lookupByName(String name);
}
```

**工具使用模式：**

- **FIXED** - 固定模式：工具始终对 LLM 可见，适合常用工具
- **DYNAMIC** - 动态模式：工具按需加载，适合大量工具或插件式场景

#### 4. ToolLoader 工具加载器

从不同来源加载工具的标准接口：

```java
public interface ToolLoader extends AutoCloseable {
    CompletionStage<Void> subscribe(ToolSubscription subscription, 
                                    ToolSubscriptionHandler handler);
    void unsubscribe(ToolSubscription subscription);
    List<ToolUse> loaded();
}
```

**内置加载器：**

| 加载器 | 协议/来源 | 说明 |
|--------|----------|------|
| `SkillLoader` | Anthropic Skills | 从文件系统加载技能包 |
| `McpLoader` | Model Context Protocol | 连接 MCP 服务器获取工具 |
| `ToolkitLoader` | 本地 Toolkit | 从代码定义的 Toolkit 加载 |

#### 5. Session 会话管理

基于拦截器的会话历史管理机制：

```java
public interface Session {
    // 召回历史消息
    CompletionStage<List<Message>> recall(UserMessage instant);
    
    // 记录新消息
    CompletionStage<Void> remember(List<Message> messages);
}
```

**工作流程：**

1. **PREPARATION 阶段** - `SettingInterceptor` 召回历史并注入请求
2. **INTERACTION 阶段** - `RecordInterceptor` 记录对话并触发压缩

---

## 💡 使用指南

### 工具箱配置

#### 方式一：使用 SimpleToolboxPlugin（推荐）

最简化的配置方式，自动创建内部 Toolbox：

```java
var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .plugins(plugins -> {
        plugins.add(SimpleToolboxPlugin.newBuilder()
            // 添加工具包（FIXED 模式）
            .toolkit(ToolUse.Mode.FIXED, ShellToolkit.create())
            .toolkit(ToolUse.Mode.FIXED, FileOpsToolkit.create())    
            
            // 添加 Skill 技能包（DYNAMIC 模式）
            .skill(ToolUse.Mode.DYNAMIC, Path.of("./skills"))
            
            // 启用搜索工具（默认 true）
            .enableSearchTools(true)
            
            // 同步间隔（默认 5 秒）
            .syncInterval(Duration.ofSeconds(10))
            
            .build());
        return plugins;
    })
    .build();
```

#### 方式二：使用 ToolboxPlugin（高级）

手动创建和管理 Toolbox，适合复杂场景：

```java
// 1. 创建工具箱
var toolbox = HashMapToolbox.newBuilder()
    .indexer(HashMapToolIndexer.newBuilder()
        .client(client)
        .model(ChatModel.QWEN_FLASH)
        .cacheFile(Path.of(".toolbox-index-cache.jsonl"))
        .build())
    .syncInterval(Duration.ofSeconds(5))
    .shared(false)  // 非共享模式，插件关闭时联动关闭
    .build();

// 2. 创建加载器
var skillLoader = SkillLoader.newBuilder()
    .directories(List.of(Path.of("./skills")))
    .build();

var mcpLoader = McpLoader.newBuilder()
    .name("amap")
    .mode(ToolUse.Mode.DYNAMIC)
    .transport(RecoverableMcpClientTransport.newBuilder()
        .transportFactory(mapper ->
            HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                .endpoint("/mcp?key=YOUR_API_KEY")
                .jsonMapper(mapper)
                .build())
        .build())
    .build();

var toolkitLoader = new ToolkitLoader()
    .append(ToolUse.Mode.FIXED, 
        RuntimeToolkit.create(),
        ShellToolkit.create())
    .append(ToolUse.Mode.DYNAMIC, 
        DashscopeToolkit.create());

// 3. 订阅加载器
CompletableFutureUtils.allOf(List.of(
        toolbox.subscribe(toolkitLoader),
        toolbox.subscribe(skillLoader),
        toolbox.subscribe(mcpLoader)
    ))
    .toCompletableFuture()
    .join();

// 4. 创建插件
var toolboxPlugin = ToolboxPlugin.newBuilder()
    .toolbox(toolbox)
    .enableSearchTools(true)
    .build();

// 5. 组装 Agent
var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .plugins(List.of(toolboxPlugin))
    .build();
```

### 会话记忆配置

添加 `SessionPlugin` 实现持久化会话管理：

```java
var sessionPlugin = SessionPlugin.newBuilder()
    .store(FileFragmentStore.newBuilder()
        .directory(Path.of(".session"))
        .build())
    .model(ChatModel.QWEN_FLASH)  // 用于生成摘要的模型
    .maxTokens(5000)              // 最大 token 数
    .gcRatio(0.3)                 // 压缩后保留 30%
    .build();

var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .plugins(plugins -> {
        plugins.add(sessionPlugin);
        plugins.add(toolboxPlugin);
        return plugins;
    })
    .build();

// 长对话自动管理上下文
agent.async("user-001", Message.user("第一轮对话"));
agent.async("user-001", Message.user("第二轮对话"));
// ... 超出 maxTokens 时自动触发压缩
```

### 自定义 Toolkit

创建自己的工具集合：

```java
public class WeatherToolkit implements Toolkit {
    
    @Override
    public List<Tool> tools() {
        var getWeather = FunctionTool.newBuilder()
            .name("get_weather")
            .description("查询指定城市的天气信息")
            .parameterType(WeatherParams.class)
            .function(params -> {
                // 调用天气 API
                String result = callWeatherApi(params.city());
                return CompletableFuture.completedStage(result);
            })
            .build();
        
        return List.of(getWeather);
    }
    
    public record WeatherParams(
        @JsonPropertyDescription("城市名称")
        @JsonProperty(value = "city", required = true)
        String city
    ) {}
}

// 使用
var agent = ReActAgent.newBuilder()
    .client(client)
    .plugins(plugins -> {
        plugins.add(SimpleToolboxPlugin.newBuilder()
            .toolkit(ToolUse.Mode.FIXED, new WeatherToolkit())
            .build());
        return plugins;
    })
    .build();
```

### 流式响应

实时接收 Agent 的思考过程和最终答案：

```java
Flux.from(agent.flow("session-001", Message.user("讲个故事")))
    .doOnNext(msg -> {
        if (msg.reasoningContent() != null) {
            System.out.print("🤔 " + msg.reasoningContent());  // 思考过程
        }
        if (msg.text() != null && !msg.text().isEmpty()) {
            System.out.print(msg.text());  // 最终答案
        }
    })
    .blockLast();
```

---

## 🔧 工具生态系统

### 1. Skill 技能包

符合 Anthropic Skills 规范的独立能力模块：

**目录结构：**

```
skills/
├── weekly-report-writer/
│   ├── SKILL.md              # 元数据和指令
│   ├── assets/               # 静态资源
│   │   └── template.md
│   └── scripts/              # 可执行脚本
│       └── gather_data.sh
└── school-score/
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
name: school-score
description: 查询学生成绩信息
license: MIT
---

# 学生成绩查询技能

## 可用工具

- `skill$school-score$get_student`: 查询学生基本信息
- `skill$school-score$get_score`: 查询学生成绩

## 使用方法

调用上述工具时传入学生姓名参数。
```

**加载使用：**

```java
plugins.add(SimpleToolboxPlugin.newBuilder()
    .skill(ToolUse.Mode.DYNAMIC, Path.of("./skills"))
    .build());

// Agent 自动发现并使用 skill$school-score 相关工具
agent.async("session-001", Message.user("小蓝的数学成绩是多少?"));
```

### 2. MCP 协议工具

通过 Model Context Protocol 连接外部服务：

```java
var mcpLoader = McpLoader.newBuilder()
    .name("amap")  // MCP 服务器名称
    .mode(ToolUse.Mode.DYNAMIC)
    .transport(RecoverableMcpClientTransport.newBuilder()
        .transportFactory(mapper ->
            HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                .endpoint("/mcp?key=API_KEY")
                .jsonMapper(mapper)
                .build())
        .build())
    .build();

// 所有 MCP 工具以 "mcp$amap$" 为前缀
// 例如: mcp$amap$get_location, mcp$amap$get_route
```

**支持的 MCP 能力：**

- 🔧 **Tools** - 可调用的函数工具
- 📝 **Prompts** - 预定义的提示词模板
- 📦 **Resources** - 可访问的资源文件

### 3. 内置 Toolkit

框架提供的常用工具集合：

| Toolkit | 功能 | 工具示例 |
|---------|------|----------|
| `ShellToolkit` | Shell 命令执行 | `shell$execute` |
| `FileOpsToolkit` | 文件操作 | `file$read`, `file$write` |
| `TextFileOpsToolkit` | 文本文件处理 | `text$search`, `text$replace` |
| `RuntimeToolkit` | 运行时信息查询 | `runtime$env`, `runtime$properties` |
| `GuiToolkit` | GUI 自动化 | `gui$screenshot`, `gui$mouse$click` |
| `HttpToolkit` | HTTP 请求 | `http$get`, `http$post` |
| `DashscopeToolkit` | 灵积平台功能 | `dashscope$tts`, `dashscope$image_gen` |

**GUI 自动化工具示例：**

```java
var guiToolkit = GuiToolkit.newBuilder()
    .enableScreenshot(true)    // 启用截图
    .enableMouse(true)         // 启用鼠标操作
    .enableKeyboard(true)      // 启用键盘输入
    .enableClipboard(true)     // 启用剪贴板
    .build();

plugins.add(SimpleToolboxPlugin.newBuilder()
    .toolkit(ToolUse.Mode.FIXED, guiToolkit)
    .build());

// Agent 可以执行：截图、鼠标点击、键盘输入等操作
```

⚠️ **注意：** GUI 工具需要在有图形界面的环境中运行，不能在纯服务器环境使用。

---

## 🧠 ReAct 推理机制

### 工作原理

ReAct（Reasoning + Acting）是一种交替进行推理和行动的推理模式：

```
Question: 用户问题
  ↓
Thought: 分析问题，决定下一步行动
  ↓
Action: 选择要使用的工具
  ↓
Action Input: 提供工具参数（JSON 格式）
  ↓
[系统执行工具，自动注入]
  ↓
Observation: 工具执行结果
  ↓
Thought: 基于结果继续思考
  ↓
... (循环直到得出结论)
  ↓
Final Answer: 最终答案
```

### 核心原则

ReAct Agent 遵循严格的执行规范（详见 [REACT_AGENT.md](src/main/resources/prompt/REACT_AGENT.md)）：

1. **严禁自我欺骗** - Observation 由系统自动注入，Agent 不能编造
2. **说到做到** - 输出 Action 后立即停止，等待系统返回结果
3. **基于事实** - 所有结论必须有 Observation 支撑
4. **自检流程** - 输出 Final Answer 前必须验证任务完整性

### search_tools 元工具

当现有工具无法满足需求时，使用 `search_tools` 动态发现工具：

```
Thought: 我需要读取 Excel 文件，但当前没有合适的工具
Action: search_tools
Action Input: {"intent": "解析 Excel 文件中的数据表格"}
Observation: ["excel$read_sheet", "excel$parse_table"]

Thought: 找到了 excel$read_sheet 工具，可以使用它
Action: excel$read_sheet
Action Input: {"file": "data.xlsx"}
```

**Intent 编写建议：**

- ✅ 使用完整句子："解析 Excel 文件中的销售数据表格"
- ❌ 避免模糊词汇："数据处理"
- ✅ 包含动作和对象："读取" + "Excel 文件"
- ✅ 多次尝试不同表述，至少尝试 3 种

---

## 🔍 调试与监控

### 启用日志

```xml
<!-- src/test/resources/logback-test.xml -->
<configuration>
    <logger name="io.github.oldmanpushcart.dashscope4j.agent" level="DEBUG"/>
</configuration>
```

### 查看执行过程

```
DEBUG - dashscope4j-agent:/react/function/search_tools >>> {"intent":"天气预报"}
DEBUG - dashscope4j-agent:/react/function/search_tools <<< {"get_weather": {...}}
DEBUG - dashscope4j-agent:/react/function/get_weather >>> {"city":"杭州"}
DEBUG - dashscope4j-agent:/react/function/get_weather <<< {"temperature": 20, "condition": "晴"}
```

### 运行测试用例

```bash
cd dashscope4j-agent
export DASHSCOPE_API_KEY=your-api-key
mvn test -Dtest=ReActAgentTestCase#test$skill
```

---

## 📚 API 参考

### Agent 构建器

```java
ReActAgent.newBuilder()
    .name(String)                    // [可选] Agent 名称
    .description(String)             // [可选] Agent 描述
    .client(DashscopeClient)         // [必需] 灵积客户端
    .model(ChatModel)                // [必需] 对话模型
    .plugins(List<Plugin>)           // [可选] 插件列表
    .plugins(UnaryOperator)          // [可选] 插件配置函数
    .build()                         // 同步构建
    .buildAsync()                    // 异步构建
```

### SimpleToolboxPlugin 配置

```java
SimpleToolboxPlugin.newBuilder()
    .toolkit(Mode, Toolkit...)       // 添加工具包
    .skill(Mode, Path)               // 添加 Skill 目录
    .mcp(Mode, McpLoader)            // 添加 MCP 加载器
    .enableSearchTools(boolean)      // 启用搜索工具（默认 true）
    .syncInterval(Duration)          // 同步间隔（默认 5 秒）
    .build()
```

### SessionPlugin 配置

```java
SessionPlugin.newBuilder()
    .store(FragmentStore)            // [必需] 存储后端
    .model(ChatModel)                // [可选] 摘要模型（默认 QWEN_FLASH）
    .maxTokens(int)                  // [可选] 最大 token 数（默认 100000）
    .gcRatio(double)                 // [可选] 压缩比例（默认 0.3）
    .build()
```

**支持的存储后端：**

| 存储器 | 类 | 持久化 |
|--------|-----|--------|
| 文件存储 | `FileFragmentStore` | ✅ 文件系统 |
| 内存存储 | `HashMapFragmentStore` | ❌ 仅内存 |

---

## 🆚 与其他框架对比

| 特性 | Dashscope4j Agent | LangChain4j | Spring AI |
|------|------------------|-------------|-----------|
| **语言** | Java | Java | Java |
| **架构模式** | 插件化 | 组件化 | Spring 集成 |
| **ReAct 支持** | ✅ 内置 | ✅ 需配置 | ⚠️ 基础 |
| **MCP 协议** | ✅ | ❌ | ❌ |
| **Skills 规范** | ✅ | ❌ | ❌ |
| **会话压缩** | ✅ 自动 | ⚠️ 手动 | ❌ |
| **异步模型** | CompletionStage + Reactor | CompletableFuture | Mono |
| **学习曲线** | 中等 | 陡峭 | 平缓 |
| **适用场景** | 企业级 Java 应用 | 多模型实验 | Spring 项目 |

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

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 🙏 致谢

- [阿里云灵积平台](https://dashscope.aliyun.com/) - 提供大模型 API 服务
- [Model Context Protocol](https://modelcontextprotocol.io/) - 标准化工具协议
- [Anthropic Skills](https://agentskills.io/) - 技能包规范参考
- [Reactor](https://projectreactor.io/) - 响应式编程支持
- [Jackson](https://github.com/FasterXML/jackson) - JSON 处理库

---

## 📮 联系方式

- 📧 Email: oldmanpushcart@gmail.com
- 🐛 Issue: [GitHub Issues](https://github.com/oldmanpushcart/dashscope4j/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/oldmanpushcart/dashscope4j/discussions)

---

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**
