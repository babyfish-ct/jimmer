package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.meta.impl.RedirectedProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

class TableImpl<E> extends AbstractDataManager<String, TableImplementor<?>> implements TableImplementor<E> {

    private static final String WEAK_JOIN_ERROR_MESSAGE =
            "Table join is forbidden in the implementation of \"" +
                    WeakJoin.class.getName() +
                    "\"";

    private final AbstractMutableStatementImpl statement;

    private final ImmutableType immutableType;

    private final TableImpl<?> parent;

    private final boolean isInverse;

    private final ImmutableProp joinProp;

    private final WeakJoinHandle weakJoinHandle;

    private JoinType joinType;

    private final String alias;

    private String middleTableAlias;

    public TableImpl(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType,
            TableImpl<?> parent,
            boolean isInverse,
            ImmutableProp joinProp,
            WeakJoinHandle weakJoinHandle,
            JoinType joinType
    ) {
        if (parent != null && immutableType instanceof AssociationType) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if ((parent == null) != (joinProp == null && weakJoinHandle == null)) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (parent != null && (joinProp == null) == (weakJoinHandle == null)) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (weakJoinHandle != null && isInverse) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }

        this.statement = statement;
        this.immutableType = immutableType;
        this.parent = parent;
        this.isInverse = isInverse;
        this.joinProp = joinProp;
        this.weakJoinHandle = weakJoinHandle;
        this.joinType = joinType;

        if (joinProp != null) {
            if (joinProp.getStorage() instanceof MiddleTable) {
                middleTableAlias = statement.getContext().allocateTableAlias();
            } else if (joinProp.getStorage() == null) {
                throw new AssertionError("Internal bug: Join property has not storage");
            }
        }
        alias = statement.getContext().allocateTableAlias();
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    public AbstractMutableStatementImpl getStatement() {
        return statement;
    }

    @Override
    public TableImplementor<?> getParent() {
        return parent;
    }

    @Override
    public boolean isInverse() {
        return isInverse;
    }

