package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TypeDefinitionImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeDefinitionImpl<?>> {

        @Override
        public void serialize(TypeDefinitionImpl<?> definition,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", definition.getTypeName(), gen);
            if (definition.getKind() != TypeDefinition.Kind.OBJECT) {
                gen.writeFieldName("kind");
                gen.writeString(definition.getKind().name());
            }
            if (definition.getDoc() != null) {
                provider.defaultSerializeField("doc", definition.getDoc(), gen);
            }
            if (definition.getError() != null) {
                provider.defaultSerializeField("error", definition.getError(), gen);
            }
            if (definition.isApiIgnore()) {
                gen.writeFieldName("apiIgnore");
                gen.writeBoolean(true);
            }
            if (definition.getGroups() != null) {
                provider.defaultSerializeField("groups", definition.getGroups(), gen);
            }
            if (!definition.getPropMap().isEmpty()) {
                provider.defaultSerializeField("props", definition.getPropMap().values(), gen);
            }
            if (!definition.getSuperTypes().isEmpty()) {
                provider.defaultSerializeField("superTypes", definition.getSuperTypes(), gen);
            }
            if (!definition.getPolymorphicBranches().isEmpty()) {
                provider.defaultSerializeField("branches", definition.getPolymorphicBranches(), gen);
            }
            if (!definition.getEnumConstantMap().isEmpty()) {
                provider.defaultSerializeField("constants", definition.getEnumConstantMap().values(), gen);
            }
            gen.writeEndObject();
        }
    }
