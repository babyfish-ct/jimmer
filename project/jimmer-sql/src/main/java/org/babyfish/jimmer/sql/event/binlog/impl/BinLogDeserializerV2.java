package org.babyfish.jimmer.sql.event.binlog.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.babyfish.jimmer.jackson.v2.NodeV2;
import org.babyfish.jimmer.meta.ImmutableType;

import java.io.IOException;

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
        JsonNode node = jp.getCodec().readTree(jp);
        return parser.deserializeEntity(immutableType, new NodeV2(node));
    }
}
