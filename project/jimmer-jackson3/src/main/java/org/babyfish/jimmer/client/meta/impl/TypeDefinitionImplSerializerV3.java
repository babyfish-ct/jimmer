package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TypeDefinitionImplSerializerV3 extends tools.jackson.databind.ValueSerializer<TypeDefinitionImpl<?>> {

        @Override
        public void serialize(TypeDefinitionImpl<?> definition,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", definition.getTypeName(), gen);
            if (definition.getKind() != TypeDefinition.Kind.OBJECT) {
                gen.writeName("kind");
                gen.writeString(definition.getKind().name());
            }
            if (definition.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", definition.getDoc(), gen);
            }
            if (definition.getError() != null) {
                ctx.defaultSerializeProperty("error", definition.getError(), gen);
            }
            if (definition.isApiIgnore()) {
                gen.writeName("apiIgnore");
                gen.writeBoolean(true);
            }
            if (definition.getGroups() != null) {
                ctx.defaultSerializeProperty("groups", definition.getGroups(), gen);
            }
            if (!definition.getPropMap().isEmpty()) {
                ctx.defaultSerializeProperty("props", definition.getPropMap().values(), gen);
            }
            if (!definition.getSuperTypes().isEmpty()) {
                ctx.defaultSerializeProperty("superTypes", definition.getSuperTypes(), gen);
            }
            if (!definition.getPolymorphicBranches().isEmpty()) {
                ctx.defaultSerializeProperty("branches", definition.getPolymorphicBranches(), gen);
            }
            if (!definition.getEnumConstantMap().isEmpty()) {
                ctx.defaultSerializeProperty("constants", definition.getEnumConstantMap().values(), gen);
            }
            gen.writeEndObject();
        }
    }
