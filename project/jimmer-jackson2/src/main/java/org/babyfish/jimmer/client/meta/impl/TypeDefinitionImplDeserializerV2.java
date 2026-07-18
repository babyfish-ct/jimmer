package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TypeDefinitionImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeDefinitionImpl<?>> {

        private static final com.fasterxml.jackson.databind.JavaType GROUPS_TYPE =
                com.fasterxml.jackson.databind.type.CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        com.fasterxml.jackson.databind.type.SimpleType.constructUnsafe(String.class)
                );

        @SuppressWarnings("unchecked")
        @Override
        public TypeDefinitionImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                                 com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeDefinitionImpl<Object> definition = new TypeDefinitionImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("groups")) {
                definition.mergeGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"), GROUPS_TYPE)
                        )
                );
                if (!Schemas.isAllowed(definition.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                    return definition;
                }
            } else {
                definition.mergeGroups(null);
            }
            if (jsonNode.has("kind")) {
                definition.setKind(TypeDefinition.Kind.valueOf(jsonNode.get("kind").asText()));
            }
            if (jsonNode.has("doc")) {
                definition.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            if (jsonNode.has("error")) {
                com.fasterxml.jackson.databind.JsonNode errorNode = jsonNode.get("error");
                definition.setError(new TypeDefinition.Error(errorNode.get("family").asText(), errorNode.get("code").asText()));
            }
            if (jsonNode.has("apiIgnore")) {
                definition.setApiIgnore(jsonNode.get("apiIgnore").asBoolean());
            }
            if (jsonNode.has("props")) {
                for (com.fasterxml.jackson.databind.JsonNode propNode : jsonNode.get("props")) {
                    definition.addProp(ctx.readTreeAsValue(propNode, PropImpl.class));
                }
            }
            if (jsonNode.has("superTypes")) {
                for (com.fasterxml.jackson.databind.JsonNode superNode : jsonNode.get("superTypes")) {
                    definition.addSuperType(ctx.readTreeAsValue(superNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("branches")) {
                for (com.fasterxml.jackson.databind.JsonNode branchNode : jsonNode.get("branches")) {
                    definition.addPolymorphicBranch(ctx.readTreeAsValue(branchNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("constants")) {
                for (com.fasterxml.jackson.databind.JsonNode propNode : jsonNode.get("constants")) {
                    definition.addEnumConstant(ctx.readTreeAsValue(propNode, EnumConstantImpl.class));
                }
            }
            return definition;
        }
    }
