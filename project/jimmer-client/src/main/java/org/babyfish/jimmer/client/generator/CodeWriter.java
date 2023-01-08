package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.generator.ts.File;
import org.babyfish.jimmer.client.meta.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public abstract class CodeWriter<C extends Context> {

    private final C ctx;

    protected final File file;

    private final StringBuilder codeBuilder = new StringBuilder();

    private int indent;

    private boolean lineDirty;

    private Scope scope;

    protected CodeWriter(C ctx, File file) {
        this.ctx = ctx;
        this.file = file;
    }

    public final C getContext() {
        return ctx;
    }

    public final File getFile() {
        return file;
    }

    public final CodeWriter<C> codeIf(boolean cond, String ts) {
        if (cond) {
            return code(ts);
        }
        return this;
    }

    public final CodeWriter<C> codeIf(boolean cond, char c) {
        if (cond) {
            return code(c);
        }
        return this;
    }

    public final CodeWriter<C> code(String ts) {
        if (ts.isEmpty()) {
            return this;
        }
        int size = ts.length();
        for (int i = 0; i < size; i++) {
            doAdd(ts.charAt(i));
        }
        return this;
    }

    public final CodeWriter<C> code(char c) {
        doAdd(c);
        return this;
    }

    public final CodeWriter<C> importFile(File file) {
        return importFile(file, false);
    }

    public final CodeWriter<C> importFile(File file, boolean treatAsData) {
        if (file != null && !file.equals(this.file)) {
            onImport(file, treatAsData);
        }
        return this;
    }

    protected abstract void onImport(File file, boolean treatAsData);

    public final CodeWriter<C> typeRef(Type type) {
        if (type instanceof UnresolvedTypeVariable) {
            code(((UnresolvedTypeVariable)type).getName());
        } else if (type instanceof SimpleType) {
            writeSimpleTypeRef((SimpleType) type);
        } else if (type instanceof NullableType) {
            writeNullableTypeRef((NullableType) type);
        } else if (type instanceof ArrayType) {
            writeArrayTypeRef((ArrayType) type);
        } else if (type instanceof MapType) {
            writeMapTypeRef((MapType) type);
        } else {
            File file = ctx.getFile(type);
            if (file != null) {
                importFile(file);
                if (type instanceof ImmutableObjectType &&
                        rawImmutableAsDynamic() &&
                        ((ImmutableObjectType)type).getCategory() == ImmutableObjectType.Category.RAW) {
                    writeDynamicTypeRef((ImmutableObjectType) type);
                } else if (type instanceof StaticObjectType) {
                    writeStaticTypeRef((StaticObjectType) type);
                } else {
                    code(file.getName());
                }
            } else if (type instanceof ImmutableObjectType) {
                writeDtoTypeRef((ImmutableObjectType) type);
            }
        }
        return this;
    }

    public CodeWriter<C> separator() {
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

    public CodeWriter<C> document(Document document) {
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

    public CodeWriter<C> scope(ScopeType type, String separator, boolean multiLines, Runnable runnable) {
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

        writePackageHeader(writer);
        writeImportHeader(writer);

        writer.write(codeBuilder.toString());
        writer.flush();
    }

    protected void writePackageHeader(Writer writer) throws IOException {}

    protected abstract void writeImportHeader(Writer writer) throws IOException;

    protected boolean rawImmutableAsDynamic() {
        return false;
    }

    protected abstract void writeSimpleTypeRef(SimpleType simpleType);

    protected abstract void writeNullableTypeRef(NullableType nullableType);

    protected abstract void writeArrayTypeRef(ArrayType arrayType);

    protected abstract void writeMapTypeRef(MapType mapType);

    protected abstract void writeDynamicTypeRef(ImmutableObjectType type);

    protected void writeStaticTypeRef(StaticObjectType type) {
        code(getContext().getFile(type).getName());
        List<Type> typeArguments = type.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            scope(ScopeType.GENERIC, ", ", false, () -> {
                for (Type typeArgument : typeArguments) {
                    separator();
                    typeRef(typeArgument);
                }
            });
        }
    }

    protected abstract void writeDtoTypeRef(ImmutableObjectType type);

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
}
