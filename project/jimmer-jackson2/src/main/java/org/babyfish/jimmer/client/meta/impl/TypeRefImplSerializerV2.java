package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeRefImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeRefImpl<?>> {

        @Override
        public void serialize(TypeRefImpl<?> typeRef,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", typeRef.getTypeName(), gen);
            if (typeRef.isNullable()) {
                gen.writeFieldName("nullable");
                gen.writeBoolean(true);
            }
            if (!typeRef.getArguments().isEmpty()) {
                provider.defaultSerializeField("arguments", typeRef.getArguments(), gen);
            }
            if (typeRef.getFetchBy() != null) {
                gen.writeFieldName("fetchBy");
                gen.writeString(typeRef.getFetchBy());
                gen.writeFieldName("fetcherOwner");
                gen.writeString(typeRef.getFetcherOwner().toString());
                provider.defaultSerializeField("fetcherDoc", typeRef.getFetcherDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
