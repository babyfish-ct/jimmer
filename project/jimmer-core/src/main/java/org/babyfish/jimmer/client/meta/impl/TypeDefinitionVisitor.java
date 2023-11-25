package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeDefinitionVisitor<S> implements TypeNameVisitor {

    private final SchemaBuilder<S> builder;

    private final Map<String, TypeDefinitionImpl<S>> typeDefinitionMap;

    private final Set<String> usedTypeNames = new HashSet<>();

    @SuppressWarnings("unchecked")
    public TypeDefinitionVisitor(SchemaBuilder<S> builder) {
        this.builder = builder;
        SchemaImpl<S> schema = builder.ancestor(SchemaImpl.class);
        this.typeDefinitionMap = (Map<String, TypeDefinitionImpl<S>>) (Map<?, ?>)schema.getTypeDefinitionMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitTypeName(String typeName) {
        usedTypeNames.add(typeName);
        if (TypeNameVisitor.isSupportedByClient(typeName) ||
                typeName.startsWith("<") ||
                typeDefinitionMap.containsKey(typeName)) {
            return;
        }
        S source = builder.loadSource(typeName);
        builder.definition(source, typeName, definition -> {
            typeDefinitionMap.put(typeName, definition);
            builder.handleDefinition(source);
            for (Prop prop : definition.getPropMap().values()) {
                ((PropImpl<S>) prop).accept(this);
            }
            for (TypeRef superType : definition.getSuperTypes()) {
                ((TypeRefImpl<S>)superType).accept(this);
            }
        });
    }

    public void clearUnusedDefinitions() {
        typeDefinitionMap.keySet().retainAll(usedTypeNames);
    }
}
