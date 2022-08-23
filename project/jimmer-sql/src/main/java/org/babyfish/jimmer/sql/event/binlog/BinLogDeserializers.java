package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.Map;

class BinLogDeserializers extends Deserializers.Base {

    private final Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap;

    BinLogDeserializers(Map<Class<?>, ScalarProvider<?, ?>> scalarProviderMap) {
        this.scalarProviderMap = scalarProviderMap;
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(
            JavaType type,
            DeserializationConfig config,
            BeanDescription beanDesc
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(type.getRawClass());
        if (immutableType != null) {
            return new BinLogDeserializer(scalarProviderMap, immutableType);
        }
        return null;
    }
}
