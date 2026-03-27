package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for SkillLoader implementations.
 * Provides common parsing logic and resource access methods.
 */
public abstract class AbstractSkillLoader implements SkillLoader {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * The single skill managed by this loader.
     */
    private SkillDefinition skill;
    
    /**
     * Current skill context for resource access operations.
     */
    private SkillDefinition currentSkill;
    
    @Override
    public void init() {
        // Load the skill during initialization
        try {
            this.skill = loadSkill();
            if (this.skill == null) {
                throw new IllegalStateException("Failed to load skill");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize skill loader", e);
        }
    }
    
    /**
     * Load the skill definition. This method should be implemented by subclasses.
     * 
     * @return The loaded skill definition, or null if not found
     * @throws Exception If loading fails
     */
    protected abstract SkillDefinition loadSkill() throws Exception;
    
    @Override
    public SkillDefinition getSkill() {
        return skill;
    }
    
    @Override
    public SkillLoader withSkill(SkillDefinition skill) {
        this.currentSkill = skill;
        return this;
    }
    
    /**
     * Get the current skill context.
     * @return The current skill definition
     * @throws IllegalStateException if no skill context is set
     */
    protected SkillDefinition getCurrentSkill() {
        if (currentSkill == null) {
            throw new IllegalStateException(
                "No skill context set. Please call withSkill(skill) before accessing resources."
            );
        }
        return currentSkill;
    }
    
    /**
     * Read content from a relative path within the skill directory.
     * To be implemented by subclasses.
     * 
     * @param relativePath Relative path within the skill
     * @return Content as string
     * @throws IOException If reading fails
     */
    protected abstract String readContent(String relativePath) throws IOException;
    
    /**
     * Check if a resource exists at the given relative path.
     * To be implemented by subclasses.
     * 
     * @param relativePath Relative path within the skill
     * @return true if exists, false otherwise
     */
    protected abstract boolean exists(String relativePath);
    
    /**
     * Get the base path for the skill (for script execution sandbox).
     * To be implemented by subclasses.
     * 
     * @return Base path, or null if not applicable
     */
    protected abstract Path getBasePath();
    
    /**
     * Parse SKILL.md content into SkillDefinition.
     * This is the common parsing logic shared by all implementations.
     * 
     * @param yamlContent YAML frontmatter content
     * @param bodyContent Markdown body content
     * @param baseDir Base directory for the skill
     * @return Parsed SkillDefinition, or null if parsing fails
     */
    protected SkillDefinition parseSkill(
            String yamlContent,
            String bodyContent,
            Path baseDir) {
        
        try {
            // Deserialize YAML to SkillDefinition using Jackson annotations
            SkillDefinition yamlDef = SkillDefinition.YAML_MAPPER.readValue(yamlContent, SkillDefinition.class);
            
            // Validate required fields
            if (yamlDef.name() == null || yamlDef.name().isEmpty()) {
                logger.error("Missing required field 'name' in SKILL.md");
                return null;
            }
            
            // Create final SkillDefinition with body content and base directory
            return new SkillDefinition(
                yamlDef.name(),
                yamlDef.description() != null ? yamlDef.description() : "",
                yamlDef.license(),
                yamlDef.compatibility(),
                yamlDef.metadata() != null ? yamlDef.metadata() : Map.of(),
                yamlDef.allowedTools() != null ? yamlDef.allowedTools() : List.of(),
                bodyContent != null ? bodyContent : "",
                baseDir != null ? baseDir.normalize() : null
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse SKILL.md: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String readResource(String relativePath) throws IOException {
        return readContent(relativePath);
    }
    
    @Override
    public String executeScript(String scriptPath, List<String> args, String interpreter) throws Exception {
        SkillDefinition skill = getCurrentSkill();
        Path baseDir = getBasePath();
        
        if (baseDir == null) {
            throw new IOException("Skill has no base directory (required for script execution)");
        }
        
        // Resolve script path
        Path fullPath = baseDir.resolve(scriptPath).normalize();
        
        // Security check
        if (!fullPath.startsWith(baseDir)) {
            throw new SecurityException(
                "Script path traversal attempt detected: " + scriptPath
            );
        }
        
        if (!java.nio.file.Files.exists(fullPath)) {
            throw new NoSuchFileException("Script not found: " + scriptPath);
        }
        
        // Determine interpreter
        String actualInterpreter = determineInterpreter(fullPath, interpreter);
        
        // Build command
        List<String> command = new ArrayList<>();
        command.add(actualInterpreter);
        command.add(fullPath.toAbsolutePath().toString());
        if (args != null) {
            command.addAll(args);
        }
        
        // Execute script
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(baseDir.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            return "{\"error\": \"Script execution timeout after 60 seconds\"}";
        }
        
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.exitValue();
        
        if (exitCode == 0) {
            return output;
        } else {
            return String.format(
                "{\"error\": \"Script failed with exit code %d\", \"output\": \"%s\"}",
                exitCode, escapeJson(output)
            );
        }
    }
    
    /**
     * Determine the interpreter for a script based on file extension or specified interpreter.
     */
    private String determineInterpreter(Path scriptPath, String specifiedInterpreter) {
        if (specifiedInterpreter != null && !specifiedInterpreter.isEmpty()) {
            return specifiedInterpreter;
        }
        
        // Auto-detect based on file extension
        String fileName = scriptPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".py")) return "python";
        if (fileName.endsWith(".js")) return "node";
        if (fileName.endsWith(".sh")) return "bash";
        if (fileName.endsWith(".rb")) return "ruby";
        
        // Default to bash
        return "bash";
    }
    
    /**
     * Escape special characters for JSON string.
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Close the loader and release resources.
     * Default implementation does nothing.
     */
    @Override
    public void close() {
        // No resources to close in base class
    }
}
