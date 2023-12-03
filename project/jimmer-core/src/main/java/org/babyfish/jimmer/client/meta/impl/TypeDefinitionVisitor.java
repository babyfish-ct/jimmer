package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.util.List;
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
        if (isContextNode(astNode)) {
            builder.push(astNode);
        } else if (astNode instanceof TypeRefImpl<?>) {
            TypeName typeName = ((TypeRefImpl<?>) astNode).getTypeName();
            if (!typeName.isGenerationRequired()) {
                return;
            }
            List<String> groups = builder.<ApiOperationImpl<S>>ancestor(ApiOperationImpl.class).getGroups();
            if (groups == null) {
                groups = builder.<ApiServiceImpl<S>>ancestor(ApiServiceImpl.class).getGroups();
            }
            TypeDefinitionImpl<S> existingDefinition = typeDefinitionMap.get(typeName);
            if (existingDefinition != null) {
                existingDefinition.mergeGroups(groups);
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
            List<String> finalGroups = groups;
            builder.definition(source, typeName, definition -> {
                typeDefinitionMap.put(typeName, definition);
                definition.mergeGroups(finalGroups);
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

    @Override
    public void visitedAstNode(AstNode<S> astNode) {
        if (isContextNode(astNode)) {
            builder.pop();
        }
    }

    private static boolean isContextNode(AstNode<?> astNode) {
        return astNode instanceof ApiServiceImpl<?> ||
                astNode instanceof ApiOperationImpl<?> ||
                astNode instanceof ApiParameterImpl<?>;
    }
}
