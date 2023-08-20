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
                        path + '.' + importedType.name.getText(),
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
                    if (expectedArgumentCount != null) {
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
        String pkg = this.ctx.getBaseType().getPackageName();
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
        standardTypes.put(TypeRef.TN_BOOLEAN, 0);
        standardTypes.put(TypeRef.TN_CHAR, 0);
        standardTypes.put(TypeRef.TN_BYTE, 0);
        standardTypes.put(TypeRef.TN_SHORT, 0);
        standardTypes.put(TypeRef.TN_INT, 0);
        standardTypes.put(TypeRef.TN_LONG, 0);
        standardTypes.put(TypeRef.TN_FLOAT, 0);
        standardTypes.put(TypeRef.TN_DOUBLE, 0);
        standardTypes.put(TypeRef.TN_STRING, 0);
        standardTypes.put(TypeRef.TN_ARRAY, 1);
        standardTypes.put(TypeRef.TN_ITERABLE, 1);
        standardTypes.put(TypeRef.TN_MUTABLE_ITERABLE, 1);
        standardTypes.put(TypeRef.TN_COLLECTION, 1);
        standardTypes.put(TypeRef.TN_MUTABLE_COLLECTION, 1);
        standardTypes.put(TypeRef.TN_LIST, 1);
        standardTypes.put(TypeRef.TN_MUTABLE_LIST, 1);
        standardTypes.put(TypeRef.TN_SET, 1);
        standardTypes.put(TypeRef.TN_MUTABLE_SET, 1);
        standardTypes.put(TypeRef.TN_MAP, 2);
        standardTypes.put(TypeRef.TN_MUTABLE_MAP, 2);
        STANDARD_TYPES = standardTypes;

        Map<String, String> illegalTypes = new HashMap<>();
        illegalTypes.put("boolean", TypeRef.TN_BOOLEAN);
        illegalTypes.put(Boolean.class.getName(), TypeRef.TN_BOOLEAN + '?');
        illegalTypes.put("kotlin.Boolean", TypeRef.TN_BOOLEAN);
        illegalTypes.put("char", TypeRef.TN_CHAR);
        illegalTypes.put(Character.class.getName(), TypeRef.TN_CHAR + '?');
        illegalTypes.put(Character.class.getSimpleName(), TypeRef.TN_CHAR + '?');
        illegalTypes.put("kotlin.Char", TypeRef.TN_CHAR);
        illegalTypes.put("byte", TypeRef.TN_BYTE);
        illegalTypes.put(Byte.class.getName(), TypeRef.TN_BYTE + '?');
        illegalTypes.put("kotlin.Byte", TypeRef.TN_BYTE);
        illegalTypes.put("short", TypeRef.TN_SHORT);
        illegalTypes.put(Short.class.getName(), TypeRef.TN_SHORT + '?');
        illegalTypes.put("kotlin.Short", TypeRef.TN_SHORT);
        illegalTypes.put("int", TypeRef.TN_INT);
        illegalTypes.put(Integer.class.getName(), TypeRef.TN_INT + '?');
        illegalTypes.put(Integer.class.getSimpleName(), TypeRef.TN_INT + '?');
        illegalTypes.put("kotlin.Int", TypeRef.TN_INT);
        illegalTypes.put("long", TypeRef.TN_LONG);
        illegalTypes.put(Long.class.getName(), TypeRef.TN_LONG + '?');
        illegalTypes.put("kotlin.Long", TypeRef.TN_LONG);
        illegalTypes.put("float", TypeRef.TN_FLOAT);
        illegalTypes.put(Float.class.getName(), TypeRef.TN_FLOAT + '?');
        illegalTypes.put("kotlin.Float", TypeRef.TN_FLOAT);
        illegalTypes.put("double", TypeRef.TN_DOUBLE);
        illegalTypes.put(Double.class.getName(), TypeRef.TN_DOUBLE + '?');
        illegalTypes.put("kotlin.Double", TypeRef.TN_DOUBLE);
        illegalTypes.put("string", TypeRef.TN_STRING);
        illegalTypes.put(String.class.getName(), TypeRef.TN_STRING);
        illegalTypes.put("kotlin.String", TypeRef.TN_STRING);
        illegalTypes.put("kotlin.Array", TypeRef.TN_ARRAY);
        illegalTypes.put("kotlin.BooleanArray", TypeRef.TN_ARRAY + "<Boolean>");
        illegalTypes.put("kotlin.CharArray", TypeRef.TN_ARRAY + "<Char>");
        illegalTypes.put("kotlin.ByteArray", TypeRef.TN_ARRAY + "<Byte>");
        illegalTypes.put("kotlin.ShortArray", TypeRef.TN_ARRAY + "<Short>");
        illegalTypes.put("kotlin.IntArray", TypeRef.TN_ARRAY + "<Int>");
        illegalTypes.put("kotlin.LongArray", TypeRef.TN_ARRAY + "<Long>");
        illegalTypes.put("kotlin.FloatArray", TypeRef.TN_ARRAY + "<Float>");
        illegalTypes.put("kotlin.DoubleArray", TypeRef.TN_ARRAY + "<Double>");
        illegalTypes.put(Iterable.class.getName(), TypeRef.TN_ITERABLE + '/' + TypeRef.TN_MUTABLE_ITERABLE);
        illegalTypes.put("kotlin.collections.Iterable", TypeRef.TN_ITERABLE);
        illegalTypes.put("kotlin.collections.MutableIterable", TypeRef.TN_MUTABLE_ITERABLE);
        illegalTypes.put(Collection.class.getName(), TypeRef.TN_COLLECTION + '/' + TypeRef.TN_MUTABLE_COLLECTION);
        illegalTypes.put("kotlin.collections.Collection", TypeRef.TN_COLLECTION);
        illegalTypes.put("kotlin.collections.MutableCollection", TypeRef.TN_MUTABLE_COLLECTION);
        illegalTypes.put(List.class.getName(), TypeRef.TN_LIST + '/' + TypeRef.TN_MUTABLE_LIST);
        illegalTypes.put("kotlin.collections.List", TypeRef.TN_LIST);
        illegalTypes.put("kotlin.collections.MutableList", TypeRef.TN_MUTABLE_LIST);
        illegalTypes.put(Set.class.getName(), TypeRef.TN_SET + '/' + TypeRef.TN_MUTABLE_SET);
        illegalTypes.put("kotlin.collections.Set", TypeRef.TN_SET);
        illegalTypes.put("kotlin.collections.MutableSet", TypeRef.TN_MUTABLE_SET);
        illegalTypes.put(Map.class.getName(), TypeRef.TN_MAP + '/' + TypeRef.TN_MUTABLE_MAP);
        illegalTypes.put("kotlin.collections.Map", TypeRef.TN_MAP);
        illegalTypes.put("kotlin.collections.MutableMap", TypeRef.TN_MUTABLE_MAP);
        ILLEGAL_TYPES = illegalTypes;
    }
}
