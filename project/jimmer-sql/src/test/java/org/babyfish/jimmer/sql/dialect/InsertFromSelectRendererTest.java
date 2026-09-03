package org.babyfish.jimmer.sql.dialect;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertFromSelectRendererTest {

    @Test
    public void testCapabilityMatrix() {
        assertSupported(true, DefaultDialect.INSTANCE, ctx(InsertFromSelectMode.INSERT));
        assertSupported(false, DefaultDialect.INSTANCE, ctx(InsertFromSelectMode.INSERT).returning());
        assertSupported(false, DefaultDialect.INSTANCE, ctx(InsertFromSelectMode.INSERT_IF_ABSENT));
        assertSupported(false, DefaultDialect.INSTANCE, ctx(InsertFromSelectMode.UPSERT));

        assertSupported(true, new H2Dialect(), ctx(InsertFromSelectMode.INSERT).returning());
        assertSupported(true, new H2Dialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT).returning());
        assertSupported(true, new H2Dialect(), ctx(InsertFromSelectMode.UPSERT).returning());
        assertSupported(true, new H2Dialect(), ctx(InsertFromSelectMode.UPSERT).conflictPredicate());

        assertSupported(true, new PostgresDialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT).returning());
        assertSupported(true, new PostgresDialect(), ctx(InsertFromSelectMode.UPSERT).aliasing(true));
        assertSupported(true, new PostgresDialect(), ctx(InsertFromSelectMode.UPSERT).conflictPredicate());
        assertSupported(false, new PostgresDialect(), ctx(InsertFromSelectMode.UPSERT).aliasing(false));

        assertSupported(true, new SQLiteDialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT));
        assertSupported(true, new SQLiteDialect(), ctx(InsertFromSelectMode.UPSERT).conflictPredicate());
        assertSupported(false, new SQLiteDialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT).returning());
        assertSupported(false, new SQLiteDialect(), ctx(InsertFromSelectMode.UPSERT).aliasing(false));

        assertSupported(true, new MySqlDialect(), ctx(InsertFromSelectMode.INSERT));
        assertSupported(false, new MySqlDialect(), ctx(InsertFromSelectMode.INSERT).returning());
        assertSupported(false, new MySqlDialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT).unambiguous(true));
        assertSupported(false, new MySqlDialect(), ctx(InsertFromSelectMode.INSERT_IF_ABSENT).unambiguous(false));
        assertSupported(true, new MySqlDialect(), ctx(InsertFromSelectMode.UPSERT).simple(true).unambiguous(true));
        assertSupported(
                false,
                new MySqlDialect(),
                ctx(InsertFromSelectMode.UPSERT).simple(true).unambiguous(true).conflictPredicate()
        );
        assertSupported(false, new MySqlDialect(), ctx(InsertFromSelectMode.UPSERT).simple(false).unambiguous(true));
        assertSupported(false, new MySqlDialect(), ctx(InsertFromSelectMode.UPSERT).simple(true).unambiguous(false));
    }

    @Test
    public void testNullableConflictTargetCapability() {
        assertSupported(
                false,
                new H2Dialect(),
                ctx(InsertFromSelectMode.UPSERT).nullableConflictTargetSupported(false)
        );
        assertSupported(
                false,
                new PostgresDialect(),
                ctx(InsertFromSelectMode.UPSERT).nullableConflictTargetSupported(false)
        );
        assertSupported(
                false,
                new SQLiteDialect(),
                ctx(InsertFromSelectMode.INSERT_IF_ABSENT).nullableConflictTargetSupported(false)
        );
        assertSupported(
                false,
                new MySqlDialect(),
                ctx(InsertFromSelectMode.UPSERT)
                        .simple(true)
                        .unambiguous(true)
                        .nullableConflictTargetSupported(false)
        );
    }

    @Test
    public void testH2MergeRendering() {
        RecordingContext ctx = ctx(InsertFromSelectMode.UPSERT)
                .returning()
                .updateAssignments()
                .updatePredicates();
        new H2Dialect().getInsertFromSelectRenderer().render(ctx);
        assertEquals(
                "select [returning] from final table (" +
                        "merge into [table] [target] using [source-table] on " +
                        "<and>[conflict-condition]</and> " +
                        "when matched and <and>[update-predicates prefix=null,suffix=null]</and> " +
                        "then update set <comma>[update-assignments target=true,prefix=null,suffix=null]</comma> " +
                        "when not matched then insert([insert-columns])" +
                        "<values>([insert-values])</values>)",
                ctx.toString()
        );
    }

    @Test
    public void testH2FakeUpdateRendering() {
        RecordingContext ctx = ctx(InsertFromSelectMode.UPSERT);
        new H2Dialect().getInsertFromSelectRenderer().render(ctx);
        assertEquals(
                "merge into [table] [target] using [source-table] on " +
                        "<and>[conflict-condition]</and> " +
                        "when matched then update set <comma>|[fake left=true,right=true]</comma> " +
                        "when not matched then insert([insert-columns])<values>([insert-values])</values>",
                ctx.toString()
        );
    }

    @Test
    public void testConflictClauseRendering() {
        RecordingContext postgres = ctx(InsertFromSelectMode.INSERT_IF_ABSENT).returning();
        new PostgresDialect().getInsertFromSelectRenderer().render(postgres);
        assertEquals(
                "insert into [table] as [target]([insert-columns]) [source-select] " +
                        "on conflict([conflict-columns]) do nothing returning [returning]",
                postgres.toString()
        );

        RecordingContext sqlite = ctx(InsertFromSelectMode.UPSERT)
                .updateAssignments()
                .updatePredicates();
        new SQLiteDialect().getInsertFromSelectRenderer().render(sqlite);
        assertEquals(
                "insert into [table] as [target]([insert-columns]) " +
                        "select * from <SUB_QUERY>[source-select]</SUB_QUERY> where true " +
                        "on conflict([conflict-columns]) do update set " +
                        "<comma>[update-assignments target=false,prefix=excluded.,suffix=]</comma> " +
                        "where <and>[update-predicates prefix=excluded.,suffix=]</and>",
                sqlite.toString()
        );
    }

    @Test
    public void testLogicalDeleteConflictPredicateRendering() {
        RecordingContext h2 = ctx(InsertFromSelectMode.UPSERT)
                .conflictPredicate()
                .updateAssignments();
        new H2Dialect().getInsertFromSelectRenderer().render(h2);
        assertEquals(
                "merge into [table] [target] using [source-table] on " +
                        "<and>[conflict-condition]|[conflict-predicate target=true]</and> " +
                        "when matched then update set " +
                        "<comma>[update-assignments target=true,prefix=null,suffix=null]</comma> " +
                        "when not matched then insert([insert-columns])<values>([insert-values])</values>",
                h2.toString()
        );

        RecordingContext postgres = ctx(InsertFromSelectMode.UPSERT)
                .conflictPredicate()
                .updateAssignments();
        new PostgresDialect().getInsertFromSelectRenderer().render(postgres);
        assertEquals(
                "insert into [table] as [target]([insert-columns]) [source-select] " +
                        "on conflict([conflict-columns]) where [conflict-predicate target=false] " +
                        "do update set <comma>[update-assignments target=false,prefix=excluded.,suffix=]</comma>",
                postgres.toString()
        );

        RecordingContext sqlite = ctx(InsertFromSelectMode.INSERT_IF_ABSENT)
                .conflictPredicate();
        new SQLiteDialect().getInsertFromSelectRenderer().render(sqlite);
        assertEquals(
                "insert into [table] as [target]([insert-columns]) " +
                        "select * from <SUB_QUERY>[source-select]</SUB_QUERY> where true " +
                        "on conflict([conflict-columns]) where [conflict-predicate target=false] do nothing",
                sqlite.toString()
        );
    }

    @Test
    public void testMySqlAndDefaultRendering() {
        RecordingContext mysql = ctx(InsertFromSelectMode.UPSERT).updateAssignments();
        new MySqlDialect().getInsertFromSelectRenderer().render(mysql);
        assertEquals(
                "insert into [table]([insert-columns]) [source-select] on duplicate key update " +
                        "<comma>[update-assignments target=false,prefix=values(,suffix=)]</comma>",
                mysql.toString()
        );

        RecordingContext strictInsert = ctx(InsertFromSelectMode.INSERT);
        DefaultDialect.INSTANCE.getInsertFromSelectRenderer().render(strictInsert);
        assertEquals(
                "insert into [table]([insert-columns]) [source-select]",
                strictInsert.toString()
        );
    }

    private static RecordingContext ctx(InsertFromSelectMode mode) {
        return new RecordingContext(mode);
    }

    private static void assertSupported(
            boolean expected,
            Dialect dialect,
            RecordingContext ctx
    ) {
        assertEquals(
                expected,
                dialect.getInsertFromSelectRenderer().isSupported(ctx),
                dialect.getClass().getSimpleName() + ": " + ctx.mode
        );
    }

    private static final class RecordingContext implements InsertFromSelectContext {

        private final InsertFromSelectMode mode;
        private final StringBuilder builder = new StringBuilder();
        private final Deque<String> closingScopes = new ArrayDeque<>();
        private boolean returning;
        private boolean updateAssignments;
        private boolean updatePredicates;
        private boolean conflictPredicate;
        private boolean aliasing = true;
        private boolean simple;
        private boolean unambiguous;
        private boolean nullableConflictTargetSupported = true;

        private RecordingContext(InsertFromSelectMode mode) {
            this.mode = mode;
        }

        RecordingContext returning() {
            returning = true;
            return this;
        }

        RecordingContext updateAssignments() {
            updateAssignments = true;
            return this;
        }

        RecordingContext updatePredicates() {
            updatePredicates = true;
            return this;
        }

        RecordingContext conflictPredicate() {
            conflictPredicate = true;
            return this;
        }

        RecordingContext aliasing(boolean value) {
            aliasing = value;
            return this;
        }

        RecordingContext simple(boolean value) {
            simple = value;
            return this;
        }

        RecordingContext unambiguous(boolean value) {
            unambiguous = value;
            return this;
        }

        RecordingContext nullableConflictTargetSupported(boolean value) {
            nullableConflictTargetSupported = value;
            return this;
        }

        @Override
        public InsertFromSelectMode getMode() {
            return mode;
        }

        @Override
        public boolean hasReturning() {
            return returning;
        }

        @Override
        public boolean hasUpdateAssignments() {
            return updateAssignments;
        }

        @Override
        public boolean hasUpdatePredicates() {
            return updatePredicates;
        }

        @Override
        public boolean hasConflictPredicate() {
            return conflictPredicate;
        }

        @Override
        public boolean isUpdateExpressionAliasingSupported() {
            return aliasing;
        }

        @Override
        public boolean isSimpleInsertedValueUpdate() {
            return simple;
        }

        @Override
        public boolean isConflictTargetUnambiguous() {
            return unambiguous;
        }

        @Override
        public boolean isNullableConflictTargetSupported() {
            return nullableConflictTargetSupported;
        }

        @Override
        public InsertFromSelectContext sql(String sql) {
            builder.append(sql);
            return this;
        }

        @Override
        public InsertFromSelectContext enter(AbstractSqlBuilder.ScopeType type) {
            switch (type) {
                case TUPLE:
                    builder.append('(');
                    closingScopes.push(")");
                    break;
                case COMMA:
                    builder.append("<comma>");
                    closingScopes.push("</comma>");
                    break;
                case AND:
                    builder.append("<and>");
                    closingScopes.push("</and>");
                    break;
                case VALUES:
                    builder.append("<values>");
                    closingScopes.push("</values>");
                    break;
                default:
                    builder.append('<').append(type).append('>');
                    closingScopes.push("</" + type + ">");
                    break;
            }
            return this;
        }

        @Override
        public InsertFromSelectContext separator() {
            builder.append('|');
            return this;
        }

        @Override
        public InsertFromSelectContext leave() {
            builder.append(closingScopes.pop());
            return this;
        }

        @Override
        public InsertFromSelectContext appendTableName() {
            return marker("table");
        }

        @Override
        public InsertFromSelectContext appendTargetAlias() {
            return marker("target");
        }

        @Override
        public InsertFromSelectContext appendInsertColumns() {
            return marker("insert-columns");
        }

        @Override
        public InsertFromSelectContext appendConflictColumns() {
            return marker("conflict-columns");
        }

        @Override
        public InsertFromSelectContext appendConflictPredicate(boolean targetAlias) {
            return marker("conflict-predicate target=" + targetAlias);
        }

        @Override
        public InsertFromSelectContext appendSourceSelect() {
            return marker("source-select");
        }

        @Override
        public InsertFromSelectContext appendSourceTable() {
            return marker("source-table");
        }

        @Override
        public InsertFromSelectContext appendConflictCondition() {
            return marker("conflict-condition");
        }

        @Override
        public InsertFromSelectContext appendInsertValues() {
            return marker("insert-values");
        }

        @Override
        public InsertFromSelectContext appendUpdateAssignments(
                boolean targetAlias,
                @Nullable String insertedValuePrefix,
                @Nullable String insertedValueSuffix
        ) {
            return marker(
                    "update-assignments target=" + targetAlias +
                            ",prefix=" + insertedValuePrefix +
                            ",suffix=" + insertedValueSuffix
            );
        }

        @Override
        public InsertFromSelectContext appendFakeUpdateAssignment(
                boolean leftTargetAlias,
                boolean rightTargetAlias
        ) {
            return marker("fake left=" + leftTargetAlias + ",right=" + rightTargetAlias);
        }

        @Override
        public InsertFromSelectContext appendUpdatePredicates(
                @Nullable String insertedValuePrefix,
                @Nullable String insertedValueSuffix
        ) {
            return marker(
                    "update-predicates prefix=" + insertedValuePrefix +
                            ",suffix=" + insertedValueSuffix
            );
        }

        @Override
        public InsertFromSelectContext appendReturning() {
            return marker("returning");
        }

        private InsertFromSelectContext marker(String marker) {
            builder.append('[').append(marker).append(']');
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
