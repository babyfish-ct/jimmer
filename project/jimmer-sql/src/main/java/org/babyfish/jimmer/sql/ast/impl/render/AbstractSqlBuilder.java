package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;

public abstract class AbstractSqlBuilder<T extends AbstractSqlBuilder<T>> {

    protected final StringBuilder builder = new StringBuilder();

    private boolean indentRequired;

    protected abstract SqlFormatter formatter();

    protected abstract ScopeManager scopeManager();

    public abstract JSqlClientImplementor sqlClient();

    @SuppressWarnings("unchecked")
    public T sql(String sql) {
        preAppend();
        builder.append(sql);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T sql(ValueGetter getter) {
        getter.metadata().renderTo(this);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T logicalDeleteAssignment(LogicalDeletedInfo logicalDeletedInfo, String alias) {
        String assignedName = logicalDeletedInfo.getColumnName();
        if (alias != null) {
            assignedName = alias + '.' + assignedName;
        }
        sql(assignedName).sql(" = ");
        Object value = logicalDeletedInfo.generateValue();
        if (value == null) {
            sql("null");
        } else {
            rawVariable(value);
        }
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T logicalDeleteFilter(LogicalDeletedInfo logicalDeletedInfo, String alias) {
        String assignedName = logicalDeletedInfo.getColumnName();
        if (alias != null) {
            assignedName = alias + '.' + assignedName;
        }
        LogicalDeletedInfo.Action action = logicalDeletedInfo.getAction();
        if (action instanceof LogicalDeletedInfo.Action.Eq) {
            LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
            sql(assignedName).sql(" = ");
            rawVariable(eq.getValue());
        } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
            LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
            sql(assignedName).sql(" <> ");
            rawVariable(ne.getValue());
        } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
            sql(assignedName).sql(" is null");
        }
        else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
            sql(assignedName).sql(" is not null");
        }
        return (T)this;
    }

    public abstract T rawVariable(Object value);

