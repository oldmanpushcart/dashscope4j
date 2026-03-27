package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill tool loader that registers skill-based tools to the tool repository.
 * Works with multiple SkillLoader instances, each managing a single skill.
 * 
 * <h2>Design Principles:</h2>
 * <ul>
 *     <li><strong>SkillLoader Responsibility:</strong> Loads and manages a single skill's resources</li>
 *     <li><strong>SkillToolLoader Responsibility:</strong> Registers tools based on skill definitions</li>
 *     <li><strong>Resource Isolation:</strong> SkillToolLoader never directly accesses files - always through SkillLoader</li>
 * </ul>
 * 
 * <h2>Architecture:</h2>
 * <pre>
 * SkillToolLoader (manages multiple loaders)
 *   ├─ SkillLoader #1 → Skill A
 *   ├─ SkillLoader #2 → Skill B
 *   └─ SkillLoader #3 → Skill C
 * </pre>
 * 
 * <h2>Tools Created per Skill:</h2>
 * <ol>
 *     <li><strong>Main Tool</strong> (skill${name}): Executes the skill's workflow defined in SKILL.md body</li>
 *     <li><strong>Read File Tool</strong> (skill${name}$read-file): Reads any file from skill resources</li>
 *     <li><strong>Execute Script Tool</strong> (skill${name}$exec-script): Executes scripts in skill resources</li>
 * </ol>
 */
