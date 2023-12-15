package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.CodeWriter;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.Writer;
import java.util.*;

public class TypeScriptWriter extends CodeWriter {

    private final NavigableMap<String, NavigableSet<String>> imports = new TreeMap<>();

    private final boolean isMutable;

    public TypeScriptWriter(Context ctx, Source source) {
        super(ctx, source);
        this.isMutable = this.<TypeScriptContext>getContext().isMutable();
    }

    @Override
    public CodeWriter typeRef(Type type) {
        if (type instanceof TypeVariable) {
            code(((TypeVariable)type).getName());
        } else if (type instanceof NullableType) {
            typeRef(((NullableType)type).getTargetType()).code(" | null | undefined");
        } else if (type instanceof ListType) {
            code(isMutable ? "Array<" : "ReadonlyArray<").typeRef(((ListType)type).getElementType()).code('>');
        } else if (type instanceof MapType) {
            code(isMutable ? "{[key:string]: " : "{readonly [key:string]: ").typeRef(((MapType)type).getValueType()).code('}');
        } else if (type instanceof EnumType) {
            code(getResource(type).getName());
        } else if (type instanceof SimpleType) {
            Class<?> javaType = ((SimpleType)type).getJavaType();
            if (javaType == boolean.class) {
                code("boolean");
            } else if (javaType == char.class) {
                code("string");
            } else if (javaType.isPrimitive()) {
                code("number");
            } else {
                code("string");
            }
        } else if (type instanceof ObjectType) {
            code(getResource(type).getName());
            List<Type> arguments = ((ObjectType) type).getArguments();
            if (!arguments.isEmpty()) {
                scope(ScopeType.GENERIC, ", ", false, () -> {
                    for (Type argument : arguments) {
                        typeRef(argument);
                    }
                });
            }
        }
        return this;
    }

    @Override
    protected void onMarkImportedType(Type type, Source source) {
        List<String> currentDirs = this.source.getDirs();
        List<String> dirs = source.getDirs();
        int maxCount = Math.min(currentDirs.size(), dirs.size());
        int sameCount = 0;
        while (sameCount < maxCount) {
            if (!currentDirs.get(sameCount).equals(dirs.get(sameCount))) {
                break;
            }
            sameCount++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = currentDirs.size() - sameCount; i > 0; --i) {
            builder.append("../");
        }
        for (String dir : dirs.subList(sameCount, dirs.size())) {
            builder.append(dir).append('/');
        }
        String path = builder.toString();
        if (!path.startsWith("../")) {
            path = "./" + path;
        }
        imports.computeIfAbsent(path, it -> new TreeSet<>()).add(source.getName());
    }

    @Override
    protected void onFlushImportedTypes() {
        for (Map.Entry<String, NavigableSet<String>> e : imports.entrySet()) {
            code("import ");
            scope(ScopeType.OBJECT, ", ", e.getValue().size() > 3, () -> {
                for (String name : e.getValue()) {
                    separator();
                    code(name);
                }
            });
            code(" from '");
            code(e.getKey());
            code("';\n");
        }
    }
}
