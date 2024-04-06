package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class TypeScriptWriter extends SourceWriter {

    private final NavigableMap<String, Import> imports = new TreeMap<>();

    private final boolean isMutable;

    private final NullRenderMode nullRenderMode;

    public TypeScriptWriter(Context ctx, Source source) {
        super(ctx, source);
        this.isMutable = this.<TypeScriptContext>getContext().isMutable();
        this.nullRenderMode = this.<TypeScriptContext>getContext().getNullRenderMode();
    }

    @Override
    public SourceWriter typeRef(Type type) {
        if (type instanceof TypeVariable) {
            code(((TypeVariable)type).getName());
        } else if (type instanceof NullableType) {
            typeRef(((NullableType) type).getTargetType());
            if (this.nullRenderMode == NullRenderMode.NULL_OR_UNDEFINED) {
                code(" | null | undefined");
            } else {
                code(" | undefined");
            }
        } else if (type instanceof ListType) {
            code(isMutable ? "Array<" : "ReadonlyArray<").typeRef(((ListType)type).getElementType()).code('>');
        } else if (type instanceof MapType) {
            code(isMutable ? "{[key:string]: " : "{readonly [key:string]: ").typeRef(((MapType)type).getValueType()).code('}');
        } else if (type instanceof EnumType) {
            code(fullName(getSource(type)));
        } else if (type instanceof VirtualType) {
            if (type instanceof VirtualType.File) {
                code("File");
            } else {
                throw new AssertionError("Internal bug: more virtual type need to be processed");
            }
        } else if (type instanceof SimpleType) {
            Class<?> javaType = ((SimpleType)type).getJavaType();
            if (javaType == Object.class) {
                code("any");
            } else if (javaType == boolean.class) {
                code("boolean");
            } else if (javaType == char.class) {
                code("string");
            } else if (javaType.isPrimitive() || javaType == BigInteger.class || javaType == BigDecimal.class) {
                code("number");
            } else {
                code("string");
            }
        } else if (type instanceof ObjectType) {
            code(fullName(getSource(type)));
            ObjectType objectType = (ObjectType) type;
            if (objectType.getKind() == ObjectType.Kind.STATIC) {
                List<Type> arguments = ((ObjectType) type).getArguments();
                if (!arguments.isEmpty()) {
                    scope(ScopeType.GENERIC, ", ", false, () -> {
                        for (Type argument : arguments) {
                            separator();
                            typeRef(argument);
                        }
                    });
                }
            }
        }
        return this;
    }

    @Override
    public void importSource(Source source, String name, boolean treatAsValue) {
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
        Import imp = imports.computeIfAbsent(path, it -> new Import());
        if (treatAsValue) {
            imp.importedValues.add(name);
        } else {
            imp.importedTypes.add(name);
        }
    }

    @Override
    protected void onFlushImportedTypes() {
        for (Map.Entry<String, Import> e : imports.entrySet()) {
            Set<String> types = e.getValue().importedTypes;
            Set<String> values = e.getValue().importedValues;
            if (!types.isEmpty()) {
                code("import type ");
                scope(ScopeType.OBJECT, ", ", types.size() > 3, () -> {
                    for (String name : types) {
                        separator();
                        code(name);
                    }
                });
                code(" from '");
                code(e.getKey());
                code("';\n");
            }
            if (!values.isEmpty()) {
                code("import ");
                scope(ScopeType.OBJECT, ", ", values.size() > 3, () -> {
                    for (String name : values) {
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

    private static String fullName(Source source) {
        if (source.getParent() == null) {
            return source.getName();
        }
        return fullName(source.getParent()) + "['" + source.getName() + "']";
    }

    private static class Import {

        final NavigableSet<String> importedTypes = new TreeSet<>();

        final NavigableSet<String> importedValues = new TreeSet<>();
    }
}
