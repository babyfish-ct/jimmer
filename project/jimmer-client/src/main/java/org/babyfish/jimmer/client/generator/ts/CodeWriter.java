package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.generator.ts.simple.DynamicWriter;
import org.babyfish.jimmer.client.meta.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

public abstract class CodeWriter {

    static final Map<Class<?>, String> SIMPLE_TYPE_NAMES;

    private static final String[] EMPTY_ARR = new String[0];

    private static final Pattern SLASH_PATTERN = Pattern.compile("/");

    private final Context ctx;

    private final File file;

    protected final boolean anonymous;

    private final StringBuilder codeBuilder = new StringBuilder();

    private final Map<String, Set<String>> importMap = new HashMap<>();

    private final Map<String, Set<String>> importDataMap = new HashMap<>();

    private int indent;

    private boolean lineDirty;

    private Scope scope;

    protected CodeWriter(Context ctx, File file) {
        this.ctx = ctx;
        this.file = file;
        this.anonymous = false;
    }

    protected CodeWriter(Context ctx, File file, boolean anonymous) {
        this.ctx = ctx;
        this.file = file;
        this.anonymous = anonymous;
    }

    public Context getContext() {
        return ctx;
    }

    public File getFile() {
        return file;
    }

    public CodeWriter codeIf(boolean cond, String ts) {
        if (cond) {
            return code(ts);
        }
        return this;
    }

    public CodeWriter codeIf(boolean cond, char c) {
        if (cond) {
            return code(c);
        }
        return this;
    }

    public CodeWriter code(String ts) {
        if (ts.isEmpty()) {
            return this;
        }
        int size = ts.length();
        for (int i = 0; i < size; i++) {
            doAdd(ts.charAt(i));
        }
        return this;
    }

    public CodeWriter code(char c) {
        doAdd(c);
        return this;
    }

    public CodeWriter importFile(File file) {
        return importFile(file, false);
    }

    public CodeWriter importFile(File file, boolean treatAsData) {

        if (file != null && !file.equals(this.file)) {
            String[] currentPaths =
                    this.file.getDir().isEmpty() ?
                            EMPTY_ARR :
                            SLASH_PATTERN.split(this.file.getDir());
            String[] paths =
                    file.getDir().isEmpty() ?
                            EMPTY_ARR :
                            SLASH_PATTERN.split(file.getDir());
            int sameCount = 0;
            int len = Math.min(currentPaths.length, paths.length);
            while (sameCount < len) {
                if (!currentPaths[sameCount].equals(paths[sameCount])) {
                    break;
                }
                sameCount++;
            }
            StringBuilder builder = new StringBuilder();
            if (sameCount < currentPaths.length) {
                for (int i = currentPaths.length - sameCount; i > 0; --i) {
                    builder.append("..");
                    if (i != 1) {
                        builder.append('/');
                    }
                }
            } else {
                builder.append(".");
            }
            if (sameCount == paths.length) {
                builder.append('/');
            }
            for (int i = sameCount; i < paths.length; i++) {
                builder.append('/').append(paths[i]);
            }
            String path = builder.toString();
            (treatAsData ? importDataMap : importMap)
                    .computeIfAbsent(path, it -> new LinkedHashSet<>())
                    .add(file.getName());
        }
        return this;
    }

    public CodeWriter type(Type type) {
        if (type instanceof UnresolvedTypeVariable) {
            code(((UnresolvedTypeVariable)type).getName());
        } else if (type instanceof SimpleType) {
            String name = SIMPLE_TYPE_NAMES.get(((SimpleType)type).getJavaType());
            if (name == null) {
                name = "any";
            }
            code(name);
        } else if (type instanceof NullableType) {
            type(((NullableType)type).getTargetType());
            code(" | undefined");
        } else if (type instanceof ArrayType) {
            code("ReadonlyArray<");
            type(((ArrayType)type).getElementType());
            code(">");
        } else if (type instanceof MapType) {
            code("ReadonlyMap<");
            type(((MapType)type).getKeyType());
            code(", ");
            type(((MapType)type).getValueType());
            code(">");
        } else {
            File file = ctx.getFile(type);
            if (file != null) {
                importFile(file);
                if (type instanceof ImmutableObjectType &&
                        rawImmutableAsDynamic() &&
                        ((ImmutableObjectType)type).getCategory() == ImmutableObjectType.Category.RAW) {
                    importFile(DynamicWriter.FILE);
                    code("Dynamic<").code(file.getName()).code('>');
                } else {
                    code(file.getName());
                    if (type instanceof StaticObjectType) {
                        List<Type> typeArguments = ((StaticObjectType) type).getTypeArguments();
                        if (!typeArguments.isEmpty()) {
                            scope(ScopeType.GENERIC, ", ", false, () -> {
                                for (Type typeArgument : typeArguments) {
                                    separator();
                                    type(typeArgument);
                                }
                            });
                        }
                    }
                }
            } else if (type instanceof ImmutableObjectType) {
                ImmutableObjectType immutableObjectType = (ImmutableObjectType) type;
                if (!anonymous && immutableObjectType.getFetchByInfo() != null) {
                    importFile(DtoWriter.dtoFile(ctx, immutableObjectType.getJavaType()));
                    code(ctx.getDtoPrefix(immutableObjectType.getJavaType()))
                            .code("['")
                            .code(ctx.getDtoSuffix(immutableObjectType))
                            .code("']");
                } else if (!anonymous && immutableObjectType.getCategory() == ImmutableObjectType.Category.VIEW) {
                    importFile(DtoWriter.dtoFile(ctx, immutableObjectType.getJavaType()));
                    code(ctx.getDtoPrefix(immutableObjectType.getJavaType()))
                            .code("['DEFAULT']");
                } else {
                    scope(ScopeType.OBJECT, ", ", immutableObjectType.getProperties().size() > 1, () -> {
                        for (Property property : immutableObjectType.getProperties().values()) {
                            separator();
                            if (property.getDocument() != null) {
                                code('\n');
                                document(property.getDocument());
                            }
                            code("readonly ")
                                    .code(property.getName())
                                    .codeIf(property.getType() instanceof NullableType, "?")
                                    .code(": ");
                            type(NullableType.unwrap(property.getType()));
                        }
                    });
                }
            }
        }
        return this;
    }

