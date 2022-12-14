package org.babyfish.jimmer.client.generator.ts;

import org.babyfish.jimmer.client.meta.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

public abstract class CodeWriter {

    static final Map<Class<?>, String> SIMPLE_TYPE_NAMES;

    private static final Pattern SLASH_PATTERN = Pattern.compile("/");

    private final Context ctx;

    private final File file;

    private final StringBuilder codeBuilder = new StringBuilder();

    private final Map<String, Set<String>> importMap = new HashMap<>();

    private int indent;

    private boolean lineDirty;

    private Scope scope;

    protected CodeWriter(Context ctx, File file) {
        this.ctx = ctx;
        this.file = file;
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
        prepareAdd();
        int size = ts.length();
        for (int i = 0; i < size; i++) {
            doAdd(ts.charAt(i));
        }
        return this;
    }

    public CodeWriter code(char c) {
        prepareAdd();
        doAdd(c);
        return this;
    }

    public void importFile(File file) {
        if (file != null && !file.getDir().equals(this.file.getDir())) {
            String[] currentPaths = SLASH_PATTERN.split(this.file.getDir());
            String[] paths = SLASH_PATTERN.split(file.getDir());
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
                builder.append("./");
            }
            for (int i = sameCount; i < paths.length; i++) {
                builder.append('/').append(paths[i]);
            }
            String path = builder.toString();
            importMap
                    .computeIfAbsent(path, it -> new LinkedHashSet<>())
                    .add(file.getName());
        }
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
            File file = ctx.file(type);
            if (file != null) {
                importFile(file);
                if (type instanceof ImmutableObjectType &&
                        rawImmutableAsDynamic() &&
                        ((ImmutableObjectType)type).getCategory() == ImmutableObjectType.Category.RAW) {
                    importFile(DynamicWriter.FILE);
                    code("Dynamic<").code(file.getName()).code('>');
                } else {
                    code(file.getName());
                }
            } else if (type instanceof StaticObjectType) {
                code(ctx.typeName(type));
                List<Type> typeArguments = ((StaticObjectType) type).getTypeArguments();
                if (!typeArguments.isEmpty()) {
                    scope(ScopeType.GENERIC, ", ", false, () -> {
                        for (Type typeArgument : typeArguments) {
                            separator();
                            type(typeArgument);
                        }
                    });
                }
            } else if (type instanceof ImmutableObjectType) {
                ImmutableObjectType immutableObjectType = (ImmutableObjectType) type;
                scope(ScopeType.OBJECT, ", ", immutableObjectType.getProperties().size() > 1, () -> {
                    for (Property property : immutableObjectType.getProperties().values()) {
                        separator();
                        code("readonly ")
                                .code(property.getName())
                                .codeIf(property.getType() instanceof NullableType, "?")
                                .code(": ");
                        type(NullableType.unwrap(property.getType()));
                    }
                });
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

    private void prepareAdd() {
        if (!lineDirty) {
            for (int i = indent; i > 0; --i) {
                codeBuilder.append(ctx.getIndent());
            }
            lineDirty = true;
        }
        if (scope != null) {
            scope.dirty();
        }
    }

    private void doAdd(char c) {
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
        writer.write("// ----");
        writer.write(getFile().toString());
        writer.write("----\n");
        write();
        if (!importMap.isEmpty()) {
            for (Map.Entry<String, Set<String>> e : importMap.entrySet()) {
                writer.write("import type { ");
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
        writer.write(codeBuilder.toString());
        writer.flush();
    }

    protected boolean rawImmutableAsDynamic() {
        return false;
    }

    enum ScopeType {
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
        map.put(Date.class, "Date");
        map.put(java.sql.Date.class, "Date");
        map.put(java.sql.Time.class, "Date");
        map.put(java.sql.Timestamp.class, "Date");
        map.put(LocalDate.class, "Date");
        map.put(LocalTime.class, "Date");
        map.put(LocalDateTime.class, "Date");
        map.put(OffsetDateTime.class, "Date");
        map.put(ZonedDateTime.class, "Date");
        SIMPLE_TYPE_NAMES = map;
    }
}
