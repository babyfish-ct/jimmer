package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public abstract class CodeWriter {

    private final Context ctx;

    protected final Source source;

    private final SourceManager sourceManager;

    private final String indentText;

    private Writer writer;

    private int indent;

    private boolean lineDirty;

    private Scope scope;

    public CodeWriter(Context ctx, Source source) {
        this.ctx = ctx;
        this.source = source;
        this.sourceManager = ctx.sourceManager;
        this.indentText = ctx.getIndent();
    }

    public abstract CodeWriter typeRef(Type type);

    protected final Source getSource(Type type) {
        Source importedSource;
        if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            ObjectType unwrapped = objectType.unwrap();
            if (unwrapped != null) {
                importedSource = sourceManager.getSource(unwrapped);
            } else {
                importedSource = sourceManager.getSource(objectType);
            }
        } else {
            importedSource = sourceManager.getSource(type);
        }
        if (importedSource != null && importedSource.getRoot() != source.getRoot()) {
            importSource(importedSource.getRoot());
        }
        return importedSource;
    }

    public final void importSource(Source source) {
        importSource(source, source.getName(), false);
    }

    public final void importSource(Source source, String name) {
        importSource(source, name, false);
    }

    public final void importSource(Source source, boolean treatAsValue) {
        importSource(source, source.getName(), treatAsValue);
    }

    public abstract void importSource(Source source, String name, boolean treatAsValue);

    protected abstract void onFlushImportedTypes();

    public final CodeWriter codeIf(boolean cond, String text) {
        if (cond) {
            return code(text);
        }
        return this;
    }

    public final CodeWriter codeIf(boolean cond, char c) {
        if (cond) {
            return code(c);
        }
        return this;
    }

    public final CodeWriter code(String text) {
        if (text.isEmpty()) {
            return this;
        }
        int size = text.length();
        for (int i = 0; i < size; i++) {
            doAdd(text.charAt(i));
        }
        return this;
    }

    public final CodeWriter code(char c) {
        doAdd(c);
        return this;
    }

    private void doAdd(char c) {
        if (writer == null) {
            throw new GeneratorException("The target writer of CodeWriter has not been set");
        }
        try {
            if (!lineDirty) {
                for (int i = indent; i > 0; --i) {
                    writer.write(indentText);
                }
                lineDirty = true;
            }
            if (scope != null) {
                scope.dirty();
            }
            writer.write(c);
            if (c == '\n') {
                lineDirty = false;
            }
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write code into writer", ex);
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

    public CodeWriter doc(Doc doc, DocPart ... parts) {
        if (doc == null) {
            return this;
        }
        Set<DocPart> partSet = EnumSet.noneOf(DocPart.class);
        partSet.addAll(Arrays.asList(parts));

        code("/**\n");
        code(" * ").code(doc.getValue().replace("\n", "\n * ")).code('\n');
        if (partSet.contains(DocPart.PARAM)) {
            for (Map.Entry<String, String> e : doc.getParameterValueMap().entrySet()) {
                code(" * @param ").code(e.getKey()).code(' ').code(e.getValue().replace("\n", "\n * ")).code('\n');
            }
        }
        if (partSet.contains(DocPart.RETURN) && doc.getReturnValue() != null) {
            code(" * @return ").code(doc.getReturnValue().replace("\n", "\n * ")).code('\n');
        }
        if (partSet.contains(DocPart.PROPERTY)) {
            for (Map.Entry<String, String> e : doc.getParameterValueMap().entrySet()) {
                code("@property ").code(e.getKey()).code(' ').code(e.getValue().replace("\n", "\n * ")).code('\n');
            }
        }
        code(" */\n");
        return this;
    }

    public CodeWriter doc(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        code("/**\n");
        code(" * ").code(text.replace("\n", "\n * ")).code('\n');
        code(" */\n");
        return this;
    }

    public CodeWriter renderChildren() {
        for (Source subSource : source.getSubSources()) {
            subSource.getRender().render(this);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <C extends Context> C getContext() {
        return (C) ctx;
    }

    public Source getSource() {
        return this.source;
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

    public enum DocPart {
        PARAM,
        RETURN,
        PROPERTY
    }

    void setWriter(Writer writer) {
        this.writer = writer;
    }
}