    public CodeWriter separator() {
        Scope scope = this.scope;
        if (scope == null) {
            throw new IllegalStateException("There is no existing scope");
        }
        if (scope.dirty) {
            code(scope.separator);
            if (scope.multiLines) {
                code('\n');
            }
        }
        return this;
    }

    public CodeWriter document(Document document) {
        if (document == null) {
            return null;
        }
        boolean visitedFirstLine = false;
        code("/**\n");
        for (Document.Item item : document.getItems()) {
            code(" * ");
            if (item.getDepth() != 0) {
                for (int i = item.getDepth(); i > 1; --i) {
                    code(ctx.getIndent());
                }
                code('-').code(ctx.getIndent().substring(1));
            } else {
                if (visitedFirstLine) {
                    code("\n * ");
                }
            }
            code(item.getText());
            code('\n');
            visitedFirstLine = true;
        }
        code(" */\n");
        return this;
    }

    private void doAdd(char c) {
        if (!lineDirty) {
            for (int i = indent; i > 0; --i) {
                codeBuilder.append(ctx.getIndent());
            }
            lineDirty = true;
        }
        if (scope != null) {
            scope.dirty();
        }
        codeBuilder.append(c);
        if (c == '\n') {
            lineDirty = false;
        }
    }

    public CodeWriter scope(ScopeType type, String separator, boolean multiLines, Runnable runnable) {
        Scope oldScope = scope;
        Scope newScope = new Scope(oldScope, separator, multiLines);

        code(type.prefix);
        if (multiLines) {
            code('\n');
            indent++;
        }

        scope = newScope;

        runnable.run();
        if (multiLines) {
            --indent;
            if (lineDirty) {
                code('\n');
            }
        }
        code(type.suffix);

        scope = oldScope;
        return this;
    }

    protected abstract void write();

    public void flush() throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(ctx.getOutputStream());
        write();

        applyImportMap(importMap, false, writer);
        applyImportMap(importDataMap, true, writer);

        writer.write(codeBuilder.toString());
        writer.flush();
    }

    private static void applyImportMap(Map<String, Set<String>> importMap, boolean treatAsData, Writer writer) throws IOException {
        if (!importMap.isEmpty()) {
            for (Map.Entry<String, Set<String>> e : importMap.entrySet()) {
                writer.write(treatAsData ? "import { " : "import type { ");
                boolean addComma = false;
                for (String name : e.getValue()) {
                    if (addComma) {
                        writer.write(", ");
                    } else {
                        addComma = true;
                    }
                    writer.write(name);
                }
                writer.write(" } from '");
                writer.write(e.getKey());
                writer.write("';\n");
            }
            writer.write('\n');
        }
    }

    protected boolean rawImmutableAsDynamic() {
        return false;
    }

    public enum ScopeType {
        OBJECT("{", "}"),
        LIST("[", "]"),
        ARGUMENTS("(", ")"),
        GENERIC("<", ">"),
        BLANK("", "");

        final String prefix;

        final String suffix;

        ScopeType(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    private static class Scope {

        private final Scope parent;

        final String separator;

        final boolean multiLines;

        boolean dirty;

        Scope(Scope parent, String separator, boolean multiLines) {
            this.parent = parent;
            this.separator = separator;
            this.multiLines = multiLines;
        }

        void dirty() {
            if (!dirty) {
                dirty = true;
                if (parent != null) {
                    parent.dirty();
                }
            }
        }
    }

    static {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(void.class, "void");
        map.put(boolean.class, "boolean");
        map.put(Boolean.class, "boolean");
        map.put(char.class, "number");
        map.put(Character.class, "number");
        map.put(byte.class, "number");
        map.put(Byte.class, "number");
        map.put(short.class, "number");
        map.put(Short.class, "number");
        map.put(int.class, "number");
        map.put(Integer.class, "number");
        map.put(long.class, "number");
        map.put(Long.class, "number");
        map.put(float.class, "number");
        map.put(Float.class, "number");
        map.put(double.class, "number");
        map.put(Double.class, "number");
        map.put(BigInteger.class, "number");
        map.put(BigDecimal.class, "number");
        map.put(String.class, "string");
        map.put(UUID.class, "string");
        map.put(Date.class, "string");
        map.put(java.sql.Date.class, "string");
        map.put(java.sql.Time.class, "string");
        map.put(java.sql.Timestamp.class, "string");
        map.put(LocalDate.class, "string");
        map.put(LocalTime.class, "string");
        map.put(LocalDateTime.class, "string");
        map.put(OffsetDateTime.class, "string");
        map.put(ZonedDateTime.class, "string");
        SIMPLE_TYPE_NAMES = map;
    }
}
