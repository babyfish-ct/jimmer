package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiOperationImplSerializerV3 extends tools.jackson.databind.ValueSerializer<ApiOperationImpl<?>> {

        @Override
        public void serialize(ApiOperationImpl<?> operation,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString(operation.getName());
            gen.writeName("key");
            gen.writeString(operation.key());
            if (operation.getGroups() != null) {
                ctx.defaultSerializeProperty("groups", operation.getGroups(), gen);
            }
            if (operation.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", operation.getDoc(), gen);
            }
            if (!operation.getParameters().isEmpty()) {
                ctx.defaultSerializeProperty("parameters", operation.getParameters(), gen);
            }
            if (operation.getReturnType() != null) {
                ctx.defaultSerializeProperty("returnType", operation.getReturnType(), gen);
            }
            if (!operation.getExceptionTypes().isEmpty()) {
                gen.writeName("exceptions");
                gen.writeStartArray();
                for (TypeRef exceptionType : operation.getExceptionTypes()) {
                    ctx.writeValue(gen, exceptionType.getTypeName());
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }
