package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;

class BinLogDeserializers extends Deserializers.Base {

    private final JSqlClient sqlClient;

    BinLogDeserializers(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(
            JavaType type,
            DeserializationConfig config,
            BeanDescription beanDesc
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(type.getRawClass());
        if (immutableType != null) {
            return new BinLogDeserializer(sqlClient, immutableType);
        }
        return null;
    }
}
