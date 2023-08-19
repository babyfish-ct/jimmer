package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;

import java.util.*;
import java.util.stream.Collectors;

class Importing {

    private static final Set<String> AUTO_IMPORTED_TYPES;

    private static final Map<String, Integer> STANDARD_TYPES;

    private static final Map<String, String> ILLEGAL_TYPES;

    private final CompilerContext<?, ?> ctx;

    private final Map<String, String> typeMap = new HashMap<>();

    Importing(CompilerContext<?, ?> ctx) {
        this.ctx = ctx;
    }

    public void add(DtoParser.ImportStatementContext ctx) {
        String path = ctx.parts.stream().map(Token::getText).collect(Collectors.joining("."));
        if (ctx.alias != null) {
            add0(ctx.alias, path, ctx.parts.get(ctx.parts.size() - 1).getLine());
        } else if (!ctx.importedTypes.isEmpty()) {
            for (DtoParser.ImportedTypeContext importedType : ctx.importedTypes) {
                add0(
                        importedType.alias != null ? importedType.alias : importedType.name,
                        path + '.' + importedType.name,
                        importedType.name.getLine()
                );
            }
        } else {
            add0(ctx.parts.get(ctx.parts.size() - 1), path, ctx.parts.get(ctx.parts.size() - 1).getLine());
        }
    }

    private void add0(Token alias, String qualifiedName, int qualifiedNameLine) {
        if (AUTO_IMPORTED_TYPES.contains(qualifiedName)) {
            throw ctx.exception(
                    qualifiedNameLine,
                    "\"" +
                            qualifiedName +
                            "\" cannot be imported because it is built-in type"
            );
        }
        if (typeMap.put(alias.getText(), qualifiedName) != null) {
            throw ctx.exception(
                    alias.getLine(),
                    "Duplicated imported alias \"" +
                            alias.getText() +
                            "\""
            );
        }
    }

    public TypeRef resolve(DtoParser.TypeRefContext ctx) {
        String name = resolveName(ctx);
        Integer expectedArgumentCount = STANDARD_TYPES.get(name);
        if (expectedArgumentCount != null && expectedArgumentCount != ctx.genericArguments.size()) {
            throw this.ctx.exception(
                    ctx.qualifiedName().stop.getLine(),
                    "Illegal type \"" +
                            ctx.getText() +
                            "\", the expected generic argument count is " +
                            expectedArgumentCount +
                            ", but the actual generic argument count is " +
                            ctx.genericArguments.size()
            );
        }
        List<TypeRef.Argument> arguments = null;
        if (!ctx.genericArguments.isEmpty()) {
            arguments = new ArrayList<>(ctx.genericArguments.size());
            for (DtoParser.GenericArgumentContext arg : ctx.genericArguments) {
                boolean in = false;
                boolean out = false;
                if (arg.modifier != null) {
                    switch (arg.modifier.getText()) {
                        case "in":
                            in = true;
                            break;
                        case "out":
                            out = true;
                            break;
                        default:
                            throw this.ctx.exception(
                                    arg.modifier.getLine(),
                                    "The generic argument modifier is neither \"in\" nor \"out\""
                            );
                    }
                    if (expectedArgumentCount != null && !name.equals("Array")) {
                        throw this.ctx.exception(
                                arg.modifier.getLine(),
                                "The modifier \"" +
                                        arg.modifier.getText() +
                                        "\" of the generic argument of standard collection cannot be specified"
                        );
                    }
                }
                arguments.add(
                        new TypeRef.Argument(
                               resolve(arg.typeRef()),
                               in,
                               out
                        )
                );
            }
        }
        return new TypeRef(
                name,
                arguments,
                ctx.optional != null
        );
    }

