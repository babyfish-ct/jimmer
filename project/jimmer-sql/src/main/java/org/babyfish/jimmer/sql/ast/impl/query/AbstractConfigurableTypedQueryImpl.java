package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.table.TableSelection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class AbstractConfigurableTypedQueryImpl implements TypedQueryImplementor {

    private TypedQueryData data;

    private AbstractMutableQueryImpl baseQuery;

    public AbstractConfigurableTypedQueryImpl(
            TypedQueryData data,
            AbstractMutableQueryImpl baseQuery
    ) {
        this.data = data;
        this.baseQuery = baseQuery;
    }

    public AbstractMutableQueryImpl getBaseQuery() {
        return baseQuery;
    }

    public TypedQueryData getData() {
        return data;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return data.getSelections();
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        AstContext astContext = visitor.getAstContext();
        astContext.pushStatement(getBaseQuery());
        try {
            Selection<?> idOnlySelection = idOnlyExpressionByOffset();
            if (idOnlySelection != null) {
                baseQuery.accept(visitor, Collections.singletonList(idOnlySelection), false);
            } else {
                for (Selection<?> selection : data.getSelections()) {
                    Ast.from(selection, visitor.getAstContext()).accept(visitor);
                }
                baseQuery.accept(visitor, data.getOldSelections(), data.isWithoutSortingAndPaging());
            }
        } finally {
            astContext.popStatement();
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(getBaseQuery());
        try {
            if (data.isWithoutSortingAndPaging() || data.getLimit() == Integer.MAX_VALUE) {
                renderWithoutPaging(builder, null);
            } else {
                PropExpressionImplementor<?> idOnlyExpression = idOnlyExpressionByOffset();
                if (idOnlyExpression != null) {
                    IdOnlyQueryWrapperWriter wrapperWriter = new IdOnlyQueryWrapperWriter(idOnlyExpression, builder);
                    TableImplementor<?> tableImplementor = TableProxies.resolve(
                            idOnlyExpression.getTable(),
                            builder.getAstContext()
                    );
                    builder.sql("select ");
                    if (data.getSelections().get(0) instanceof FetcherSelection<?>) {
                        for (Field field : ((FetcherSelection<?>)data.getSelections().get(0)).getFetcher().getFieldMap().values()) {
                            wrapperWriter.prop(field.getProp(), false);
                        }
                    } else {
                        for (ImmutableProp prop : tableImplementor.getImmutableType().getProps().values()) {
                            wrapperWriter.prop(prop, false);
                        }
                    }
                    builder.sql(" from ").sql(tableImplementor.getImmutableType().getTableName());
                    if (wrapperWriter.getAlias() != null) {
                        builder.sql(" as ").sql(wrapperWriter.getAlias());
                    }
                    builder.sql(" where ");
                    wrapperWriter.resetComma();
                    wrapperWriter.prop(tableImplementor.getImmutableType().getIdProp(), true);
                    builder.sql(" in(");
                }
                SqlBuilder subBuilder = builder.createChildBuilder();
                renderWithoutPaging(
                        subBuilder,
                        idOnlyExpression != null ?
                                Collections.singletonList(idOnlyExpression) :
                                null
                );
                subBuilder.build(result -> {
                    PaginationContextImpl ctx = new PaginationContextImpl(
                            data.getLimit(),
                            data.getOffset(),
                            result.get_1(),
                            result.get_2()
                    );
                    baseQuery.getSqlClient().getDialect().paginate(ctx);
                    return ctx.build();
                });
                if (idOnlyExpression != null) {
                    builder.sql(")");
                }
            }
            if (data.isForUpdate()) {
                builder.sql(" for update");
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void renderWithoutPaging(SqlBuilder builder, List<Selection<?>> overrideSelections) {
        builder.sql("select ");
        if (data.isDistinct()) {
            builder.sql("distinct ");
        }
        renderSelections(overrideSelections != null ? overrideSelections : data.getSelections(), builder);
        baseQuery.renderTo(builder, data.isWithoutSortingAndPaging());
    }

    private static void renderSelections(List<Selection<?>> selections, SqlBuilder builder) {
        String separator = "";
        for (Selection<?> selection : selections) {
            builder.sql(separator);
            if (selection instanceof TableSelection) {
                TableSelection tableSelection = (TableSelection) selection;
                renderAllProps(tableSelection, builder);
            } else if (selection instanceof Table<?>) {
                TableSelection tableSelection = TableProxies.resolve(
                        (Table<? extends Object>) selection,
                        builder.getAstContext()
                );
                renderAllProps(tableSelection, builder);
            } else {
                Ast ast = Ast.from(selection, builder.getAstContext());
                if (ast instanceof PropExpressionImplementor<?>) {
                    ((PropExpressionImplementor<?>) ast).renderTo(builder, true);
                } else {
                    ast.renderTo(builder);
                }
            }
            separator = ", ";
        }
    }

    private PropExpressionImplementor<?> idOnlyExpressionByOffset() {
        if (data.getOffset() >= baseQuery.getSqlClient().getMinOffsetForIdOnlyScanMode()) {
            return data.getIdOnlyExpression();
        }
        return null;
    }

    private static void renderAllProps(TableSelection table, SqlBuilder builder) {
        String separator = "";
        Map<String, ImmutableProp> selectableProps = table
                .getImmutableType()
                .getSelectableProps();
        for (ImmutableProp prop : selectableProps.values()) {
            builder.sql(separator);
            table.renderSelection(prop, builder, null);
            separator = ", ";
        }
    }

    private static class IdOnlyQueryWrapperWriter {

        private static final String ALIAS = "tb_pagination_wrapper__";

        private final PropExpressionImplementor<?> idOnlyExpression;

        private final SqlBuilder builder;

        private boolean addComma;

        private boolean alias;

        IdOnlyQueryWrapperWriter(PropExpressionImplementor<?> idOnlyExpression, SqlBuilder builder) {
            this.idOnlyExpression = idOnlyExpression;
            this.builder = builder;
        }

        public void prop(ImmutableProp prop, boolean multiColumnsAsTuple) {
            SqlTemplate template = prop.getSqlTemplate();
            if (template instanceof FormulaTemplate) {
                appendComma();
                builder.sql(((FormulaTemplate)template).toSql(ALIAS));
                alias = true;
                return;
            }
            Storage storage = prop.getStorage();
            if (storage instanceof ColumnDefinition) {
                ColumnDefinition definition = (ColumnDefinition) storage;
                int size = definition.size();
                if (size == 1) {
                    appendComma();
                    builder.sql(definition.name(0));
                } else if (multiColumnsAsTuple) {
                    builder.enterTuple();
                    for (int i = 0; i < size; i++) {
                        appendComma();
                        builder.sql(definition.name(i));
                    }
                    builder.leaveTuple();
                } else {
                    for (int i = 0; i < size; i++) {
                        appendComma();
                        builder.sql(definition.name(i));
                    }
                }
            }
        }

        public String getAlias() {
            return alias ? ALIAS : null;
        }

        public void resetComma() {
            addComma = false;
        }

        private void appendComma() {
            if (addComma) {
                builder.sql(", ");
            } else {
                addComma = true;
            }
        }
    }
}
