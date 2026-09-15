package org.babyfish.jimmer.sql.event.binlog.impl;

import org.babyfish.jimmer.jackson.v3.NodeV3;
import org.babyfish.jimmer.meta.ImmutableType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

class BinLogDeserializerV3 extends StdDeserializer<Object> {

    private final BinLogParser parser;

    private final ImmutableType immutableType;

    public BinLogDeserializerV3(
            BinLogParser parser,
            ImmutableType immutableType
    ) {
        super(immutableType.getJavaClass());
        this.parser = parser;
        this.immutableType = immutableType;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        return parser.deserializeEntity(immutableType, new NodeV3(node));
    }
}
