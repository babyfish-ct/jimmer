package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SchemaImplSerializerV3 extends tools.jackson.databind.ValueSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("services", schema.getApiServiceMap().values(), gen);
            ctx.defaultSerializeProperty("definitions", schema.getTypeDefinitionMap().values(), gen);
            gen.writeEndObject();
        }
    }
