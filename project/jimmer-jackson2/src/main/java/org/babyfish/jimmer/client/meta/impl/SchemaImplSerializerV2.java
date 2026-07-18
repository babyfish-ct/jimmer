package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SchemaImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("services", schema.getApiServiceMap().values(), gen);
            provider.defaultSerializeField("definitions", schema.getTypeDefinitionMap().values(), gen);
            gen.writeEndObject();
        }
    }