public class SkillToolLoader implements Repository.Loader<String, Tool>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SkillToolLoader.class);

    /**
     * Map of skill loaders managed by this loader.
     * Key: skill ID, Value: SkillLoader instance
     */
    private final Map<String, SkillLoader> skillLoaders = new ConcurrentHashMap<>();

    private Repository.Updater<String, Tool> updater;

    /**
     * Constructor.
     *
     * @param skillLoaders Map of skill loaders to manage
     */
    public SkillToolLoader(Map<String, SkillLoader> skillLoaders) {
        this.skillLoaders.putAll(skillLoaders);
    }

    /**
     * Create a new SkillToolLoader instance from a single skill directory.
     *
     * @param skillDir The skill directory containing SKILL.md
     * @return SkillToolLoader instance
     */
    public static SkillToolLoader fromSingleSkill(Path skillDir) {
        FileSkillLoader skillLoader = FileSkillLoader.fromSkillDir(skillDir);
        Map<String, SkillLoader> loaders = new java.util.HashMap<>();
        loaders.put(skillDir.getFileName().toString(), skillLoader);
        return new SkillToolLoader(loaders);
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {
        this.updater = updater;

        // Initialize all skill loaders and load skills in parallel
        List<CompletionStage<Void>> initializations = new ArrayList<>();
        
        for (SkillLoader loader : skillLoaders.values()) {
            initializations.add(CompletableFuture.runAsync(() -> {
                try {
                    loader.init();
                    SkillDefinition skillDef = loader.getSkill();
                    if (skillDef != null) {
                        logger.info("Initialized skill loader: {}", skillDef.name());
                    }
                } catch (Exception e) {
                    logger.error("Failed to initialize skill loader", e);
                }
            }));
        }
        
        return CompletableFuture.allOf(initializations.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    // Create tools for all loaded skills in parallel
                    List<CompletionStage<Void>> toolCreations = new ArrayList<>();
                    for (Map.Entry<String, SkillLoader> entry : skillLoaders.entrySet()) {
                        SkillLoader loader = entry.getValue();
                        SkillDefinition skillDef = loader.getSkill();
                        if (skillDef != null) {
                            toolCreations.add(createSkillToolsAsync(skillDef));
                        }
                    }
                    
                    // Wait for all tool creations to complete
                    return CompletableFuture.allOf(
                        toolCreations.toArray(new CompletableFuture[0])
                    );
                })
                .thenRun(() -> logger.info("Initialized {} skills", skillLoaders.size()));
    }

    /**
     * Create tools for a skill asynchronously.
     */
    private CompletionStage<Void> createSkillToolsAsync(SkillDefinition skillDef) {
        String skillName = skillDef.name();

        // Create all tools in parallel
        List<CompletionStage<Void>> toolCreations = new ArrayList<>();
        
        // 1. Main skill tool
        FunctionTool mainTool = createMainSkillTool(skillDef);
        toolCreations.add(updater.upsert("skill$" + skillName, mainTool)
                .thenRun(() -> logger.info("Created main tool: {}", skillName)));

        // 2. Read-file tool
        FunctionTool readFileTool = createReadFileTool(skillDef);
        toolCreations.add(updater.upsert("skill$" + skillName + "$read-file", readFileTool)
                .thenRun(() -> logger.info("Created read-file tool: {}", skillName)));

        // 3. Execute-script tool
        FunctionTool execScriptTool = createExecuteScriptTool(skillDef);
        toolCreations.add(updater.upsert("skill$" + skillName + "$exec-script", execScriptTool)
                .thenRun(() -> logger.info("Created exec-script tool: {}", skillName)));
        
        // Return a CompletableFuture that completes when all tools are created
        return CompletableFuture.allOf(
            toolCreations.toArray(new CompletableFuture[0])
        );
    }
    
    /**
     * Remove all tools for a skill asynchronously.
     */
    private CompletionStage<Void> removeSkillToolsAsync(String skillId) {
        // Remove all tools in parallel
        List<CompletionStage<Void>> removals = new ArrayList<>();
        
        removals.add(updater.remove("skill$" + skillId));
        removals.add(updater.remove("skill$" + skillId + "$read-file"));
        removals.add(updater.remove("skill$" + skillId + "$exec-script"));
        
        return CompletableFuture.allOf(
                removals.toArray(new CompletableFuture[0])
            )
            .thenRun(() -> logger.info("Removed tools for skill: {}", skillId));
    }

    /**
     * Create main skill tool that executes the workflow defined in SKILL.md body.
     * 
     * <p>The tool will:</p>
     * <ol>
     *     <li>Return the SKILL.md body content as instructions (default behavior)</li>
     *     <li>Optionally execute scripts if specified in the input</li>
     * </ol>
     */
    private FunctionTool createMainSkillTool(SkillDefinition skillDef) {
        return FunctionTool.newBuilder()
                .name("skill$" + skillDef.name())
                .description(buildDescription(skillDef))
                .parameterType(SkillInvocationSpec.class)
                .<SkillInvocationSpec>function((caller, spec) -> {
                    logger.debug("Executing skill: {} with input: {}",
                            skillDef.name(), spec);

                    try {
                        // Return the skill's body content as workflow instructions
                        // The body contains the actual skill logic and prompts
                        return CompletableFuture.completedStage(skillDef.bodyContent());

                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Failed to execute skill: " + e.getMessage(), e)
                        );
                    }
                })
                .build();
    }

    /**
     * Create read-file tool for accessing files within the skill resources.
     * 
     * <p>This tool allows LLM to read any file from the skill's resource directory.</p>
     * <p><strong>Security:</strong> Path traversal is prevented - can only access files within the skill's base directory.</p>
     */
    private FunctionTool createReadFileTool(SkillDefinition skillDef) {
        return FunctionTool.newBuilder()
                .name("skill$" + skillDef.name() + "$read-file")
                .description("Read any file from the skill's resource directory. " +
                        "Use this tool to access skill-specific files such as templates, documentation, or data files. " +
                        "The path parameter must be relative to the skill's base directory. " +
                        "Examples: 'weekly-report-template.md', 'scripts/extract_data.py', 'docs/guide.md'. " +
                        "Note: For security reasons, you can only access files within this skill's directory. " +
                        "Cross-skill access or accessing parent directories is not allowed.")
                .parameterType(FileReadSpec.class)
                .<FileReadSpec>function((caller, spec) -> {
                    try {
                        // Read file through SkillLoader's ResourceProvider
                        String content = readSkillFile(skillDef, spec.path());
                        return CompletableFuture.completedStage(content);
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(
                                new IOException("Failed to read file: " + spec.path() +
                                        " - " + e.getMessage(), e)
                        );
                    } catch (SecurityException e) {
                        return CompletableFuture.failedFuture(
                                new SecurityException("Access denied: " + e.getMessage())
                        );
                    }
                })
                .build();
    }

    /**
     * Create execute-script tool for running scripts in the skill resources.
     * 
     * <p>This tool allows LLM to execute scripts (Python, Node.js, Bash, etc.) within the skill's resource directory.</p>
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *     <li>Path traversal prevention - scripts must be within skill's base directory</li>
     *     <li>Timeout control - scripts are terminated after 60 seconds</li>
     *     <li>Interpreter auto-detection based on file extension</li>
     * </ul>
     */
    private FunctionTool createExecuteScriptTool(SkillDefinition skillDef) {
        return FunctionTool.newBuilder()
                .name("skill$" + skillDef.name() + "$exec-script")
                .description("Execute a script in the skill's resource directory. " +
                        "Supports Python (.py), Node.js (.js), Bash (.sh), Ruby (.rb), and other interpreters. " +
                        "Use this tool to run custom scripts that process data, generate reports, or perform automated tasks. " +
                        "Parameters: script_path (relative path like 'scripts/extract.py'), " +
                        "args (command-line arguments array). " +
                        "Example usage: script_path='scripts/extract_data.py', args=['/mnt/data/invoice.pdf']. " +
                        "Note: Script execution is sandboxed with security restrictions including path validation " +
                        "and a 60-second timeout to prevent resource abuse.")
                .parameterType(ScriptExecSpec.class)
                .<ScriptExecSpec>function((caller, spec) -> {
                    try {
                        // Execute script through SkillLoader's ResourceProvider
                        String result = executeSkillScript(skillDef, spec);
                        return CompletableFuture.completedStage(result);

                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(
                                new IOException("Failed to execute script: " + e.getMessage(), e)
                        );
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Script execution interrupted", e)
                        );
                    } catch (Exception e) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Error executing script: " + e.getMessage(), e)
                        );
                    }
                })
                .build();
    }

    /**
     * Read a file from skill resources through SkillLoader.
     * This is the ONLY way SkillToolLoader accesses files - completely isolated from storage implementation.
     */
    private String readSkillFile(SkillDefinition skillDef, String relativePath) throws IOException {
        // Get the skill loader for this skill and read resource
        SkillLoader loader = skillLoaders.get(skillDef.name());
        if (loader == null) {
            throw new IOException("No skill loader found for skill: " + skillDef.name());
        }
        return loader.withSkill(skillDef)
                     .readResource(relativePath);
    }
    
    /**
     * Execute a script in skill resources through SkillLoader.
     */
    private String executeSkillScript(SkillDefinition skillDef, ScriptExecSpec spec) throws Exception {
        // Get the skill loader for this skill and execute script
        SkillLoader loader = skillLoaders.get(skillDef.name());
        if (loader == null) {
            throw new Exception("No skill loader found for skill: " + skillDef.name());
        }
        return loader.withSkill(skillDef)
                     .executeScript(spec.scriptPath(), spec.args(), spec.interpreter());
    }

    private String buildDescription(SkillDefinition skillDef) {
        return PromptTemplate.newBuilder()
                .template("""
                        ${description}
                        
                        ## Available Capabilities
                        
                        This skill provides the following tools:
                        1. **Main Tool** (this): Execute the complete workflow automatically
                        2. **Read File** (skill$${name}$read-file): Read any file in the skill directory
                        3. **Execute Script** (skill$${name}$exec-script): Run custom scripts with parameters
                        
                        ## Workflow
                        
                        The skill will automatically execute its built-in workflow when called.
                        For advanced usage, you can use the read-file or exec-script sub-tools.
                        
                        ## License
                        ${license}
                        """)
                .variable("description", skillDef.description())
                .variable("name", skillDef.name())
                .variable("license", skillDef.license() != null ? skillDef.license() : "Unknown")
                .build()
                .render();
    }

    @Override
    public void close() {
        for (SkillLoader loader : skillLoaders.values()) {
            try {
                loader.close();
            } catch (Exception e) {
                logger.warn("Error closing skill loader", e);
            }
        }
        skillLoaders.clear();
        logger.info("SkillToolLoader closed");
    }

    /**
     * File read parameter specification.
     */
    public record FileReadSpec(
            @JsonPropertyDescription("Relative file path within the skill directory")
            @JsonProperty("path")
            String path
    ) {
    }

    /**
     * Script execution parameter specification.
     */
    public record ScriptExecSpec(
            @JsonPropertyDescription("Relative script path, e.g., 'scripts/extract.py'")
            @JsonProperty("script_path")
            String scriptPath,

            @JsonPropertyDescription("Command-line arguments array")
            @JsonProperty("args")
            List<String> args,

            @JsonPropertyDescription("Interpreter override (optional), e.g., 'python', 'node', 'bash'")
            @JsonProperty("interpreter")
            String interpreter
    ) {
    }

    /**
     * Skill invocation parameter specification.
     */
    public record SkillInvocationSpec(
            @JsonPropertyDescription("Input parameters (JSON format)")
            @JsonProperty("input")
            String input
    ) {
    }
}
