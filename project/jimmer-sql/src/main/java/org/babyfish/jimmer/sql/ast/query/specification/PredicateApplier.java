package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.PolymorphicTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.*;

public class PredicateApplier {

    private Context context;

    private final Deque<PredicateCaptureFrame> captureFrames = new ArrayDeque<>();

    public PredicateApplier(MutableQuery query) {
        AbstractMutableStatementImpl statement = (AbstractMutableStatementImpl) query;
        this.context = new Context(null, statement, null);
    }

    public Predicate capture(Runnable block) {
        Context oldContext = context;
        AbstractMutableStatementImpl statement = context.statement();
        PredicateCaptureFrame frame = new PredicateCaptureFrame(statement);
        captureFrames.push(frame);
        try {
            block.run();
            return Predicate.and(frame.toArray());
        } finally {
            captureFrames.pop();
            context = oldContext;
        }
    }

    public void where(Predicate... predicates) {
        where(context.statement(), predicates);
    }

    public void apply(JSpecification<?, ?> specification) {
        Specifications.apply(this, specification);
    }

    void applyWithTypeContext(JSpecification<?, ?> specification) {
        applyTypePredicate(specification.entityType());
        Context oldContext = context;
        context = context.forType(specification.entityType());
        try {
            applyBody(specification);
        } finally {
            context = oldContext;
        }
    }

    void applyBody(JSpecification<?, ?> specification) {
        specification.applyTo(new SpecificationArgs<>(this));
    }

    public void applyTypePredicate(Class<?> type) {
        Table<?> table = context.table();
        ImmutableType tableType = table.getImmutableType();
        if (tableType.getInheritanceInfo() != null &&
                tableType.getJavaClass() != type &&
                tableType.getJavaClass().isAssignableFrom(type)
        ) {
            where(TableProxies.instanceOf(table, type));
        }
    }

