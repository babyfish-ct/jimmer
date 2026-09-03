package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;

final class InsertFromSelectRenderers {

    static final InsertFromSelectRenderer DEFAULT = new DefaultRenderer();

    static final InsertFromSelectRenderer H2 = new H2Renderer();

    static final InsertFromSelectRenderer POSTGRES = new ConflictClauseRenderer(true, false);

    static final InsertFromSelectRenderer SQLITE = new ConflictClauseRenderer(false, true);

    static final InsertFromSelectRenderer MYSQL = new MySqlRenderer();

    private InsertFromSelectRenderers() {
    }

    private static void renderInsertHead(InsertFromSelectContext ctx, boolean aliasTarget) {
        ctx.sql("insert into ").appendTableName();
        if (aliasTarget) {
            ctx.sql(" as ").appendTargetAlias();
        }
        ctx.enter(AbstractSqlBuilder.ScopeType.TUPLE)
                .appendInsertColumns()
                .leave()
                .sql(" ");
    }

    private static void renderStrictInsert(InsertFromSelectContext ctx) {
        renderStrictInsert(ctx, true);
    }

    private static void renderStrictInsert(InsertFromSelectContext ctx, boolean appendReturning) {
        renderInsertHead(ctx, false);
        ctx.appendSourceSelect();
        if (appendReturning && ctx.hasReturning()) {
            ctx.sql(" returning ").appendReturning();
        }
    }

    private static class DefaultRenderer implements InsertFromSelectRenderer {

        @Override
        public boolean isSupported(InsertFromSelectContext ctx) {
            return ctx.getMode() == InsertFromSelectMode.INSERT && !ctx.hasReturning();
        }

        @Override
        public void render(InsertFromSelectContext ctx) {
            renderStrictInsert(ctx);
        }
    }

    private static class ConflictClauseRenderer implements InsertFromSelectRenderer {

        private final boolean returningSupported;

        private final boolean wrapSourceWithWhere;

        private ConflictClauseRenderer(boolean returningSupported, boolean wrapSourceWithWhere) {
            this.returningSupported = returningSupported;
            this.wrapSourceWithWhere = wrapSourceWithWhere;
        }

        @Override
        public boolean isSupported(InsertFromSelectContext ctx) {
            if (ctx.hasReturning() && !returningSupported) {
                return false;
            }
            if (ctx.getMode() != InsertFromSelectMode.INSERT &&
                    !ctx.isNullableConflictTargetSupported()) {
                return false;
            }
            return ctx.getMode() != InsertFromSelectMode.UPSERT ||
                    ctx.isUpdateExpressionAliasingSupported();
        }

        @Override
        public void render(InsertFromSelectContext ctx) {
            if (ctx.getMode() == InsertFromSelectMode.INSERT) {
                renderStrictInsert(ctx);
                return;
            }
            renderInsertHead(ctx, true);
            if (wrapSourceWithWhere) {
                ctx.sql("select * from ")
                        .enter(AbstractSqlBuilder.ScopeType.SUB_QUERY)
                        .appendSourceSelect()
                        .leave()
                        .sql(" where true");
            } else {
                ctx.appendSourceSelect();
            }
            ctx.sql(" on conflict")
                    .enter(AbstractSqlBuilder.ScopeType.TUPLE)
                    .appendConflictColumns()
                    .leave();
            if (ctx.hasConflictPredicate()) {
                ctx.sql(" where ").appendConflictPredicate(false);
            }
            if (ctx.getMode() == InsertFromSelectMode.INSERT_IF_ABSENT) {
                ctx.sql(" do nothing");
            } else {
                ctx.sql(" do update set ").enter(AbstractSqlBuilder.ScopeType.COMMA);
                if (ctx.hasUpdateAssignments()) {
                    ctx.appendUpdateAssignments(false, "excluded.", "");
                } else {
                    ctx.separator().appendFakeUpdateAssignment(false, true);
                }
                ctx.leave();
                if (ctx.hasUpdatePredicates()) {
                    ctx.sql(" where ")
                            .enter(AbstractSqlBuilder.ScopeType.AND)
                            .appendUpdatePredicates("excluded.", "")
                            .leave();
                }
            }
            if (ctx.hasReturning()) {
                ctx.sql(" returning ").appendReturning();
            }
        }
    }

