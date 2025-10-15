package org.babyfish.jimmer.client.generator;

import java.io.IOException;

public class CodeWriter<W extends CodeWriter<W>> {

    private final String indentText;

    private Appendable writer;

    private int indent;

    private boolean lineDirty;

    private Scope scope;

    public CodeWriter(String indent) {
        this.indentText = indent;
    }

    @SuppressWarnings("unchecked")
    public final W codeIf(boolean cond, String text) {
        if (cond) {
            return code(text);
        }
        return (W) this;
    }

    @SuppressWarnings("unchecked")
    public final W codeIf(boolean cond, char c) {
        if (cond) {
            return code(c);
        }
        return (W) this;
    }

    @SuppressWarnings("unchecked")
    public final W code(String text) {
        if (text.isEmpty()) {
            return (W) this;
        }
        int size = text.length();
        for (int i = 0; i < size; i++) {
            doAdd(text.charAt(i));
        }
        return (W) this;
    }

    @SuppressWarnings("unchecked")
    public final W code(char c) {
        doAdd(c);
        return (W) this;
    }

    private void doAdd(char c) {
        if (writer == null) {
            throw new GeneratorException("The target writer of CodeWriter has not been set");
        }
        try {
            if (!lineDirty) {
                for (int i = indent; i > 0; --i) {
                    writer.append(indentText);
                }
                lineDirty = true;
            }
            if (scope != null) {
                scope.dirty();
            }
            writer.append(c);
            if (c == '\n') {
                lineDirty = false;
            }
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write code into writer", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public final W scope(
            CodeWriter.ScopeType type,
            String separator,
            boolean multiLines,
            Runnable block
    ) {
        CodeWriter.Scope oldScope = scope;
        CodeWriter.Scope newScope = new CodeWriter.Scope(oldScope, separator, multiLines);

        code(type.prefix);
        if (multiLines) {
            code('\n');
        }
        indent++;

        scope = newScope;

        block.run();

        --indent;
        if (multiLines && lineDirty) {
            code('\n');
        }
        code(type.suffix);

        scope = oldScope;
        return (W)this;
    }

    @SuppressWarnings("unchecked")
    public final W separator() {
        CodeWriter.Scope scope = this.scope;
        if (scope == null) {
            throw new IllegalStateException("There is no existing scope");
        }
        if (scope.dirty) {
            code(scope.separator);
            if (scope.multiLines) {
                code('\n');
            }
        }
        return (W) this;
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

        private final CodeWriter.Scope parent;

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

    protected void setWriter(Appendable writer) {
        this.writer = writer;
    }
}
