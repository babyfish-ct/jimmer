package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.util.Map;

public class TypeDefinitionVisitor<S> implements AstNodeVisitor<S> {

    private final SchemaBuilder<S> builder;

    private final Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap;

    @SuppressWarnings("unchecked")
    public TypeDefinitionVisitor(SchemaBuilder<S> builder) {
        this.builder = builder;
        SchemaImpl<S> schema = builder.ancestor(SchemaImpl.class);
        this.typeDefinitionMap = (Map<TypeName, TypeDefinitionImpl<S>>) (Map<?, ?>)schema.getTypeDefinitionMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitAstNode(AstNode<S> astNode) {
        if (astNode instanceof TypeRefImpl<?>) {
            TypeName typeName = ((TypeRefImpl<?>) astNode).getTypeName();
            if (!typeName.isGenerationRequired() || typeDefinitionMap.containsKey(typeName)) {
                return;
            }
            S source = builder.loadSource(typeName.toString());
            if (source == null) {
                builder.throwException(
                        builder.ancestorSource(),
                        "Cannot resolve the type name \"" +
                                typeName +
                                "\""
                );
            }
            builder.definition(source, typeName, definition -> {
                typeDefinitionMap.put(typeName, definition);
                builder.fillDefinition(source);
                for (Prop prop : definition.getPropMap().values()) {
                    ((PropImpl<S>) prop).accept(this);
                }
                for (TypeRef superType : definition.getSuperTypes()) {
                    ((TypeRefImpl<S>) superType).accept(this);
                }
            });
        }
    }
}
