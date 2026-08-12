# Dashscope4j

![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)
![JDK17+](https://img.shields.io/badge/JDK-17+-blue.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.oldmanpushcart/dashscope4j)

> 阿里云百炼平台（灵积）的 Java SDK，提供客户端库和智能体框架两个模块。

## ✨ 核心特性

### dashscope4j-client

- **拦截器链机制** - 在请求生命周期中注入自定义逻辑，实现关注点分离
- **响应式编程模型** - 支持 async/flow/task/realtime 四种调用模式
- **类型安全设计** - 泛型贯穿全程，编译时检查杜绝运行时错误
- **多协议兼容** - 内置 OpenAI 格式转换、响应模式桥接等兼容层
- **基础操作接口** - 文件管理、存储上传、Token 计算等辅助功能

👉 [查看详细文档](dashscope4j-client/README.md)

### dashscope4j-agent

- **插件化架构** - 基于 Plugin 系统的模块化设计，灵活扩展
- **ReAct 推理引擎** - 思维链与行动交替的推理循环机制
- **动态工具管理** - 支持 MCP、Skills、Toolkit 多种工具来源
- **会话记忆管理** - 历史记录自动压缩与持久化存储
- **完全异步设计** - 基于 CompletionStage + Reactor 的非阻塞架构

👉 [查看详细文档](dashscope4j-agent/README.md)

---

## 📦 项目组成

Dashscope4j 包含两个独立但互补的模块：

| 模块 | 说明 | 适用场景 |
|------|------|----------|
| **[dashscope4j-client](dashscope4j-client/README.md)** | 灵积平台 Java 客户端库 | API 调用、响应式编程、类型安全访问 |
| **[dashscope4j-agent](dashscope4j-agent/README.md)** | 智能体框架 | ReAct 推理、工具管理、会话记忆 |

---

## 🚀 快速开始

### 添加依赖

**仅使用客户端：**

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-client</artifactId>
    <version>4.0.0</version>
</dependency>
```

**使用智能体框架（自动包含 client）：**

```xml
<dependency>
    <groupId>io.github.oldmanpushcart</groupId>
    <artifactId>dashscope4j-agent</artifactId>
    <version>4.0.0</version>
</dependency>
```

### 最简示例

#### Client - 发起对话

```java
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

var client = DashscopeClient.newBuilder()
    .ak(System.getenv("DASHSCOPE_API_KEY"))
    .build();

var request = AigcRequest.newBuilder(ChatModel.QWEN_PLUS)
    .input(ChatModel.Input.newBuilder()
        .addMessage(Message.user("你好！"))
        .build())
    .build();

String reply = client.async(request)
    .toCompletableFuture().join()
    .output().best().message().text();

System.out.println(reply);
```

#### Agent - 智能对话

```java
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.SimpleToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;

var agent = ReActAgent.newBuilder()
        .client(client)
        .model(ChatModel.QWEN_PLUS)
        .plugins(plugins -> {
            plugins.add(SimpleToolboxPlugin.newBuilder()
                    .toolkit(ToolUse.Mode.FIXED, ShellToolkit.create())
                    .build());
            return plugins;
        })
        .build();

var response = agent.async("session-001", Message.user("查询当前目录文件"))
        .toCompletableFuture().join();

System.out.

println(response.text());
```

---

## 📖 使用场景

### 选择 Client

当你需要：
- ✅ 直接调用灵积 API（对话、向量化、音频等）
- ✅ 精细控制请求处理流程
- ✅ 实现自定义的 Agent 逻辑
- ✅ 高并发批量处理任务

### 选择 Agent

当你需要：
- ✅ 开箱即用的 ReAct 智能体
- ✅ 自动化工具调用序列
- ✅ 会话记忆和历史管理
- ✅ 企业级应用集成

### 组合使用

典型的企业级应用架构：

```
┌─────────────────────────┐
│   业务应用层             │
├─────────────────────────┤
│  dashscope4j-agent      │  ← 智能体编排、工具管理
├─────────────────────────┤
│  dashscope4j-client     │  ← API 调用、拦截器链
├─────────────────────────┤
│   灵积平台 API           │
└─────────────────────────┘
```

---

## 🔗 相关链接

- **客户端文档**: [dashscope4j-client/README.md](dashscope4j-client/README.md)
- **智能体文档**: [dashscope4j-agent/README.md](dashscope4j-agent/README.md)
- **阿里云灵积**: https://dashscope.aliyun.com
- **帮助文档**: https://help.aliyun.com/zh/dashscope/
- **GitHub Issues**: https://github.com/oldmanpushcart/dashscope4j/issues

---

## 📄 许可证

本项目采用 Apache License 2.0 许可证。

---

## 🙏 致谢

感谢以下开源项目：

- [OkHttp](https://square.github.io/okhttp/) - HTTP/WebSocket 客户端
- [Reactor](https://projectreactor.io/) - 响应式编程支持
- [Jackson](https://github.com/FasterXML/jackson) - JSON/XML 处理
- [OpenTelemetry](https://opentelemetry.io/) - 分布式追踪
- [Model Context Protocol](https://modelcontextprotocol.io/) - 标准化工具协议

---

**⭐ 如果这个项目对你有帮助，请给我们一个 Star！**