    @SuppressWarnings("unchecked")
    public T enter(String separator) {
        enterImpl(ScopeType.BLANK, separator);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T enter(ScopeType type) {
        enterImpl(type, null);
        return (T)this;
    }

    private void enterImpl(ScopeType type, String separator) {
        ScopeManager scopeManager = scopeManager();
        Scope parentScope = scopeManager.current;
        boolean ignored = type == ScopeType.NULL || (
                type == ScopeType.TUPLE &&
                        parentScope != null &&
                        parentScope.type == ScopeType.TUPLE
        );
        if (!ignored) {
            if (type == ScopeType.SMART_OR) {
                if (parentScope == null) {
                    part(ScopeType.SUB_QUERY.prefix);
                } else if (parentScope.isAndLike()) {
                    part(ScopeType.SUB_QUERY.prefix);
                }
            } else {
                part(type.prefix);
            }
        }
        scopeManager.current = new Scope(parentScope, type, ignored, separator);
    }

    @SuppressWarnings("unchecked")
    public T separator() {
        Scope scope = this.scopeManager().current;
        if (scope != null && scope.dirty) {
            boolean forceInLine = false;
            if (scope.type == ScopeType.LIST) {
                forceInLine = ++scope.listSeparatorCount < formatter().getListParamCountInLine();
                if (!forceInLine) {
                    scope.listSeparatorCount = 0;
                }
            }
            if (scope.type.isSeparatorIndent) {
                part(scope.separator, forceInLine);
            } else {
                scope.depth--;
                part(scope.separator, forceInLine);
                scope.depth++;
            }
            scope.dirty = false;
        }
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public T leave() {
        ScopeManager scopeManager = scopeManager();
        Scope scope = scopeManager.current;
        Scope parentScope = scope.parent;
        scopeManager.current = parentScope;
        if (!scope.ignored) {
            if (scope.type == ScopeType.SMART_OR) {
                if (parentScope == null) {
                    part(ScopeType.SUB_QUERY.suffix);
                } else if (parentScope.isAndLike()) {
                    part(ScopeType.SUB_QUERY.suffix);
                }
            } else {
                part(scope.type.suffix);
            }
        }
        return (T)this;
    }

    private void part(ScopeType.Part part) {
        part(part, false);
    }

    private void part(ScopeType.Part part, boolean forceInLine) {
        if (part == null) {
            return;
        }
        space(part.before, forceInLine);
        preAppend();
        builder.append(part.value);
        space(part.after, forceInLine);
    }

    public T space(char ch) {
        return space(ch, false);
    }

    @SuppressWarnings("unchecked")
    public T space(char ch, boolean forceInLine) {
        switch (ch) {
            case '?':
                if (!forceInLine && formatter().isPretty()) {
                    newLine();
                } else {
                    preAppend();
                    builder.append(' ');
                }
                break;
            case ' ':
                preAppend();
                builder.append(' ');
                break;
            case '\n':
                if (!forceInLine && formatter().isPretty()) {
                    newLine();
                }
                break;
        }
        return (T)this;
    }

    protected final void preAppend() {
        Scope scope = scopeManager().current;
        if (scope != null) {
            scope.setDirty();
        }
        if (scope == null || !indentRequired) {
            indentRequired = false;
            return;
        }
        indentRequired = false;
        String indent = formatter().getIndent();
        for (int i = scope.depth; i > 0; --i) {
            builder.append(indent);
        }
    }

    private void newLine() {
        builder.append('\n');
        indentRequired = true;
    }

    public enum ScopeType {
        NULL(null, null, null),
        BLANK(null, null, null),
        SELECT("select?", ",?", null),
        SELECT_DISTINCT("select distinct?", ",?", null),
        SET("?set?", ",?", null),
        WHERE("?where?", "?and?", null),
        ORDER_BY("?order by?", ",?", null),
        GROUP_BY("?group by?", ",?", null),
        HAVING("?having?", "?and?", null),
        SUB_QUERY("(\n", null, "\n)"),
        LIST("(\n", ",?", "\n)"),
        COMMA(null, ",?", null),
        TUPLE("(", ", ", ")"),
        AND(null, "?and?", null, false),
        OR(null, "?or?", null, false),
        SMART_OR(null, "?or?", null, false),
        VALUES("?values\n", ",?", null);

        final Part prefix;

        final Part separator;

        final Part suffix;

        final boolean isSeparatorIndent;

        ScopeType(String prefix, String separator, String suffix) {
            this(prefix, separator, suffix, true);
        }

        ScopeType(String prefix, String separator, String suffix, boolean isSeparatorIndent) {
            this.prefix = partOf(prefix);
            this.separator = partOf(separator);
            this.suffix = partOf(suffix);
            this.isSeparatorIndent = isSeparatorIndent;
        }

        private static class Part {

            final char before;
            final String value;
            final char after;

            Part(char before, String value, char after) {
                this.before = before;
                this.value = value;
                this.after = after;
            }
        }

        private static Part partOf(String value) {
            if (value == null) {
                return null;
            }
            char before = spaceChar(value.charAt(0));
            char after = value.length() > 1 ? spaceChar(value.charAt(value.length() - 1)) : 0;
            return new Part(
                    before,
                    value.substring(before == '\0' ? 0 : 1, value.length() - (after == '\0' ? 0 : 1)),
                    after
            );
        }

        private static char spaceChar(char c) {
            return c == ' ' || c == '\n' || c == '?' ? c : '\0';
        }
    }

    protected static class ScopeManager {
        public ScopeManager() {}
        public Scope current;
        public Scope cloneScope() {
            if (current == null) {
                return null;
            }
            return new Scope(current);
        }
    }

    protected static class Scope {

        final Scope parent;

        final ScopeType type;

        final boolean ignored;

        final ScopeType.Part separator;

        int depth;

        boolean dirty;

        int listSeparatorCount;

        Scope(
                Scope parent,
                ScopeType type,
                boolean ignored,
                String separator
        ) {
            this.parent = parent;
            this.type = type;
            this.ignored = ignored;
            this.depth = ignored ? (parent != null ? parent.depth: 0) : (parent != null ? parent.depth + 1: 1);
            this.separator = separator != null ? ScopeType.partOf(separator) : type.separator;
        }

        Scope(Scope base) {
            this.parent = base.parent != null ? new Scope(base.parent) : null;
            this.type = base.type;
            this.ignored = base.ignored;
            this.separator = base.separator;
            this.depth = base.depth;
            this.dirty = base.dirty;
            this.listSeparatorCount = base.listSeparatorCount;
        }

        void setDirty() {
            for (Scope scope = this; scope != null; scope = scope.parent) {
                if (scope.dirty) {
                    break;
                }
                scope.dirty = true;
            }
        }

        public boolean isAndLike() {
            for (Scope scope = this; scope != null; scope = scope.parent) {
                ScopeType scopeType = scope.type;
                if (scopeType != ScopeType.NULL) {
                    return scopeType == ScopeType.AND ||
                            scopeType == ScopeType.WHERE ||
                            scopeType == ScopeType.HAVING;
                }
            }
            return false;
        }
    }
}
