package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeRefImplSerializerV3 extends tools.jackson.databind.ValueSerializer<TypeRefImpl<?>> {

        @Override
        public void serialize(TypeRefImpl<?> typeRef,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", typeRef.getTypeName(), gen);
            if (typeRef.isNullable()) {
                gen.writeName("nullable");
                gen.writeBoolean(true);
            }
            if (!typeRef.getArguments().isEmpty()) {
                ctx.defaultSerializeProperty("arguments", typeRef.getArguments(), gen);
            }
            if (typeRef.getFetchBy() != null) {
                gen.writeName("fetchBy");
                gen.writeString(typeRef.getFetchBy());
                gen.writeName("fetcherOwner");
                gen.writeString(typeRef.getFetcherOwner().toString());
                ctx.defaultSerializeProperty("fetcherDoc", typeRef.getFetcherDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
