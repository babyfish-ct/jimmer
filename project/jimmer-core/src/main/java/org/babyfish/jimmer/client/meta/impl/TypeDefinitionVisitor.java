package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.util.IdentityHashMap;
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
    public boolean visitAstNode(AstNode<S> astNode) {
        if (isContextNode(astNode)) {
            builder.push(astNode);
        } else if (astNode instanceof TypeRefImpl<?>) {
            TypeName typeName = ((TypeRefImpl<?>) astNode).getTypeName();
            if (!typeName.isGenerationRequired()) {
                return true;
            }
            List<String> groups = builder.<ApiOperationImpl<S>>ancestor(ApiOperationImpl.class).getGroups();
            if (groups == null) {
                groups = builder.<ApiServiceImpl<S>>ancestor(ApiServiceImpl.class).getGroups();
            }
            TypeDefinitionImpl<S> existingDefinition = typeDefinitionMap.get(typeName);
            if (existingDefinition != null) {
                existingDefinition.accept(new ApplyGroupsVisitor<>(typeDefinitionMap, groups));
                return true;
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
                if (builder.getThisModuleTypeNameList().contains(typeName)) {
                    definition.setRealModuleType(true);
                }
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
        return true;
    }

    @Override
    public boolean visitAstNode(AstNode<S> astNode,boolean onlySaveCurrentModuleClass) {
        if (isContextNode(astNode)) {
            builder.push(astNode);
        } else if (astNode instanceof TypeRefImpl<?>) {
            TypeName typeName = ((TypeRefImpl<?>) astNode).getTypeName();
            if (!typeName.isGenerationRequired()) {
                return true;
            }
            if (onlySaveCurrentModuleClass && !builder.getThisModuleTypeNameList().contains(typeName)){
                return true;
            }
            ApiOperationImpl<S> ancestor = builder.ancestor(ApiOperationImpl.class);
            List<String> groups = null;
            if (ancestor == null || ancestor.getGroups() == null ) {
                ApiServiceImpl<S> ancestor1 = builder.ancestor(ApiServiceImpl.class);
                if (ancestor1 != null) {
                    groups = ancestor1.getGroups();
                }
            }
            TypeDefinitionImpl<S> existingDefinition = typeDefinitionMap.get(typeName);
            if (existingDefinition != null) {
                existingDefinition.accept(new ApplyGroupsVisitor<>(typeDefinitionMap, groups));
                return true;
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
                if (builder.getThisModuleTypeNameList().contains(typeName)) {
                    definition.setRealModuleType(true);
                }
                typeDefinitionMap.put(typeName, definition);
                definition.mergeGroups(finalGroups);
                builder.fillDefinition(source);
                for (Prop prop : definition.getPropMap().values()) {
                    ((PropImpl<S>) prop).accept(this,onlySaveCurrentModuleClass);
                }
                for (TypeRef superType : definition.getSuperTypes()) {
                    ((TypeRefImpl<S>) superType).accept(this,onlySaveCurrentModuleClass);
                }
            });
        }
        return true;
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

    private static class ApplyGroupsVisitor<S> implements AstNodeVisitor<S> {

        private static final Object PRESENT = new Object();

        private final IdentityHashMap<AstNode<S>, Object> map = new IdentityHashMap<>();

        private final Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap;

        private final List<String> groups;

        private ApplyGroupsVisitor(Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap, List<String> groups) {
            this.typeDefinitionMap = typeDefinitionMap;
            this.groups = groups;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean visitAstNode(AstNode<S> astNode) {
            if (astNode instanceof TypeDefinitionImpl<?>) {
                TypeDefinitionImpl<S> definition = (TypeDefinitionImpl<S>) astNode;
                if (map.put(definition, PRESENT) != null) {
                    return false;
                }
                definition.mergeGroups(groups);
            } else if (astNode instanceof TypeRefImpl<?>) {
                TypeDefinitionImpl<S> definition = typeDefinitionMap.get(((TypeRefImpl<?>)astNode).getTypeName());
                if (definition != null) {
                    definition.accept(this);
                }
            }
            return true;
        }
    }
}
