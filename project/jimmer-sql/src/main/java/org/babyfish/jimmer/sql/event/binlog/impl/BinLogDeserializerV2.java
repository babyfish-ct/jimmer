package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.babyfish.jimmer.jackson.v2.NodeV2;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.impl.mutation.EmbeddableObjects;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class BinLogDeserializerV2 extends StdDeserializer<Object> {

    private final BinLogParser parser;

    private final ImmutableType immutableType;

    public BinLogDeserializerV2(
            BinLogParser parser,
            ImmutableType immutableType
    ) {
        super(immutableType.getJavaClass());
        this.parser = parser;
        this.immutableType = immutableType;
    }

    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        MetadataStrategy strategy = parser.sqlClient().getMetadataStrategy();
        JsonNode node = jp.getCodec().readTree(jp);
        return Internal.produce(immutableType, null, draft -> {
            Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
            while (itr.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = itr.next();
                String columnName = fieldEntry.getKey();
                JsonNode childNode = fieldEntry.getValue();
                List<ImmutableProp> chain = immutableType.getPropChain(columnName, strategy);
                ValueParser.addEntityProp((DraftSpi) draft, chain, new NodeV2(childNode), parser);
            }
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (prop.isMutable() && prop.isEmbedded(EmbeddedLevel.BOTH)) {
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
