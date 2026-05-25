package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * 获取 Skill 中的静态资源文件路径
 */
class GetAssetFunction implements Function<GetAssetFunction.Spec, URI> {

    private final Map<String, Skill> skills;

    public GetAssetFunction(Map<String, Skill> skills) {
        this.skills = skills;
    }

    @Override
    public URI apply(Spec spec) {

        final var skill = skills.get(spec.name());
        if (null == skill) {
            throw new IllegalArgumentException("Skill %s not found!".formatted(spec.name()));
        }

        try {
            final Path assetPath = skill.getAsset(spec.path());
            return assetPath.toUri();
        } catch (Exception ex) {
            final var message = "Get asset error! skill=%s;asset=%s;".formatted(
                    spec.name(),
                    spec.path()
            );
            throw new IllegalStateException(message, ex);
        }

    }

    public Tool asTool() {
        return FunctionTool.newBuilder()
                .name("global$skill$get_asset")
                .description("""
                        获取 Skill 中的静态资源 URI。
                        
                        【使用场景】
                        需要使用 Skill 提供的模板文件、图片、数据文件等二进制资源时使用此工具。
                        例如：获取 Excel 模板用于生成报表、获取图片用于图像处理、获取配置文件用于程序运行等。
                        
                        【参数说明】
                        - skill_name: Skill 名称，如 "data-processor"
                        - asset_path: 资源文件相对路径
                        
                        【路径使用严格规范】
                        ⚠️ 重要：路径参数是**资源的唯一标识符**,必须原样复制：
                        - 从 SKILL.md、README.md 等文档中看到的路径，必须**逐字复制**到 asset_path 参数
                        - 不要修改、优化或"修正"路径格式（例如：不要去掉前缀，不要转换为绝对路径）
                        - 不要对路径进行 URL 编码/解码
                        - 即使路径看起来"奇怪"或"不正确",也要原样保留
                        
                        ❌ 错误示例：
                        - 修改路径的前缀部分
                        - 将相对路径改为绝对路径
                        
                        ✅ 正确示例：
                        - 文档中怎么写，参数就怎么填，保持完全一致
                        
                        【返回结果】
                        临时文件的 URI (file:///...)，可以直接用于文件读取或其他需要文件路径的场景。
                        临时文件会在 SkillToolLoader 关闭时自动清理。
                        
                        【示例】
                        ```json
                        {
                          "skill_name": "report-generator",
                          "asset_path": "assets/excel-template.xlsx"
                        }
                        ```
                        """)
                .parameterType(Spec.class)
                .function(this)
                .build();
    }

    /**
     * 参数类
     */
    record Spec(

            @JsonProperty(value = "skill_name", required = true)
            @JsonPropertyDescription("Skill 名称")
            String name,

            @JsonProperty(value = "asset_path", required = true)
            @JsonPropertyDescription("资源文件相对路径，如 'assets/template.xlsx'")
            String path

    ) {
    }

}