    public void push(ImmutableProp prop) {
        Context ctx = context;
        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
            this.context = new Context(ctx, prop.isReference(TargetLevel.PERSISTENT), prop);
        } else if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            this.context = new Context(ctx, prop);
        } else {
            throw new IllegalArgumentException("\"" + prop + "\" is not association property");
        }
    }

    public void pop() {
        Context ctx = context;
        Context parentCtx = ctx.parent;
        if (parentCtx == null) {
            throw new IllegalStateException("No context to be pop");
        }
        this.context = parentCtx;
    }

    public AbstractMutableStatementImpl getQuery() {
        return this.context.statement();
    }

    public Table<?> getTable() {
        return this.context.table();
    }

    public TableImplementor<?> getTableImplementor() {
        Table<?> table = this.context.table();
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<?>) table;
        }
        if (table instanceof TableProxy<?>) {
            TableImplementor<?> implementor = ((TableProxy<?>) table).__unwrap();
            if (implementor != null) {
                return implementor;
            }
        }
        throw new IllegalStateException(
                "The current table cannot be resolved to \"" +
                        TableImplementor.class.getName() +
                        "\" immediately"
        );
    }

    public void eq(ImmutableProp[] props, Object value) {
        if (isNullOrEmpty(value)) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            predicates[i] = context.get(props[i]).eq(value);
        }
        where(context.statement(), Predicate.or(predicates));
    }

    public void ne(ImmutableProp prop, Object value) {
        if (isNullOrEmpty(value)) {
            return;
        }
        where(context.statement(), context.get(prop).ne(value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void gt(ImmutableProp prop, Object value) {
        if (value == null) {
            return;
        }
        ComparableExpression<Comparable<Comparable<?>>> expr = (ComparableExpression) context.get(prop);
        where(context.statement(), expr.gt((Comparable<Comparable<?>>) value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void ge(ImmutableProp prop, Object value) {
        if (value == null) {
            return;
        }
        ComparableExpression<Comparable<Comparable<?>>> expr = (ComparableExpression) context.get(prop);
        where(context.statement(), expr.ge((Comparable<Comparable<?>>) value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void lt(ImmutableProp prop, Object value) {
        if (value == null) {
            return;
        }
        ComparableExpression<Comparable<Comparable<?>>> expr = (ComparableExpression) context.get(prop);
        where(context.statement(), expr.lt((Comparable<Comparable<?>>) value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void le(ImmutableProp prop, Object value) {
        if (value == null) {
            return;
        }
        ComparableExpression<Comparable<Comparable<?>>> expr = (ComparableExpression) context.get(prop);
        where(context.statement(), expr.le((Comparable<Comparable<?>>) value));
    }

    public void isNull(ImmutableProp[] props, boolean value) {
        if (!value) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            predicates[i] = props[i].isAssociation(TargetLevel.ENTITY) ?
                    context.table().getAssociatedId(props[i]).isNull() :
                    context.get(props[i]).isNull();
        }
        where(context.statement(), Predicate.or(predicates));
    }

    public void isNotNull(ImmutableProp[] props, boolean value) {
        if (!value) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            predicates[i] = props[i].isAssociation(TargetLevel.ENTITY) ?
                    context.table().getAssociatedId(props[i]).isNotNull() :
                    context.get(props[i]).isNotNull();
        }
        where(context.statement(), Predicate.or(predicates));
    }

    public void like(ImmutableProp[] props, String value, boolean insensitive, boolean matchStart, boolean matchEnd) {
        if (isNullOrEmpty(value)) {
            return;
        }
        LikeMode mode = likeMode(matchStart, matchEnd);
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            predicates[i] = insensitive ?
                    ((StringExpression) context.<String>get(props[i])).ilike(value, mode) :
                    ((StringExpression) context.<String>get(props[i])).like(value, mode);
        }
        where(context.statement(), Predicate.or(predicates));
    }

    public void notLike(ImmutableProp prop, String value, boolean insensitive, boolean matchStart, boolean matchEnd) {
        if (isNullOrEmpty(value)) {
            return;
        }
        LikeMode mode = likeMode(matchStart, matchEnd);
        Predicate predicate = insensitive ?
                ((StringExpression) context.<String>get(prop)).ilike(value, mode) :
                ((StringExpression) context.<String>get(prop)).like(value, mode);
        where(context.statement(), Predicate.not(predicate));
    }

    @SuppressWarnings("unchecked")
    public void valueIn(ImmutableProp[] props, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            predicates[i] = context.get(props[i]).in((Collection<Object>) values);
        }
        where(context.statement(), Predicate.or(predicates));
    }

    @SuppressWarnings("unchecked")
    public void valueNotIn(ImmutableProp prop, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        where(context.statement(), context.get(prop).notIn((Collection<Object>) values));
    }

    public void associatedIdEq(ImmutableProp[] props, Object associatedId) {
        if (associatedId == null) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            ImmutableProp prop = props[i];
            predicates[i] = prop.isReferenceList(TargetLevel.ENTITY) ?
                    context.table().exists(prop, target -> target.getId().eq(associatedId)) :
                    context.table().getAssociatedId(props[i]).eq(associatedId);
        }
        where(context.statement(), Predicate.or(predicates));
    }

    public void associatedIdNe(ImmutableProp prop, Object associatedId) {
        if (associatedId == null) {
            return;
        }
        Table<?> parentTable = context.statement().getTable();
        Table<?> table;
        MutableSubQueryImpl subQuery;
        if (parentTable instanceof TableImplementor<?>) {
            subQuery = new MutableSubQueryImpl(context.statement(), prop.getTargetType());
            table = subQuery.getTable();
        } else {
            TableProxy<?> proxy = TableProxies.fluent(prop.getTargetType().getJavaClass());
            subQuery = new MutableSubQueryImpl(context.statement(), proxy);
            table = proxy;
        }
        subQuery.where(table.inverseGetAssociatedId(prop).eq(parentTable.getId()));
        subQuery.where(table.getId().eq(associatedId));
        where(context.statement(), subQuery.notExists());
    }

    @SuppressWarnings("unchecked")
    public void associatedIdIn(ImmutableProp[] props, Collection<?> associatedIds) {
        if (associatedIds == null || associatedIds.isEmpty()) {
            return;
        }
        Predicate[] predicates = new Predicate[props.length];
        for (int i = predicates.length - 1; i >= 0; --i) {
            ImmutableProp prop = props[i];
            predicates[i] = prop.isReferenceList(TargetLevel.ENTITY) ?
                    context.table().exists(prop, target -> target.getId().in((Collection<Object>) associatedIds)) :
                    context.table().getAssociatedId(prop).in((Collection<Object>) associatedIds);
        }
        where(context.statement(), Predicate.or(predicates));
    }

    @SuppressWarnings("unchecked")
    public void associatedIdNotIn(ImmutableProp prop, Collection<?> associatedIds) {
        if (associatedIds == null || associatedIds.isEmpty()) {
            return;
        }
        Table<?> parentTable = context.statement().getTable();
        Table<?> table;
        MutableSubQueryImpl subQuery;
        if (parentTable instanceof TableImplementor<?>) {
            subQuery = new MutableSubQueryImpl(context.statement(), prop.getTargetType());
            table = subQuery.getTable();
        } else {
            TableProxy<?> proxy = TableProxies.fluent(prop.getTargetType().getJavaClass());
            subQuery = new MutableSubQueryImpl(context.statement(), proxy);
            table = proxy;
        }
        subQuery.where(table.inverseGetAssociatedId(prop).eq(parentTable.getId()));
        subQuery.where(table.getId().in((Collection<Object>) associatedIds));
        where(context.statement(), subQuery.notExists());
    }

    private static boolean isNullOrEmpty(Object value) {
        return value == null || value instanceof String && ((String) value).isEmpty();
    }

    private static LikeMode likeMode(boolean matchStart, boolean matchEnd) {
        if (matchStart && matchEnd) {
            return LikeMode.EXACT;
        }
        if (matchStart) {
            return LikeMode.START;
        }
        return matchEnd ? LikeMode.END : LikeMode.ANYWHERE;
    }

    private void where(AbstractMutableStatementImpl statement, Predicate... predicates) {
        PredicateCaptureFrame frame = captureFrames.peek();
        if (frame != null && statement == frame.statement) {
            frame.add(predicates);
        } else {
            statement.where(predicates);
        }
    }

    private static class PredicateCaptureFrame {

        private final AbstractMutableStatementImpl statement;

        private final List<Predicate> predicates = new ArrayList<>();

        PredicateCaptureFrame(AbstractMutableStatementImpl statement) {
            this.statement = statement;
        }

        void add(Predicate... predicates) {
            for (Predicate predicate : predicates) {
                if (predicate != null) {
                    this.predicates.add(predicate);
                }
            }
        }

        Predicate[] toArray() {
            return predicates.toArray(new Predicate[0]);
        }
    }

    private class Context {

        final Context parent;

        private AbstractMutableStatementImpl _statement;

        private boolean borrowParentStatement;

        final ImmutableProp prop;

        private Table<?> _table;

        private final PropExpression.Embedded<?> _embedded;

        private final Context baseContextForProps;

        Context(
                Context parent,
                AbstractMutableStatementImpl statement,
                ImmutableProp prop
        ) {
            this.parent = parent;
            this._statement = statement;
            this.prop = prop;
            if (parent == null) {
                if (!(statement.getTable() instanceof Table<?>)) {
                    throw new IllegalArgumentException(
                            "Cannot create predicate applier for the statement because " +
                                    "its table is not \"" +
                                    Table.class.getName() +
                                    "\""
                    );
                }
                this._table = statement.getTable();
            }
            this._embedded = null;
            this.baseContextForProps = null;
        }

        Context(
                Context parent,
                AbstractMutableStatementImpl statement,
                Table<?> table,
                ImmutableProp prop
        ) {
            this.parent = parent;
            this._statement = statement;
            this._table = table;
            this.prop = prop;
            this._embedded = null;
            this.baseContextForProps = parent;
        }

        Context(
                Context parent,
                boolean borrowParentStatement,
                ImmutableProp prop
        ) {
            this.parent = parent;
            this.borrowParentStatement = borrowParentStatement;
            this.prop = prop;
            this._embedded = null;
            this.baseContextForProps = null;
        }

        Context(
                Context parent,
                ImmutableProp prop
        ) {
            this.parent = parent;
            this._statement = parent.statement();
            this._table = parent.table();
            this.prop = prop;
            this._embedded = parent._embedded != null ?
                    (PropExpression.Embedded<?>) parent._embedded.get(prop) :
                    (PropExpression.Embedded<?>) _table.get(prop);
            this.baseContextForProps = null;
        }

        Context forType(Class<?> type) {
            Table<?> table = table();
            Class<?> currentJavaClass = table.getImmutableType().getJavaClass();
            if (type == currentJavaClass || type.isAssignableFrom(currentJavaClass)) {
                return this;
            }
            if (!currentJavaClass.isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The type \"" +
                                type.getName() +
                                "\" does not belong to the inheritance hierarchy of \"" +
                                currentJavaClass.getName() +
                                "\""
                );
            }
            return new Context(
                    this,
                    statement(),
                    tryTreatAs(table, type),
                    prop
            );
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Table<?> tryTreatAs(Table<?> table, Class<?> type) {
            ImmutableType immutableType = ImmutableType.get(type);
            if (table instanceof TableImplementor<?>) {
                return ((TableImplementor<?>) table).treatAsImplementor(immutableType, JoinType.LEFT);
            }
            if (table instanceof PolymorphicTable<?>) {
                TableProxy<?> proxy = TableProxies.fluent(type);
                if (proxy != null) {
                    return ((PolymorphicTable) table).tryTreatAs(proxy.getClass());
                }
            }
            throw new IllegalStateException(
                    "Cannot treat table \"" +
                            table +
                            "\" as \"" +
                            immutableType +
                            "\""
            );
        }

        AbstractMutableStatementImpl statement() {
            AbstractMutableStatementImpl statement = this._statement;
            if (statement == null) {
                if (borrowParentStatement) {
                    this._statement = statement = parent.statement();
                } else {
                    AbstractMutableStatementImpl parentStatement = parent.statement();
                    MutableSubQueryImpl subQuery = null;
                    if (parentStatement.getTable() instanceof TableProxy<?>) {
                        TableProxy<?> proxy = TableProxies.fluent(prop.getTargetType().getJavaClass());
                        if (proxy != null) {
                            subQuery = new MutableSubQueryImpl(parentStatement.getSqlClient(), proxy);
                        }
                    }
                    if (subQuery == null) {
                        subQuery = new MutableSubQueryImpl(parentStatement, prop.getTargetType());
                    }
                    subQuery.where(
                            parent.table().getId().eq(
                                    subQuery.<Table<?>>getTable().inverseGetAssociatedId(prop)
                            )
                    );
                    PredicateApplier.this.where(parentStatement, subQuery.exists());
                    this._statement = statement = subQuery;
                }
            }
            return statement;
        }

        Table<?> table() {
            Table<?> table = this._table;
            if (table == null) {
                if (prop.isReferenceList(TargetLevel.PERSISTENT)) {
                    table = statement().getTable();
                } else {
                    table = parent.table().join(prop);
                }
                this._table = table;
            }
            return table;
        }

        <X> Expression<X> get(ImmutableProp prop) {
            if (_embedded != null) {
                return _embedded.get(prop);
            }
            Context baseContext = baseContextForProps;
            if (baseContext != null &&
                    prop.getDeclaringType().isAssignableFrom(baseContext.table().getImmutableType())
            ) {
                return baseContext.get(prop);
            }
            return table().get(prop);
        }
    }
}
