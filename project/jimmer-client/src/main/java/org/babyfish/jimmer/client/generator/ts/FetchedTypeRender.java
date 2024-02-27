package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.runtime.impl.FetchedTypeImpl;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

public class FetchedTypeRender implements Render {

    private final String dtoName;

    private final String name;

    private final ObjectType objectType;

    private final String fetcherPrefix;

    private final LinkedList<String> paths = new LinkedList<>();

    final Map<Type, String> recursiveTypeNames;

    public FetchedTypeRender(String dtoName, String name, ObjectType objectType, Map<Type, String> recursiveTypeNames) {
        FetchByInfo fetchByInfo = objectType.getFetchByInfo();
        assert fetchByInfo != null;
        this.dtoName = dtoName;
        this.name = name;
        this.objectType = objectType;
        this.recursiveTypeNames = recursiveTypeNames;
        this.fetcherPrefix = fetchByInfo.getOwnerType().getSimpleName() + '/' + fetchByInfo.getConstant();
        collectRecursiveTypeNames(objectType);
    }

    private void collectRecursiveTypeNames(ObjectType type) {
        if (type.isRecursiveFetchedType()) {
            if (recursiveTypeNames.containsKey(type)) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(fetcherPrefix);
            ListIterator<String> itr = paths.listIterator(paths.size());
            while (itr.hasPrevious()) {
                builder.append('@').append(itr.previous());
            }
            recursiveTypeNames.put(type, builder.toString());
        }
        for (Property property : type.getProperties().values()) {
            Type targetType = property.getType();
            boolean isNullable = targetType instanceof NullableType;
            if (isNullable) {
                targetType = ((NullableType)targetType).getTargetType();
            }
            boolean isList = targetType instanceof ListType;
            if (isList) {
                targetType = ((ListType)targetType).getElementType();
            }
            if (targetType instanceof ObjectType) {
                ObjectType targetObjectType = (ObjectType) targetType;
                if (targetObjectType.isRecursiveFetchedType() && !objectType.hasMultipleRecursiveProps()) {
                    collectRecursiveTypeNames((ObjectType) targetType);
                } else {
                    paths.push(property.getName());
                    collectRecursiveTypeNames((ObjectType) targetType);
                    paths.pop();
                }
            }
        }
    }

    @Override
    public void render(SourceWriter writer) {
        assert objectType.getFetchByInfo() != null;
        Doc doc = objectType.getFetchByInfo().getDoc();
        if (doc == null) {
            doc = objectType.getDoc();
        }
        writer.doc(doc);
        writer.code('\'').code(name).code("': ");
        render(dtoName, objectType, writer, recursiveTypeNames);
    }

    static void render(String dtoName, ObjectType type, SourceWriter writer, Map<Type, String> recursiveTypeNames) {
        TypeScriptContext ctx = writer.getContext();
        writer.scope(SourceWriter.ScopeType.OBJECT, "", true, () -> {
            for (Property property : type.getProperties().values()) {
                writer.separator();
                DocUtils.doc(property, type.getDoc(), writer);
                writer
                        .codeIf(!ctx.isMutable(), "readonly ")
                        .code(property.getName());
                Type targetType = property.getType();
                boolean isNullable = targetType instanceof NullableType;
                if (isNullable) {
                    targetType = ((NullableType)targetType).getTargetType();
                }
                boolean isList = targetType instanceof ListType;
                if (isList) {
                    targetType = ((ListType)targetType).getElementType();
                }
                String recursiveTypeName = recursiveTypeNames.get(targetType);
                writer.codeIf(property.getType() instanceof NullableType || recursiveTypeName != null, '?')
                        .code(": ");
                if (targetType instanceof FetchedTypeImpl) {
                    ObjectType targetObjectType = (ObjectType) targetType;
                    writeResolvedType(writer, isNullable, isList, () -> {
                        if (recursiveTypeName != null) {
                            writer.code(dtoName).code("['").code(recursiveTypeName).code("']");
                        } else {
                            render(dtoName, targetObjectType, writer, recursiveTypeNames);
                        }
                    });
                } else {
                    writer.typeRef(property.getType());
                }
                writer.codeIf(
                        recursiveTypeName != null && !(property.getType() instanceof NullableType),
                        ctx.getNullRenderMode() == NullRenderMode.NULL_OR_UNDEFINED ?
                                " | null | undefined" :
                                " | undefined"
                );
                writer.code(';');
            }
        });
    }

    private static void writeResolvedType(SourceWriter writer, boolean isNullable, boolean isList, Runnable block) {
        TypeScriptContext ctx = writer.getContext();
        if (isList) {
            writer.code(ctx.isMutable() ? "Array<" : "ReadonlyArray<");
        }
        block.run();
        if (isList) {
            writer.code('>');
        }
        if (isNullable) {
            writer.code(
                    ctx.getNullRenderMode() == NullRenderMode.NULL_OR_UNDEFINED ?
                            " | null | undefined" :
                            " | undefined"
                    );
        }
    }
}