    private static class H2Renderer implements InsertFromSelectRenderer {

        @Override
        public boolean isSupported(InsertFromSelectContext ctx) {
            return ctx.getMode() == InsertFromSelectMode.INSERT ||
                    ctx.isNullableConflictTargetSupported();
        }

        @Override
        public void render(InsertFromSelectContext ctx) {
            if (ctx.hasReturning()) {
                ctx.sql("select ").appendReturning().sql(" from final table (");
            }
            if (ctx.getMode() == InsertFromSelectMode.INSERT) {
                renderStrictInsert(ctx, false);
            } else {
                renderMerge(ctx);
            }
            if (ctx.hasReturning()) {
                ctx.sql(")");
            }
        }

        private void renderMerge(InsertFromSelectContext ctx) {
            ctx.sql("merge into ")
                    .appendTableName()
                    .sql(" ")
                    .appendTargetAlias()
                    .sql(" using ")
                    .appendSourceTable()
                    .sql(" on ")
                    .enter(AbstractSqlBuilder.ScopeType.AND)
                    .appendConflictCondition();
            if (ctx.hasConflictPredicate()) {
                ctx.separator().appendConflictPredicate(true);
            }
            ctx.leave();
            if (ctx.getMode() == InsertFromSelectMode.UPSERT) {
                ctx.sql(" when matched");
                if (ctx.hasUpdatePredicates()) {
                    ctx.sql(" and ")
                            .enter(AbstractSqlBuilder.ScopeType.AND)
                            .appendUpdatePredicates(null, null)
                            .leave();
                }
                ctx.sql(" then update set ").enter(AbstractSqlBuilder.ScopeType.COMMA);
                if (ctx.hasUpdateAssignments()) {
                    ctx.appendUpdateAssignments(true, null, null);
                } else {
                    ctx.separator().appendFakeUpdateAssignment(true, true);
                }
                ctx.leave();
            }
            ctx.sql(" when not matched then insert")
                    .enter(AbstractSqlBuilder.ScopeType.TUPLE)
                    .appendInsertColumns()
                    .leave()
                    .enter(AbstractSqlBuilder.ScopeType.VALUES)
                    .enter(AbstractSqlBuilder.ScopeType.TUPLE)
                    .appendInsertValues()
                    .leave()
                    .leave();
        }
    }

    private static class MySqlRenderer implements InsertFromSelectRenderer {

        @Override
        public boolean isSupported(InsertFromSelectContext ctx) {
            if (ctx.hasReturning()) {
                return false;
            }
            if (ctx.hasConflictPredicate()) {
                return false;
            }
            switch (ctx.getMode()) {
                case INSERT:
                    return true;
                case INSERT_IF_ABSENT:
                    return false;
                case UPSERT:
                    return ctx.isNullableConflictTargetSupported() &&
                            ctx.isSimpleInsertedValueUpdate() &&
                            ctx.isConflictTargetUnambiguous();
                default:
                    return false;
            }
        }

        @Override
        public void render(InsertFromSelectContext ctx) {
            if (ctx.getMode() == InsertFromSelectMode.INSERT) {
                renderStrictInsert(ctx);
                return;
            }
            renderInsertHead(ctx, false);
            ctx.appendSourceSelect()
                    .sql(" on duplicate key update ")
                    .enter(AbstractSqlBuilder.ScopeType.COMMA);
            if (ctx.hasUpdateAssignments()) {
                ctx.appendUpdateAssignments(false, "values(", ")");
            } else {
                ctx.separator().appendFakeUpdateAssignment(false, false);
            }
            ctx.leave();
        }
    }
}
