//package org.babyfish.jimmer.sql.ast.impl.base;
//
//import org.babyfish.jimmer.meta.ImmutableProp;
//import org.babyfish.jimmer.sql.ast.PropExpression;
//import org.babyfish.jimmer.sql.ast.impl.*;
//import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
//import org.babyfish.jimmer.sql.ast.table.Table;
//import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
//import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
//import org.babyfish.jimmer.sql.meta.MetadataStrategy;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.time.temporal.Temporal;
//import java.util.Date;
//
//class BaseTablePropExpression<T> implements PropExpressionImplementor<T> {
//
//    protected final PropExpressionImplementor<T> raw;
//
//    private final BaseTableOwner baseTableOwner;
//
//    BaseTablePropExpression(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
//        this.raw = raw;
//        this.baseTableOwner = baseTableOwner;
//    }
//
//    @Override
//    public Table<?> getTable() {
//        return raw.getTable();
//    }
//
//    @Override
//    public ImmutableProp getProp() {
//        return raw.getProp();
//    }
//
//    @Override
//    public ImmutableProp getDeepestProp() {
//        return raw.getDeepestProp();
//    }
//
//    @Override
//    public PropExpressionImpl.@Nullable EmbeddedImpl<?> getBase() {
//        return raw.getBase();
//    }
//
//    @Override
//    public @Nullable String getPath() {
//        return raw.getPath();
//    }
//
//    @Override
//    public boolean isRawId() {
//        return raw.isRawId();
//    }
//
//    @Override
//    public EmbeddedColumns.@Nullable Partial getPartial(MetadataStrategy strategy) {
//        return raw.getPartial(strategy);
//    }
//
//    @Override
//    public void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets) {
//        this.raw.renderTo(builder, ignoreBrackets);
//    }
//
//    @Override
//    public PropExpression<T> unwrap() {
//        return raw.unwrap();
//    }
//
//    @Override
//    public Class<T> getType() {
//        return raw.getType();
//    }
//
//    @Override
//    public int precedence() {
//        return raw.precedence();
//    }
//
//    static class Cmp<T extends Comparable<?>>
//            extends BaseTablePropExpression<T>
//            implements PropExpression.Cmp<T> {
//
//        Cmp(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
//            super(raw, baseTableOwner);
//        }
//    }
//
//    static class Str extends Cmp<String> implements PropExpression.Str {
//
//        Str(PropExpressionImplementor<String> raw, BaseTableOwner baseTableOwner) {
//            super(raw, baseTableOwner);
//        }
//    }
//
//    static class Num<N extends Number & Comparable<N>>
//            extends Cmp<N>
//            implements PropExpression.Num<N> {
//
//        Num(PropExpressionImplementor<N> raw, BaseTableOwner baseTableOwner) {
//            super(raw, baseTableOwner);
//        }
//    }
//
//    static class Dt<T extends Date>
//        extends Cmp<T>
//        implements PropExpression.Dt<T> {
//
//        Dt(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
//            super(raw, baseTableOwner);
//        }
//    }
//
//    static class Tp<T extends Temporal & Comparable<?>>
//        extends Cmp<T>
//        implements PropExpression.Tp<T> {
//
//        Tp(PropExpressionImplementor<T> raw, BaseTableOwner baseTableOwner) {
//            super(raw, baseTableOwner);
//        }
//    }
//}
