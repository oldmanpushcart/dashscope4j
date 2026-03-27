package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Skill definition loaded from SKILL.md file.
 * Used for both YAML deserialization and runtime representation.
 * 
 * @param name           Skill name (must match directory name)
 * @param description    Skill description
 * @param license        License type
 * @param compatibility  Compatibility information
 * @param metadata       Metadata map
 * @param allowedTools   List of allowed tools
 * @param bodyContent    Markdown body content (Instructions)
 * @param baseDir        Base directory path of the skill (absolute path, not in YAML)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillDefinition(
        @JsonProperty("name")
        String name,
        
        @JsonProperty("description")
        String description,
        
        @JsonProperty("license")
        String license,
        
        @JsonProperty("compatibility")
        String compatibility,
        
        @JsonProperty("metadata")
        Map<String, String> metadata,
        
        @JsonProperty("allowed-tools")
        List<String> allowedTools,
        
        @JsonProperty("body")
        String bodyContent,
        
        // This field is set programmatically, not from YAML
        Path baseDir
) {
    
    /**
     * Shared YAML mapper instance for all implementations.
     */
    public static final YAMLMapper YAML_MAPPER = new YAMLMapper();
}
