package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;

import java.lang.reflect.Type;

public class SchemaUtils {

    private static final SchemaGenerator GENERATOR = new SchemaGenerator(
            new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)

                    /*
                     * 支持 Jackson 注解
                     */
                    .with(new JacksonModule(

                            /*
                             * 启用识别 @JsonProperty 的 required 属性
                             */
                            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,

                            /*
                             * 启用识别枚举上的 @JsonProperty
                             */
                            JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY

                    ))

                    /*
                     * 支持 Jakarta Validation 注解
                     */
                    .with(new JakartaValidationModule())

                    /*
                     * 启用额外的 OpenAPI 兼容格式（format）值。
                     * 使生成的 JSON Schema 更符合 OpenAPI Specification 对 format 字段的约定。
                     */
                    .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)

                    /*
                     * 禁用“将 Enum 的 toString() 结果作为枚举值”的行为，
                     * 强制使用 Enum 常量名（name） 作为 JSON Schema 中的 enum 值。
                     */
                    .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)

                    /*
                     * 禁用 schema 版本信息输出，
                     * 例如："$schema":"https://json-schema.org/draft/2020-12/schema"
                     */
                    .without(Option.SCHEMA_VERSION_INDICATOR)
                    .build()

    );

    public static JsonNode schema(Type type) {
        return GENERATOR.generateSchema(type);
    }

}
