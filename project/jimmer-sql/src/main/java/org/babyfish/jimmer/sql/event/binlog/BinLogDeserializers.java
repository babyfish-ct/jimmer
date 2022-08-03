package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import org.babyfish.jimmer.meta.ImmutableType;

class BinLogDeserializers extends Deserializers.Base {

    @Override
    public JsonDeserializer<?> findBeanDeserializer(
            JavaType type,
            DeserializationConfig config,
            BeanDescription beanDesc
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(type.getRawClass());
        if (immutableType != null) {
            return new BinLogDeserializer(immutableType);
        }
        return null;
    }
}
