package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

class BinLogDeserializer extends StdDeserializer<Object> {

    private final ImmutableType immutableType;

    public BinLogDeserializer(ImmutableType immutableType) {
        super(immutableType.getJavaClass());
        this.immutableType = immutableType;
    }

    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return Internal.produce(immutableType, null, draft -> {
            Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
            while (itr.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = itr.next();
                String columnName = fieldEntry.getKey();
                JsonNode childNode = fieldEntry.getValue();
                ImmutableProp prop = immutableType.getPropByColumnName(columnName);
                Object value;
                if (prop.isAssociation()) {
                    ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
                    Object valueId = ctx.readTreeAsValue(
                            childNode,
                            SimpleType.constructUnsafe(
                                    targetIdProp.getElementClass()
                            )
                    );
                    value = valueId == null ?
                            null :
                            Internal.produce(
                                    prop.getTargetType(),
                                    null,
                                    targetDraft -> {
                                        ((DraftSpi)targetDraft).__set(
                                                targetIdProp.getId(),
                                                valueId
                                        );
                                    }
                            );
                } else {
                    value = ctx.readTreeAsValue(
                            childNode,
                            SimpleType.constructUnsafe(
                                    prop.getElementClass()
                            )
                    );
                }
                ((DraftSpi)draft).__set(prop.getId(), value);
            }
        });
    }
}
