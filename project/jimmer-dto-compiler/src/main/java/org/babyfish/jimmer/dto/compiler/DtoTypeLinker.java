package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;

public final class DtoTypeLinker {

    private DtoTypeLinker() {
    }

    public static <T extends BaseType, P extends BaseProp> void link(
            Collection<DtoType<T, P>> dtoTypes
    ) {
        link(dtoTypes, qualifiedName -> null);
    }

    public static <T extends BaseType, P extends BaseProp> void link(
            Collection<DtoType<T, P>> dtoTypes,
            DtoTypeResolver<T> resolver
    ) {
        LinkContext<T, P> ctx = new LinkContext<>(resolver);
        for (DtoType<T, P> dtoType : dtoTypes) {
            String qualifiedName = dtoType.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            DtoType<T, P> conflictType = ctx.typeMap.put(qualifiedName, dtoType);
            if (conflictType != null) {
                throw new DtoAstException(
                        dtoType.getDtoFile(),
                        1,
                        0,
                        "Duplicated DTO type name \"" + qualifiedName + "\""
                );
            }
            ctx.typeInfoMap.put(qualifiedName, typeInfo(dtoType));
        }

        Map<DtoType<T, P>, List<Edge<T, P>>> graph = new IdentityHashMap<>();
        for (DtoType<T, P> dtoType : dtoTypes) {
            List<Edge<T, P>> edges = new ArrayList<>();
            collectEdges(
                    dtoType,
                    kind(dtoType),
                    dtoType,
                    ctx,
                    Collections.newSetFromMap(new IdentityHashMap<>()),
                    edges
            );
            graph.put(dtoType, edges);
        }
        detectCycles(dtoTypes, graph);
    }

    private static <T extends BaseType, P extends BaseProp> void collectEdges(
            DtoType<T, P> ownerType,
            DtoTypeKind ownerKind,
            DtoType<T, P> shape,
            LinkContext<T, P> ctx,
            Set<DtoType<T, P>> visitedShapes,
            List<Edge<T, P>> edges
    ) {
        if (!visitedShapes.add(shape)) {
            return;
        }
        for (DtoProp<T, P> prop : shape.getDtoProps()) {
            DtoTypeRef<T, P> ref = prop.getTargetTypeRef();
            if (ref != null) {
                DtoType<T, P> targetType = ctx.typeMap.get(ref.getQualifiedName());
                DtoTypeInfo<T> typeInfo = targetType != null ?
                        ctx.typeInfoMap.get(ref.getQualifiedName()) :
                        ctx.resolveExternal(ref.getQualifiedName());
                if (typeInfo == null) {
                    throw exception(
                            ownerType,
                            ref,
                            "Cannot resolve reusable DTO type \"" + ref.getQualifiedName() + "\""
                    );
                }
                validate(ownerType, ownerKind, ref, typeInfo);
                ref.resolve(typeInfo, targetType);
                if (targetType != null) {
                    edges.add(new Edge<>(prop, ref, targetType));
                }
            } else {
                DtoType<T, P> targetType = prop.getTargetType();
                if (targetType != null) {
                    collectEdges(
                            ownerType,
                            ownerKind,
                            targetType,
                            ctx,
                            visitedShapes,
                            edges
                    );
                }
            }
        }
        for (FoldProp<T, P> prop : shape.getFoldProps()) {
            collectEdges(
                    ownerType,
                    ownerKind,
                    prop.getTargetType(),
                    ctx,
                    visitedShapes,
                    edges
            );
        }
        DtoPolymorphism<T, P> polymorphism = shape.getPolymorphism();
        if (polymorphism != null) {
            DtoPolymorphicBranch<T, P> defaultBranch = polymorphism.getDefaultBranch();
            if (defaultBranch != null) {
                collectEdges(
                        ownerType,
                        ownerKind,
                        defaultBranch.getDtoType(),
                        ctx,
                        visitedShapes,
                        edges
                );
            }
            for (DtoPolymorphicBranch<T, P> branch : polymorphism.getTypeBranches()) {
                collectEdges(
                        ownerType,
                        ownerKind,
                        branch.getDtoType(),
                        ctx,
                        visitedShapes,
                        edges
                );
            }
        }
    }

    private static <T extends BaseType, P extends BaseProp> void validate(
            DtoType<T, P> ownerType,
            DtoTypeKind ownerKind,
            DtoTypeRef<T, P> ref,
            DtoTypeInfo<T> typeInfo
    ) {
        if (typeInfo.getKind() != ownerKind) {
            throw exception(
                    ownerType,
                    ref,
                    "Reusable " +
                            propertyKind(ownerKind) +
                            " property requires " +
                            article(ownerKind) +
                            ", but \"" +
                            ref.getQualifiedName() +
                            "\" is " + article(typeInfo.getKind())
            );
        }
        T associationTargetType = ref.getTargetBaseType();
        if (!associationTargetType.getQualifiedName().equals(typeInfo.getBaseType().getQualifiedName())) {
            throw exception(
                    ownerType,
                    ref,
                    "The association target type \"" +
                            associationTargetType.getQualifiedName() +
                            "\" does not match the reusable DTO entity type \"" +
                            typeInfo.getBaseType().getQualifiedName() +
                            "\""
            );
        }
    }

