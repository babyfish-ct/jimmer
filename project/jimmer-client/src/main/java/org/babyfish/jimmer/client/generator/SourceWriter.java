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

public abstract class SourceWriter extends CodeWriter<SourceWriter> {

    private final Context ctx;

    protected final Source source;

    private final SourceManager sourceManager;

    public SourceWriter(Context ctx, Source source) {
        super(ctx.getIndent());
        this.ctx = ctx;
        this.source = source;
        this.sourceManager = ctx.sourceManager;
    }

    @SuppressWarnings("unchecked")
    public final <C extends Context> C getContext() {
        return (C) ctx;
    }

    public abstract SourceWriter typeRef(Type type);

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

    public SourceWriter doc(Doc doc, DocPart ... parts) {
        if (doc == null) {
            return this;
        }
        Set<DocPart> partSet = EnumSet.noneOf(DocPart.class);
        partSet.addAll(Arrays.asList(parts));

        if (partSet.isEmpty() && doc.getValue() == null) {
            return this;
        }

        code("/**\n");
        if (doc.getValue() != null) {
            code(" * ").code(doc.getValue().replace("\n", "\n * ")).code('\n');
        }
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

    public SourceWriter doc(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        code("/**\n");
        code(" * ").code(text.replace("\n", "\n * ")).code('\n');
        code(" */\n");
        return this;
    }

    public SourceWriter renderChildren() {
        for (Source subSource : source.getSubSources()) {
            separator();
            subSource.getRender().render(this);
        }
        return this;
    }

    public Source getSource() {
        return this.source;
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
}
