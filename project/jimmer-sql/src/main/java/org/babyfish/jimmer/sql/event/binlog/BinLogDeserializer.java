package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.meta.MultipleColumns;

import java.io.IOException;
import java.util.*;

class BinLogDeserializer extends StdDeserializer<Object> {

    private final JSqlClient sqlClient;

    private final ImmutableType immutableType;

    public BinLogDeserializer(
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(immutableType.getJavaClass());
        this.sqlClient = sqlClient;
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
                List<ImmutableProp> chain = immutableType.getPropChainByColumnName(columnName);
                ValueParser.addEntityProp((DraftSpi) draft, chain, childNode, sqlClient);
            }
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
                    if (!EmbeddableObjects.isCompleted(((DraftSpi) draft).__get(prop.getId()))) {
                        if (!prop.isNullable()) {
                            throw new IllegalArgumentException(
                                    "Illegal binlog data, the property \"" + prop + "\" is not nullable"
                            );
                        }
                        ((DraftSpi) draft).__set(prop.getId(), null);
                    }
                }
            }
        });
    }
}