    private static <T extends BaseType, P extends BaseProp> DtoTypeInfo<T> typeInfo(
            DtoType<T, P> dtoType
    ) {
        return new DtoTypeInfo<>(dtoType.getBaseType(), kind(dtoType));
    }

    private static DtoTypeKind kind(DtoType<?, ?> dtoType) {
        if (dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            return DtoTypeKind.INPUT;
        }
        if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            return DtoTypeKind.SPECIFICATION;
        }
        return DtoTypeKind.VIEW;
    }

    private static String article(DtoTypeKind kind) {
        switch (kind) {
            case INPUT:
                return "an input DTO";
            case VIEW:
                return "a view DTO";
            default:
                return "a specification DTO";
        }
    }

    private static String propertyKind(DtoTypeKind kind) {
        switch (kind) {
            case INPUT:
                return "input";
            case VIEW:
                return "output";
            default:
                return "specification";
        }
    }

    private static <T extends BaseType, P extends BaseProp> void detectCycles(
            Collection<DtoType<T, P>> dtoTypes,
            Map<DtoType<T, P>, List<Edge<T, P>>> graph
    ) {
        Set<DtoType<T, P>> completed = Collections.newSetFromMap(new IdentityHashMap<>());
        List<DtoType<T, P>> typePath = new ArrayList<>();
        List<Edge<T, P>> edgePath = new ArrayList<>();
        Map<DtoType<T, P>, Integer> pathIndexMap = new IdentityHashMap<>();
        for (DtoType<T, P> dtoType : dtoTypes) {
            detectCycles(dtoType, graph, completed, typePath, edgePath, pathIndexMap);
        }
    }

    private static <T extends BaseType, P extends BaseProp> void detectCycles(
            DtoType<T, P> type,
            Map<DtoType<T, P>, List<Edge<T, P>>> graph,
            Set<DtoType<T, P>> completed,
            List<DtoType<T, P>> typePath,
            List<Edge<T, P>> edgePath,
            Map<DtoType<T, P>, Integer> pathIndexMap
    ) {
        if (completed.contains(type)) {
            return;
        }
        pathIndexMap.put(type, typePath.size());
        typePath.add(type);
        for (Edge<T, P> edge : graph.getOrDefault(type, Collections.emptyList())) {
            Integer cycleStart = pathIndexMap.get(edge.targetType);
            if (cycleStart != null) {
                StringBuilder builder = new StringBuilder("Circular reusable DTO reference: ");
                for (int i = cycleStart; i < edgePath.size(); i++) {
                    builder
                            .append(typePath.get(i).getQualifiedName())
                            .append('.')
                            .append(edgePath.get(i).prop.getName())
                            .append(" -> ");
                }
                builder
                        .append(type.getQualifiedName())
                        .append('.')
                        .append(edge.prop.getName())
                        .append(" -> ")
                        .append(edge.targetType.getQualifiedName());
                throw exception(type, edge.ref, builder.toString());
            }
            edgePath.add(edge);
            detectCycles(edge.targetType, graph, completed, typePath, edgePath, pathIndexMap);
            edgePath.remove(edgePath.size() - 1);
        }
        typePath.remove(typePath.size() - 1);
        pathIndexMap.remove(type);
        completed.add(type);
    }

    private static DtoAstException exception(DtoType<?, ?> ownerType, DtoTypeRef<?, ?> ref, String message) {
        return new DtoAstException(ownerType.getDtoFile(), ref.getLine(), ref.getColumn(), message);
    }

    private static class Edge<T extends BaseType, P extends BaseProp> {

        final DtoProp<T, P> prop;

        final DtoTypeRef<T, P> ref;

        final DtoType<T, P> targetType;

        Edge(DtoProp<T, P> prop, DtoTypeRef<T, P> ref, DtoType<T, P> targetType) {
            this.prop = prop;
            this.ref = ref;
            this.targetType = targetType;
        }
    }

    private static class LinkContext<T extends BaseType, P extends BaseProp> {

        final DtoTypeResolver<T> resolver;

        final Map<String, DtoType<T, P>> typeMap = new LinkedHashMap<>();

        final Map<String, DtoTypeInfo<T>> typeInfoMap = new HashMap<>();

        final Map<String, DtoTypeInfo<T>> externalTypeInfoMap = new HashMap<>();

        LinkContext(DtoTypeResolver<T> resolver) {
            this.resolver = resolver;
        }

        DtoTypeInfo<T> resolveExternal(String qualifiedName) {
            DtoTypeInfo<T> typeInfo = externalTypeInfoMap.get(qualifiedName);
            if (typeInfo == null) {
                typeInfo = resolver.resolve(qualifiedName);
                if (typeInfo != null) {
                    externalTypeInfoMap.put(qualifiedName, typeInfo);
                }
            }
            return typeInfo;
        }
    }

}