    @Override
    public ImmutableProp getJoinProp() {
        return joinProp;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (other.getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.<Expression<Object>>get(idPropName).eq(other.get(idPropName));
    }

    @Override
    public Predicate isNull() {
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNull();
    }

    @Override
    public Predicate isNotNull() {
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNotNull();
    }

    @Override
    public NumericExpression<Long> count() {
        return count(false);
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        return this.get(immutableType.getIdProp().getName()).count(distinct);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return (XE) PropExpressionImpl.of(this, immutableProp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT join(String prop) {
        return (XT) TableProxies.wrap(joinImplementor(prop));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        return (XT) TableProxies.wrap(joinImplementor(prop, joinType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        return (XT) TableProxies.wrap(joinImplementor(prop, joinType, treatedAs));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        return (XT) TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        return (XT) TableProxies.wrap(inverseJoinImplementor(prop, joinType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        return (XT) TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return (XT) TableProxies.wrap(inverseJoinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        return inverseJoin(ImmutableProps.join(targetTableType, backPropBlock));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
        return inverseJoin(ImmutableProps.join(targetTableType, backPropBlock), joinType);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop) {
        return joinImplementor(prop, JoinType.INNER);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType) {
        return joinImplementor(prop, joinType, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType, ImmutableType treatedAs) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (!immutableProp.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not association property of \"" +
                            this.immutableType +
                            "\""
            );
        }
        if (treatedAs != null) {
            immutableProp = RedirectedProp.target(immutableProp, treatedAs);
        }
        return (TableImplementor<X>) join0(false, immutableProp, joinType);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(ImmutableProp prop) {
        return inverseJoinImplementor(prop, JoinType.INNER);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(TypedProp.Association<?, ?> prop) {
        return inverseJoinImplementor(prop.unwrap(), JoinType.INNER);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return inverseJoinImplementor(prop.unwrap(), joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(ImmutableProp backProp, JoinType joinType) {
        if (backProp.getTargetType() != immutableType) {
            throw new IllegalArgumentException("'" + backProp + "' is not back association property");
        }
        if (!backProp.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("'" + backProp + "' is not declared in entity");
        }
        return (TableImplementor<X>) join0(true, backProp, joinType);
    }

    private TableImplementor<?> join0(
            boolean isInverse,
            ImmutableProp prop,
            JoinType joinType
    ) {
        if (prop.isTransient()) {
            throw new ExecutionException(
                    "Cannot join to '" +
                            prop.getName() +
                            "' because it's transient association"
            );
        }
        if (isInverse && prop instanceof AssociationProp) {
            throw new ExecutionException(
                    "Cannot join to '" + prop + "' by inverse mode because it's property of association entity"
            );
        }

        // TODO:
        // statement.validateMutable();

        String joinName;
        if (!isInverse) {
            joinName = prop.getName();
        } else if (prop.getOpposite() != null) {
            joinName = prop.getOpposite().getName();
        } else {
            joinName = "inverse(" + prop + ")";
        }

        if (prop.getMappedBy() != null) {
            return join1(
                    joinName,
                    !isInverse,
                    RedirectedProp.source(prop.getMappedBy(), prop.getTargetType()),
                    joinType
            );
        }
        return join1(joinName, isInverse, prop, joinType);
    }

    private TableImplementor<?> join1(
            String joinName,
            boolean isInverse,
            ImmutableProp prop,
            JoinType joinType
    ) {
        TableImpl<?> existing = (TableImpl<?>) getValue(joinName);
        if (existing != null) {
            if (existing.joinType != joinType) {
                existing.joinType = JoinType.INNER;
            }
            return existing;
        }
        TableImpl<?> newTable = new TableImpl<>(
                statement,
                isInverse ? prop.getDeclaringType() : prop.getTargetType(),
                this,
                isInverse,
                prop,
                null,
                joinType
        );
        putValue(joinName, newTable);
        return newTable;
    }

    @Override
    public <X> TableImplementor<X> weakJoinImplementor(Class<? extends WeakJoin<?, ?>> weakJoinType, JoinType joinType) {
        return weakJoinImplementor(WeakJoinHandle.of(weakJoinType), joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> weakJoinImplementor(WeakJoinHandle handle, JoinType joinType) {
        String joinName = "weak(" + handle.getWeakJoinType().getName() + ")";
        TableImpl<X> existing = (TableImpl<X>) getValue(joinName);
        if (existing != null) {
            if (existing.joinType != joinType) {
                existing.joinType = JoinType.INNER;
            }
            return existing;
        };
        TableImpl<X> newTable = new TableImpl<>(
                statement,
                handle.getTargetType(),
                this,
                isInverse,
                null,
                handle,
                joinType
        );
        putValue(joinName, newTable);
        return newTable;
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        if (fetcher == null) {
            return this;
        }
        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalArgumentException(
                    "Illegal fetcher type, current table is \"" +
                            this +
                            "\" but the fetcher type is \"" +
                            fetcher.getImmutableType() +
                            "\""
            );
        }
        return new FetcherSelectionImpl<>(this, fetcher);
    }

    @Override
    public TableEx<E> asTableEx() {
        return TableProxies.wrap(this);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(this, null);
    }

    @Override
    public void renderJoinAsFrom(SqlBuilder builder, RenderMode mode) {
        if (parent == null) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom can only be called base on joined tables");
        }
        if (mode == RenderMode.NORMAL) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom does not accept render mode ALL");
        }
        TableUsedState usedState = builder.getAstContext().getTableUsedState(this);
        if (usedState != TableUsedState.NONE) {
            renderSelf(builder, mode);
            if (mode == RenderMode.DEEPER_JOIN_ONLY) {
                for (TableImplementor<?> childTable : this) {
                    childTable.renderTo(builder);
                }
            }
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        TableUsedState usedState = builder.getAstContext().getTableUsedState(this);
        if (parent == null || usedState != TableUsedState.NONE) {
            renderSelf(builder, RenderMode.NORMAL);
            for (TableImplementor<?> childTable : this) {
                childTable.renderTo(builder);
            }
        }
    }

    private void renderSelf(SqlBuilder sqlBuilder, RenderMode mode) {
        if (isInverse) {
            renderInverseJoin(sqlBuilder, mode);
        } else if (joinProp != null || weakJoinHandle != null) {
            renderJoin(sqlBuilder, mode);
        } else {
            sqlBuilder
                    .sql(" from ")
                    .sql(immutableType.getTableName())
                    .sql(" as ")
                    .sql(alias);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderJoin(SqlBuilder builder, RenderMode mode) {

        if (weakJoinHandle != null) {
            if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
                Predicate predicate = weakJoinHandle.createPredicate(parent, this);
                builder
                        .sql(" ")
                        .sql(joinType.name().toLowerCase())
                        .sql(" join ")
                        .sql(immutableType.getTableName())
                        .sql(" as ")
                        .sql(alias)
                        .sql(" on ");
                if (predicate == null) {
                    builder.sql("1 = 1");
                } else {
                    ((Ast)predicate).renderTo(builder);
                }
            }
            return;
        }

        if (joinProp instanceof AssociationProp) {
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
                renderJoinImpl(
                        builder,
                        joinType,
                        parent.alias,
                        joinProp.<Column>getStorage().getName(),
                        immutableType.getTableName(),
                        alias,
                        immutableType.getIdProp().<Column>getStorage().getName(),
                        mode
                );
            }
            return;
        }

        TableImpl<?> parent = this.parent;
        JoinType joinType = this.joinType;
        MiddleTable middleTable = null;
        if (joinProp.getStorage() instanceof MiddleTable) {
            middleTable = joinProp.getStorage();
        }

        if (middleTable != null) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getJoinColumnName(),
                    mode
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        middleTableAlias,
                        middleTable.getTargetJoinColumnName(),
                        immutableType.getTableName(),
                        alias,
                        ((Column)immutableType.getIdProp().getStorage()).getName(),
                        RenderMode.NORMAL
                );
            }
        } else if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    ((Column)joinProp.getStorage()).getName(),
                    immutableType.getTableName(),
                    alias,
                    ((Column)immutableType.getIdProp().getStorage()).getName(),
                    mode
            );
        }
    }

    private void renderInverseJoin(SqlBuilder sqlBuilder, RenderMode mode) {

        TableImpl<?> parent = this.parent;
        JoinType joinType = this.joinType;
        MiddleTable middleTable = null;
        if (joinProp.getStorage() instanceof MiddleTable) {
            middleTable = joinProp.getStorage();
        }

        if (middleTable != null) {
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getTargetJoinColumnName(),
                    mode
            );
            if (sqlBuilder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        sqlBuilder,
                        joinType,
                        middleTableAlias,
                        middleTable.getJoinColumnName(),
                        immutableType.getTableName(),
                        alias,
                        ((Column)immutableType.getIdProp().getStorage()).getName(),
                        RenderMode.NORMAL
                );
            }
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    immutableType.getTableName(),
                    alias,
                    ((Column)joinProp.getStorage()).getName(),
                    mode
            );
        }
    }

    private void renderJoinImpl(
            SqlBuilder sqlBuilder,
            JoinType joinType,
            String previousAlias,
            String previousColumnName,
            String newTableName,
            String newAlias,
            String newColumnName,
            RenderMode mode
    ) {
        if (mode != RenderMode.NORMAL && joinType != JoinType.INNER) {
            throw new AssertionError("Internal bug: outer join cannot be accepted by abnormal render mode");
        }
        switch (mode) {
            case NORMAL:
                sqlBuilder
                        .sql(" ")
                        .sql(joinType.name().toLowerCase())
                        .sql(" join ")
                        .sql(newTableName)
                        .sql(" as ")
                        .sql(newAlias)
                        .sql(" on ");
                break;
            case FROM_ONLY:
                sqlBuilder
                        .sql(newTableName)
                        .sql(" as ")
                        .sql(newAlias);
                break;
        }
        if (mode == RenderMode.NORMAL || mode == RenderMode.WHERE_ONLY) {
            sqlBuilder
                    .sql(previousAlias)
                    .sql(".")
                    .sql(previousColumnName)
                    .sql(" = ")
                    .sql(newAlias)
                    .sql(".")
                    .sql(newColumnName);
        }
    }

    @Override
    public void renderSelection(ImmutableProp prop, SqlBuilder builder) {
        if (prop.isId() && joinProp != null) {
            MiddleTable middleTable;
            if (joinProp.getStorage() instanceof MiddleTable) {
                middleTable = joinProp.getStorage();
            } else {
                middleTable = null;
            }
            boolean isInverse = this.isInverse;
            if (middleTable != null) {
                builder.sql(middleTableAlias).sql(".");
                if (isInverse) {
                    builder.sql(middleTable.getJoinColumnName());
                } else {
                    builder.sql(middleTable.getTargetJoinColumnName());
                }
                return;
            }
            if (!isInverse) {
                builder
                        .sql(parent.alias)
                        .sql(".")
                        .sql(((Column)joinProp.getStorage()).getName());
                return;
            }
        }
        builder.sql(alias).sql(".").sql(((Column)prop.getStorage()).getName());
    }

    @Override
    public String toString() {
        String text;
        if (joinProp == null) {
            text = immutableType.getJavaClass().getSimpleName();
        } else if (isInverse) {
            ImmutableProp opposite = joinProp.getOpposite();
            if (opposite != null) {
                text = parent.toString() + '.' + opposite.getName();
            } else {
                text = parent + "[← " + joinProp + ']';
            }
        } else {
            return parent.toString() + '.' + joinProp.getName();
        }
        if (joinType == JoinType.INNER) {
            return text;
        }
        return text + '(' + joinType.name().toLowerCase() + ')';
    }

    @Override
    public TableRowCountDestructive getDestructive() {
        if (joinProp == null) {
            return TableRowCountDestructive.NONE;
        }
        ImmutableProp prop;
        if (isInverse) {
            prop = joinProp.getOpposite();
            if (prop == null) {
                return TableRowCountDestructive.BREAK_REPEATABILITY;
            }
        } else {
            prop = joinProp;
        }
        if (prop.isReferenceList(TargetLevel.ENTITY)) {
            return TableRowCountDestructive.BREAK_REPEATABILITY;
        }
        if (prop.isNullable() && joinType != JoinType.LEFT) {
            return TableRowCountDestructive.BREAK_ROW_COUNT;
        }
        return TableRowCountDestructive.NONE;
    }
}