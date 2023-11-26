package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.util.Iterator;
import java.util.Map;

public class TypeDefinitionVisitor<S> implements AstNodeVisitor<S> {

    private final SchemaBuilder<S> builder;

    private final Map<String, TypeDefinitionImpl<S>> typeDefinitionMap;

    @SuppressWarnings("unchecked")
    public TypeDefinitionVisitor(SchemaBuilder<S> builder) {
        this.builder = builder;
        SchemaImpl<S> schema = builder.ancestor(SchemaImpl.class);
        this.typeDefinitionMap = (Map<String, TypeDefinitionImpl<S>>) (Map<?, ?>)schema.getTypeDefinitionMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitAstNode(AstNode<S> astNode) {
        if (astNode instanceof TypeRefImpl<?>) {
            String typeName = ((TypeRefImpl<?>) astNode).getTypeName();
            if (!TypeDefinition.isGenerationRequired(typeName) ||
                    typeDefinitionMap.containsKey(typeName)) {
                return;
            }
            S source = builder.loadSource(typeName);
            if (source == null) {
                throw builder.typeNameNotFound(typeName);
            }
            builder.definition(source, typeName, definition -> {
                typeDefinitionMap.put(typeName, definition);
                builder.handleDefinition(source);
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
