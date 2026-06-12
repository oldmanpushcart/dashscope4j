package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.SessionPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.store.FileFragmentStore;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.LlmToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.McpLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.RecoverableMcpClientTransport;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.TextFileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.network.HttpToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.GuiToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.plan.Plan;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class DebugTestCase implements LoadingEnv {

    private Plugin buildingSessionPlugin() {
        return SessionPlugin.newBuilder()
                .store(FileFragmentStore.newBuilder()
                        .directory(Path.of(".session"))
                        .build())
                .maxTokens(50 * 100)
                .gcRatio(0.3)
                .build();
    }

    private Plugin buildingToolboxPlugin() {

        final var skillLoader = SkillLoader.newBuilder()
                .directories(List.of(
                        Path.of("./skills")
                ))
                .build();

        final var mcpLoader = McpLoader.newBuilder()
                .name("amap")
                .mode(ToolUse.Mode.DYNAMIC)
                .transport(RecoverableMcpClientTransport.newBuilder()
                        .transportFactory(mapper ->
                                HttpClientStreamableHttpTransport.builder("https://mcp.amap.com")
                                        .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                        .jsonMapper(mapper)
                                        .build())
                        .build())
                .build();

        final var toolkitLoader = new ToolkitLoader()
                .append(ToolUse.Mode.DYNAMIC, List.of(
                        DashscopeToolkit.create(),
                        GuiToolkit.create(),
                        HttpToolkit.create()
                ))
                .append(ToolUse.Mode.FIXED, List.of(
                        RuntimeToolkit.create(),
                        ShellToolkit.create(),
                        FileOpsToolkit.create(),
                        TextFileOpsToolkit.create()
                ));

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(LlmToolIndexer.newBuilder()
                        .client(client)
                        .model(ChatModel.QWEN_FLASH)
                        .cacheFile(Path.of(".toolbox-index-cache.jsonl"))
                        .build())
                .build();

        CompletableFutureUtils.allOf(List.of(
                        toolbox.subscribe(toolkitLoader),
                        toolbox.subscribe(skillLoader),
                        toolbox.subscribe(mcpLoader)
                ))
                .toCompletableFuture()
                .join();

        return ToolboxPlugin.newBuilder()
                .toolbox(toolbox)
                .enableSearchTools(true)
                .build();
    }

    @Disabled
    @Test
    public void debug$1() {

        final var sessionId =
                //"SESSION-snake"
                UUID.randomUUID().toString()
                //"SESSION-001"
                ;
        final var sessionPlugin = buildingSessionPlugin();
        final var toolboxPlugin = buildingToolboxPlugin();

        final var agent = ReActAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_PLUS)
                .plugins(plugins -> {
                    plugins.add(sessionPlugin);
                    plugins.add(toolboxPlugin);
                    return plugins;
                })

                .build();

        {
            final var outbound = Flux.from(agent.flow(sessionId, Message.user("""
                            根据杭州今天天气生成一幅山水画，画上要有地名、天气、时间，并且保存到./weather.png
                            """)))
                    .reduce(AssistantMessage::accumulate)
                    .toFuture()
                    .join();
            System.out.println(outbound.text());
        }

//        {
//            final var outbound = agent.async(sessionId, Message.user("""
//                            根据杭州今天天气生成一幅山水画，画上要有地名、天气、时间，并且保存到./weather.png
//                            """))
//                    .toCompletableFuture()
//                    .join();
//
//            System.out.println(outbound.text());
//        }

    }

    @Test
    public void debug$2() {

        final var prompt = """
                # Role
                你是一个高级 AI 任务规划专家（Planner）。你的任务是将用户的复杂需求拆解为结构化、可执行的 `Plan` 对象。
                
                # Core Concept: Data Flow Architecture
                你必须将生成的 `Plan` 视为一个全局内存数据库。每个 `Step` 都是一个处理函数，它们通过 `arguments` 和 `result` 进行数据传递。
                - **写入数据**：Step 执行后，其产出会自动存入 `step_id.result`。
                - **读取数据**：后续 Step 必须在 `arguments` 中使用 `${step_id.result.field_name}` 语法来引用前置步骤的数据。
                
                # Rules
                1. **原子化**：每个 Step 必须只做一件事。
                2. **强依赖声明**：如果 Step B 需要 Step A 的数据，必须在 `dependencyIds` 中声明 `["step_A"]`，并在 `arguments` 中使用 `${step_A.result.xxx}`。
                3. **Plan 级变量**：所有步骤都可以随时回顾总目标，使用 `${plan.goal}`。
                4. **格式约束**：只输出合法的 JSON，不要包含任何 Markdown 标记或解释性文字。
                
                # Data Flow & Variable Safety Rules (极其重要)
                1. **禁止脑补字段**：工具（Tool）的输出结构是未知的（黑盒）。在 `arguments` 中引用前置步骤结果时，**绝对不要**自行编造深层嵌套字段（如 `${step_1.result.target_date}`）。
                2. **优先整体传递**：如果不确定工具返回的具体字段名，请直接传递整个结果对象，例如 `"date_obj": "${step_1.result}"`。让执行器（Executor）自行处理。
                3. **顶层字段引用**：如果你确信工具会返回某个字段，只能引用 `result` 的第一层属性（如 `${step_1.result.date}`），禁止超过两层。
                4. **描述中声明预期**：在 `description` 中明确写出你期望该步骤返回什么数据，以便 Executor 能够按照预期格式化输出。
                
                # Output Format (Strict JSON)
                {
                  "id": "uuid-string",
                  "goal": "用户的原始目标",
                  "steps": [
                    {
                      "id": "step_1",
                      "name": "步骤名称",
                      "description": "详细的执行指令...",
                      "status": "PENDING",
                      "dependencyIds": [],
                      "arguments": {
                        "query": "静态参数"
                      },
                      "result": null,
                      "message": null
                    },
                    {
                      "id": "step_2",
                      "name": "依赖步骤",
                      "description": "使用前置步骤的结果进行处理...",
                      "status": "PENDING",
                      "dependencyIds": ["step_1"],
                      "arguments": {
                        "input_data": "${step_1.result}",
                        "goal_review": "${plan.goal}"
                      },
                      "result": null,
                      "message": null
                    }
                  ]
                }
                """;

        final var request = AigcRequest.newBuilder(ChatModel.QWEN_FLASH)
                .input(ChatModel.Input.newBuilder()
                        .messages(List.of(
                                Message.system(prompt),
                                Message.user("明天杭州天气如何")
                        ))
                        .build())
                .build();

        final var outbound = client.async(request)
                .thenApply(response->response.output().best().message().text())
                .toCompletableFuture()
                .join();

        System.out.println(outbound);
//        final var plan = JacksonJsonUtils.toObject(outbound, Plan.class);
//        System.out.println(plan);


    }


}