    private String resolveName(DtoParser.TypeRefContext ctx) {
        String qualifiedName = ctx.qualifiedName().parts.stream().map(Token::getText).collect(Collectors.joining("."));
        if (STANDARD_TYPES.containsKey(qualifiedName)) {
            return qualifiedName;
        }
        String suggested = ILLEGAL_TYPES.get(qualifiedName);
        if (suggested != null) {
            throw this.ctx.exception(
                    ctx.qualifiedName().stop.getLine(),
                    "Illegal type \"" +
                            qualifiedName +
                            "\", please use \"" +
                            suggested +
                            "\""
            );
        }
        String imported;
        int index = qualifiedName.indexOf('.');
        if (index == -1) {
            imported = typeMap.get(qualifiedName);
        } else {
            imported = typeMap.get(qualifiedName.substring(0, index));
            if (imported != null) {
                imported += qualifiedName.substring(index);
            }
        }
        if (imported != null) {
            return imported;
        }
        String pkg = this.ctx.getBaseType().getQualifiedName();
        if (pkg.isEmpty()) {
            return qualifiedName;
        }
        return pkg + '.' + qualifiedName;
    }

    static {
        Set<String> autoImportedTypes = new HashSet<>();
        autoImportedTypes.add(void.class.getName());
        autoImportedTypes.add(boolean.class.getName());
        autoImportedTypes.add(char.class.getName());
        autoImportedTypes.add(byte.class.getName());
        autoImportedTypes.add(short.class.getName());
        autoImportedTypes.add(int.class.getName());
        autoImportedTypes.add(long.class.getName());
        autoImportedTypes.add(float.class.getName());
        autoImportedTypes.add(double.class.getName());
        autoImportedTypes.add(Boolean.class.getName());
        autoImportedTypes.add(Character.class.getName());
        autoImportedTypes.add(Void.class.getName());
        autoImportedTypes.add(Byte.class.getName());
        autoImportedTypes.add(Short.class.getName());
        autoImportedTypes.add(Integer.class.getName());
        autoImportedTypes.add(Long.class.getName());
        autoImportedTypes.add(Float.class.getName());
        autoImportedTypes.add(Double.class.getName());
        autoImportedTypes.add(String.class.getName());
        autoImportedTypes.add(Iterable.class.getName());
        autoImportedTypes.add(Collection.class.getName());
        autoImportedTypes.add(List.class.getName());
        autoImportedTypes.add(Set.class.getName());
        autoImportedTypes.add(Map.class.getName());
        autoImportedTypes.add("kotlin.Unit");
        autoImportedTypes.add("kotlin.Boolean");
        autoImportedTypes.add("kotlin.Char");
        autoImportedTypes.add("kotlin.Byte");
        autoImportedTypes.add("kotlin.Short");
        autoImportedTypes.add("kotlin.Int");
        autoImportedTypes.add("kotlin.Long");
        autoImportedTypes.add("kotlin.Float");
        autoImportedTypes.add("kotlin.Double");
        autoImportedTypes.add("kotlin.String");
        autoImportedTypes.add("kotlin.Array");
        autoImportedTypes.add("kotlin.BooleanArray");
        autoImportedTypes.add("kotlin.CharArray");
        autoImportedTypes.add("kotlin.ByteArray");
        autoImportedTypes.add("kotlin.ShortArray");
        autoImportedTypes.add("kotlin.IntArray");
        autoImportedTypes.add("kotlin.LongArray");
        autoImportedTypes.add("kotlin.FloatArray");
        autoImportedTypes.add("kotlin.DoubleArray");
        autoImportedTypes.add("kotlin.collections.Iterable");
        autoImportedTypes.add("kotlin.collections.Collection");
        autoImportedTypes.add("kotlin.collections.List");
        autoImportedTypes.add("kotlin.collections.Set");
        autoImportedTypes.add("kotlin.collections.Map");
        autoImportedTypes.add("kotlin.collections.MutableIterable");
        autoImportedTypes.add("kotlin.collections.MutableCollection");
        autoImportedTypes.add("kotlin.collections.MutableList");
        autoImportedTypes.add("kotlin.collections.MutableSet");
        autoImportedTypes.add("kotlin.collections.MutableMap");
        AUTO_IMPORTED_TYPES = autoImportedTypes;

        Map<String, Integer> standardTypes = new HashMap<>();
        standardTypes.put("Boolean", 0);
        standardTypes.put("Char", 0);
        standardTypes.put("Byte", 0);
        standardTypes.put("Short", 0);
        standardTypes.put("Int", 0);
        standardTypes.put("Long", 0);
        standardTypes.put("Float", 0);
        standardTypes.put("Double", 0);
        standardTypes.put("String", 0);
        standardTypes.put("Iterable", 1);
        standardTypes.put("Array", 1);
        standardTypes.put("Collection", 1);
        standardTypes.put("List", 1);
        standardTypes.put("Set", 1);
        standardTypes.put("Map", 2);
        standardTypes.put("MutableIterable", 1);
        standardTypes.put("MutableCollection", 1);
        standardTypes.put("MutableList", 1);
        standardTypes.put("MutableSet", 1);
        standardTypes.put("MutableMap", 2);
        STANDARD_TYPES = standardTypes;

        Map<String, String> illegalTypes = new HashMap<>();
        illegalTypes.put("boolean", "Boolean");
        illegalTypes.put(Boolean.class.getName(), "Boolean?");
        illegalTypes.put("kotlin.Boolean", "Boolean");
        illegalTypes.put("char", "Char");
        illegalTypes.put(Character.class.getName(), "Char?");
        illegalTypes.put(Character.class.getSimpleName(), "Char?");
        illegalTypes.put("kotlin.Char", "Char");
        illegalTypes.put("byte", "Byte");
        illegalTypes.put(Byte.class.getName(), "Byte?");
        illegalTypes.put("kotlin.Byte", "Byte");
        illegalTypes.put("short", "Short");
        illegalTypes.put(Short.class.getName(), "Short?");
        illegalTypes.put("kotlin.Short", "Short");
        illegalTypes.put("int", "Integer");
        illegalTypes.put(Integer.class.getName(), "Int?");
        illegalTypes.put(Integer.class.getSimpleName(), "Int?");
        illegalTypes.put("kotlin.Int", "Int");
        illegalTypes.put("long", "Long");
        illegalTypes.put(Long.class.getName(), "Long?");
        illegalTypes.put("kotlin.Long", "Long");
        illegalTypes.put("float", "Float");
        illegalTypes.put(Float.class.getName(), "Float?");
        illegalTypes.put("kotlin.Float", "Float");
        illegalTypes.put("double", "Double");
        illegalTypes.put(Double.class.getName(), "Double?");
        illegalTypes.put("kotlin.Double", "Double");
        illegalTypes.put("string", "String");
        illegalTypes.put(String.class.getName(), "String");
        illegalTypes.put("kotlin.String", "String");
        illegalTypes.put("kotlin.Array", "Array");
        illegalTypes.put("kotlin.BooleanArray", "Array<Boolean>");
        illegalTypes.put("kotlin.CharArray", "Array<Char>");
        illegalTypes.put("kotlin.ByteArray", "Array<Byte>");
        illegalTypes.put("kotlin.ShortArray", "Array<Short>");
        illegalTypes.put("kotlin.IntArray", "Array<Int>");
        illegalTypes.put("kotlin.LongArray", "Array<LongArray>");
        illegalTypes.put("kotlin.FloatArray", "Array<Float>");
        illegalTypes.put("kotlin.DoubleArray", "Array<Double>");
        illegalTypes.put(Iterable.class.getName(), "Iterable/MutableIterable");
        illegalTypes.put("kotlin.collections.Iterable", "Iterable");
        illegalTypes.put("kotlin.collections.MutableIterable", "MutableIterable");
        illegalTypes.put(Collection.class.getName(), "Collection/MutableCollection");
        illegalTypes.put("kotlin.collections.Collection", "Collection");
        illegalTypes.put("kotlin.collections.MutableCollection", "MutableCollection");
        illegalTypes.put(List.class.getName(), "List/MutableList");
        illegalTypes.put("kotlin.collections.List", "List");
        illegalTypes.put("kotlin.collections.MutableList", "MutableList");
        illegalTypes.put(Set.class.getName(), "Set/MutableSet");
        illegalTypes.put("kotlin.collections.Set", "Set");
        illegalTypes.put("kotlin.collections.MutableSet", "MutableSet");
        illegalTypes.put(Map.class.getName(), "Map/MutableMap");
        illegalTypes.put("kotlin.collections.Map", "Map");
        illegalTypes.put("kotlin.collections.MutableMap", "MutableMap");
        ILLEGAL_TYPES = illegalTypes;
    }
}
