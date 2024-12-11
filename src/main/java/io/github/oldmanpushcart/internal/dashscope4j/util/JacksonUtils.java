package io.github.oldmanpushcart.internal.dashscope4j.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

public class JacksonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
            .setTimeZone(TimeZone.getTimeZone("GMT+8"))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 压缩Json字符串
     *
     * @param json json
     * @return json
     */
    public static String compact(String json) {
        return toJson(toNode(json));
    }

    /**
     * {@code object -> node}
     *
     * @param object object
     * @return node
     */
    public static JsonNode toNode(Object object) {
        return mapper.valueToTree(object);
    }

    /**
     * {@code object -> json}
     *
     * @param object 目标对象
     * @return json
     */
    public static String toJson(Object object) {
        try {
            return mapper.writer().writeValueAsString(object);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse object to json failed!", cause);
        }
    }

    /**
     * {@code json -> node}
     *
     * @param json json
     * @return node
     */
    public static JsonNode toNode(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("parse json to node failed!", cause);
        }
    }

    /**
     * {@code json -> T}
     *
     * @param json json
     * @param type 对象类型
     * @param <T>  对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    /**
     * {@code json -> T}
     *
     * @param json json
     * @param type 对象类型
     * @param <T>  对象类型
     * @return 目标对象
     */
    public static <T> T toObject(String json, Type type) {
        try {
            final JavaType jType = mapper.constructType(type);
            return mapper.readValue(json, jType);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    /**
     * {@code node -> T}
     * @param node json node
     * @param type 对象类型
     * @return 目标对象
     * @param <T> 对象类型
     */
    public static <T> T toObject(JsonNode node, Class<T> type) {
        try {
            return mapper.treeToValue(node, type);
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException("parse json to object failed!", cause);
        }
    }

    public static ObjectNode newObjectNode() {
        return mapper.createObjectNode();
    }

    /**
     * 生成json-schema描述对象
     *
     * @param type 类型
     * @return json-schema
     */
    public static JsonNode schema(Type type) {
        final JavaType target = mapper.constructType(type);
        final JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);

        try {

            final JsonSchema schema = schemaGen.generateSchema(target);
            final String schemaJson = mapper.writer().writeValueAsString(schema);
            final JsonNode schemaNode = mapper.reader().readTree(schemaJson);

            correct$field$id(schemaNode);
            correct$field$required(schemaNode);
            correct$field$empty_properties(schemaNode);

            return schemaNode;
        } catch (JsonProcessingException cause) {
            throw new IllegalArgumentException(
                    String.format("failed to generate schema for class: %s", type.getTypeName()),
                    cause
            );
        }
    }

    /**
     * 移除json-schema描述对象中的id字段
     *
     * @param node json-schema描述对象
     */
    private static void correct$field$id(JsonNode node) {

        // 不是Json对象不需要修复
        if (!node.isObject()) {
            return;
        }

        // 移除json-schema描述对象中的id字段
        if (node.has("type") && node.get("type").asText().equals("object")) {
            if (node.has("id")) {
                ((ObjectNode) node).remove("id");
            }
            if (node.has("properties")) {
                node.get("properties").fields().forEachRemaining(entry -> correct$field$id(entry.getValue()));
            }
        }

        // 修复json-schema描述数组中的items字段，移除items字段中的对象中的id字段
        else if (node.has("type") && node.get("type").asText().equals("array")) {
            if (node.has("items")) {
                correct$field$id(node.get("items"));
            }
        }

    }

    /**
     * 修复json-schema描述对象中的required字段
     *
     * @param node json-schema描述对象
     */
    private static void correct$field$required(JsonNode node) {

        // 不是Json对象不需要修复
        if (!node.isObject()) {
            return;
        }

        // 修复json-schema描述对象中的required字段
        if (node.has("type") && node.get("type").asText().equals("object")) {
            if (node.has("properties")) {
                final JsonNode propertiesNode = node.get("properties");
                final Set<JsonNode> requiredSet = new LinkedHashSet<JsonNode>();
                propertiesNode.fields().forEachRemaining(entry -> {
                    final String name = entry.getKey();
                    final JsonNode propertyNode = entry.getValue();
                    if (propertyNode.has("required") && propertyNode.get("required").asBoolean()) {
                        requiredSet.add(new TextNode(name));
                        ((ObjectNode) propertyNode).remove("required");
                    }
                    if (propertyNode.isObject()) {
                        correct$field$required(propertyNode);
                    }
                });
                if (!requiredSet.isEmpty()) {
                    ((ObjectNode) node).putArray("required").addAll(requiredSet);
                }
            }
        }

        // 修复json-schema描述数组中的items字段，处理items字段中的对象中的required字段
        else if (node.has("type") && node.get("type").asText().equals("array")) {
            if (node.has("items")) {
                correct$field$required(node.get("items"));
            }
        }

    }

    private static void correct$field$empty_properties(JsonNode node) {

        // 不是Json对象不需要修复
        if (!node.isObject()) {
            return;
        }

        // 修复json-schema描述对象中的required字段
        if (node.has("type") && node.get("type").asText().equals("object")) {
            if (node.has("properties")) {
                final JsonNode propertiesNode = node.get("properties");
                propertiesNode.fields().forEachRemaining(entry -> {
                    final JsonNode propertyNode = entry.getValue();
                    if (propertyNode.isObject()) {
                        correct$field$empty_properties(propertyNode);
                    }
                });
            } else {
                ((ObjectNode) node).putObject("properties");
            }
        }

        // 修复json-schema描述数组中的items字段，处理items字段中的对象中的required字段
        else if (node.has("type") && node.get("type").asText().equals("array")) {
            if (node.has("items")) {
                correct$field$empty_properties(node.get("items"));
            }
        }

    }

}
