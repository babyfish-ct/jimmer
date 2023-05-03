package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.babyfish.jimmer.meta.ImmutableType;

public class ImmutableSerializers extends Serializers.Base {

    @Override
    public JsonSerializer<?> findSerializer(
        SerializationConfig config,
        JavaType type,
        BeanDescription beanDesc
    ) {
        Class<?> javaClass = type.getRawClass();
        ImmutableType immutableType = ImmutableType.tryGet(javaClass);
        if (immutableType != null) {
            return new ImmutableSerializer(
                    immutableType,
                    PropNameConverter.of(config, immutableType)
            );
        }
        return null;
    }
}