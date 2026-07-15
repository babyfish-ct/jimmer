package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiOperationImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<ApiOperationImpl<?>> {

        @Override
        public void serialize(ApiOperationImpl<?> operation,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(operation.getName());
            gen.writeFieldName("key");
            gen.writeString(operation.key());
            if (operation.getGroups() != null) {
                provider.defaultSerializeField("groups", operation.getGroups(), gen);
            }
            if (operation.getDoc() != null) {
                provider.defaultSerializeField("doc", operation.getDoc(), gen);
            }
            if (!operation.getParameters().isEmpty()) {
                provider.defaultSerializeField("parameters", operation.getParameters(), gen);
            }
            if (operation.getReturnType() != null) {
                provider.defaultSerializeField("returnType", operation.getReturnType(), gen);
            }
            if (!operation.getExceptionTypes().isEmpty()) {
                gen.writeFieldName("exceptions");
                gen.writeStartArray();
                for (TypeRef exceptionType : operation.getExceptionTypes()) {
                    provider.defaultSerializeValue(exceptionType.getTypeName(), gen);
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }
