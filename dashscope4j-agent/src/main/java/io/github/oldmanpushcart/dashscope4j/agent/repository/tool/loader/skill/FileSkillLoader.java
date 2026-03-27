package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File system based SkillLoader implementation.
 * Each FileSkillLoader instance manages exactly one skill.
 * 
 * <p>This implementation uses {@link FileSystemResourceProvider} to encapsulate all file system
 * operations. The SkillLoader base class has NO direct knowledge of file system access.</p>
 * 
 * <p><strong>Note:</strong> This loader does NOT support dynamic file watching.
 * If the skill file changes, you need to manually recreate the loader.</p>
 */
public class FileSkillLoader extends AbstractSkillLoader {
    
    private final Path skillDir;
    private final String skillId;
    
    private FileSkillLoader(Builder builder) {
        this.skillDir = builder.skillDir.toAbsolutePath().normalize();
        this.skillId = builder.skillDir.getFileName().toString();
        
        logger.info("FileSkillLoader initialized for skill: {} at {}", 
                skillId, this.skillDir);
    }
    
    /**
     * Create a new Builder instance.
     *
     * @return Builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }
    
    /**
     * Create a FileSkillLoader for a specific skill directory.
     *
     * @param skillDir The skill directory containing SKILL.md
     * @return FileSkillLoader instance
     */
    public static FileSkillLoader fromSkillDir(Path skillDir) {
        return newBuilder()
                .skillDir(skillDir)
                .build();
    }
    
    @Override
    protected Path getBasePath() {
        return skillDir;
    }
    
    @Override
    protected String readContent(String relativePath) throws IOException {
        Path fullPath = skillDir.resolve(relativePath).normalize();
        
        // Security check
        if (!fullPath.startsWith(skillDir)) {
            throw new SecurityException(
                "Path traversal attempt detected: " + relativePath
            );
        }
        
        if (!java.nio.file.Files.exists(fullPath)) {
            throw new java.nio.file.NoSuchFileException("Resource not found: " + relativePath);
        }
        
        return java.nio.file.Files.readString(fullPath);
    }
    
    @Override
    protected boolean exists(String relativePath) {
        Path fullPath = skillDir.resolve(relativePath).normalize();
        return java.nio.file.Files.exists(fullPath);
    }
    
    @Override
    protected SkillDefinition loadSkill() throws Exception {
        try {
            // Read SKILL.md content directly
            String skillMdContent = readContent("SKILL.md");
            
            // Split YAML frontmatter and markdown body
            String[] parts = splitFrontmatter(skillMdContent);
            if (parts == null) {
                logger.error("Invalid SKILL.md format for skill: {}", skillId);
                return null;
            }
            
            String yamlContent = parts[0];
            String bodyContent = parts[1];
            Path baseDir = getBasePath();
            
            return parseSkill(yamlContent, bodyContent, baseDir);
            
        } catch (IOException e) {
            logger.error("Failed to load skill: {}", skillId, e);
            return null;
        }
    }
    
    /**
     * Split SKILL.md content into YAML frontmatter and markdown body.
     * 
     * @param content Full content of SKILL.md
     * @return Array of [yamlContent, bodyContent], or null if invalid format
     */
    private String[] splitFrontmatter(String content) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "^---\\s*$\n(.*?)^---\\s*$\n(.*)$",
            java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (!matcher.matches()) {
            return null;
        }
        
        return new String[] {
            matcher.group(1).trim(),
            matcher.group(2).trim()
        };
    }
    
    /**
     * Builder for creating FileSkillLoader instances.
     */
    public static class Builder implements Buildable<FileSkillLoader, Builder> {
        
        private Path skillDir;
        
        /**
         * Set the skill directory.
         *
         * @param skillDir The directory containing SKILL.md
         * @return This builder instance
         */
        public Builder skillDir(Path skillDir) {
            this.skillDir = skillDir;
            return this;
        }
        
        /**
         * Validate and build the FileSkillLoader.
         *
         * @return FileSkillLoader instance
         * @throws IllegalArgumentException if skillDir is not set or doesn't exist
         */
        public FileSkillLoader build() {
            if (skillDir == null) {
                throw new IllegalArgumentException("skillDir is required");
            }
            
            if (!Files.isDirectory(skillDir)) {
                throw new IllegalArgumentException("skillDir must be a valid directory: " + skillDir);
            }
            
            if (!Files.exists(skillDir.resolve("SKILL.md"))) {
                throw new IllegalArgumentException("SKILL.md not found in: " + skillDir);
            }
            
            return new FileSkillLoader(this);
        }
    }
}
