package io.github.oldmanpushcart.dashscope4j.client.util.jackson;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.util.Map;

/**
 * 允许注入 null 的 InjectableValues
 */
class NullableInjectableValues extends InjectableValues.Std {

    public NullableInjectableValues(Map<String, Object> values) {
        super(values);
    }

    @Override
    public Object findInjectableValue(final DeserializationContext ctxt,
                                      final Object valueId,
                                      final BeanProperty forProperty,
                                      final Object beanInstance,
                                      final Boolean optional,
                                      final Boolean useInput
    ) throws JsonMappingException {
        try {
            return super.findInjectableValue(ctxt, valueId, forProperty, beanInstance, optional, useInput);
        } catch (IllegalArgumentException ex) {

            // 如果找不到 injectable value，则返回 null
            if (ex.getMessage().contains("No injectable id with value")) {
                return null;
            }

            throw ex;
        }
    }

}
