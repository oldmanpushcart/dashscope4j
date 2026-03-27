package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Skill loader interface responsible for loading and managing a single skill.
 * Each SkillLoader instance manages exactly one skill.
 */
public interface SkillLoader extends AutoCloseable {
    
    /**
     * Initialize the loader.
     * This should be called before any other operations.
     */
    public void init();
    
    /**
     * Get the skill definition managed by this loader.
     * 
     * @return The skill definition, or null if not loaded yet
     */
    public SkillDefinition getSkill();
    
    /**
     * Set the current skill context for resource access operations.
     * This method should be called before readResource() or executeScript().
     * 
     * @param skill The skill definition to set as current context
     * @return This loader instance for method chaining
     */
    public SkillLoader withSkill(SkillDefinition skill);
    
    /**
     * Read a resource file from the current skill context.
     * This is the ONLY way to access skill resources - completely isolated from storage implementation.
     * 
     * <p><strong>Note:</strong> Must call {@link #withSkill(SkillDefinition)} before calling this method.</p>
     * 
     * @param relativePath Relative path within the skill (e.g., "scripts/extract.py")
     * @return File content as string
     * @throws IOException If reading fails or file doesn't exist
     * @throws IllegalStateException if no skill context is set
     */
    public String readResource(String relativePath) throws IOException;
    
    /**
     * Execute a script in the current skill context.
     * This is the ONLY way to execute scripts - completely isolated from execution implementation.
     * 
     * <p><strong>Note:</strong> Must call {@link #withSkill(SkillDefinition)} before calling this method.</p>
     * 
     * @param scriptPath Relative path to the script (e.g., "scripts/extract.py")
     * @param args Command-line arguments for the script
     * @param interpreter Interpreter override (optional), e.g., "python", "node"
     * @return Script output as string
     * @throws Exception If execution fails
     * @throws IllegalStateException if no skill context is set
     */
    public String executeScript(String scriptPath, List<String> args, String interpreter) throws Exception;
    
    @Override
    void close();
}
