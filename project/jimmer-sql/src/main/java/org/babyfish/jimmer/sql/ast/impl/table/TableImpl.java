package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

class TableImpl<E> extends AbstractDataManager<TableImpl.Key, TableLikeImplementor<?>>implements TableImplementor<E> {

    final AbstractMutableStatementImpl statement;

    final ImmutableType immutableType;

    final TableImpl<?> parent;

    final boolean isInverse;

    final ImmutableProp joinProp;

    final WeakJoinHandle weakJoinHandle;

    private final JoinType joinType;

    private final boolean fetch;

    @Nullable
    private final BaseTableOwner baseTableOwner;

    private Map<BaseTableOwner, TableImpl<E>> neighborMap;

    private RealTableImpl realTable;

    private boolean hasBaseTable;

    public TableImpl(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType,
            TableImpl<?> parent,
            boolean isInverse,
            ImmutableProp joinProp,
            WeakJoinHandle weakJoinHandle,
            JoinType joinType,
            boolean fetch
    ) {
        if (statement == null) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
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
        this.fetch = fetch;
        this.baseTableOwner = parent != null ? parent.baseTableOwner : null;
    }

    private TableImpl(
            TableImpl<E> base,
            TableImpl<?> parent,
            @Nullable BaseTableOwner baseTableOwner
    ) {
        this.statement = base.statement;
        this.immutableType = base.immutableType;
        this.parent = parent;
        this.isInverse = base.isInverse;
        this.joinProp = base.joinProp;
        this.weakJoinHandle = base.weakJoinHandle;
        this.joinType = base.joinType;
        this.fetch = base.fetch;
        this.neighborMap = base.neighborMap;
        this.baseTableOwner = baseTableOwner;
    }

    @Override
    public final ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public final AbstractMutableStatementImpl getStatement() {
        return statement;
    }

    @Override
    public final TableImplementor<?> getParent() {
        return parent;
    }

    @Override
    public final boolean isInverse() {
        return isInverse;
    }

    @Override
    public final boolean isEmpty(java.util.function.Predicate<TableLikeImplementor<?>> filter) {
        if (isEmpty()) {
            return true;
        }
        if (filter == null) {
            return false;
        }
        for (TableLikeImplementor<?> childTable : this) {
            if (filter.test(childTable)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean isRemote() {
        return joinProp != null && joinProp.isRemote();
    }

    @Override
    public final ImmutableProp getJoinProp() {
        return joinProp;
    }

    @Override
    public final WeakJoinHandle getWeakJoinHandle() {
        return weakJoinHandle;
    }

    @Override
    public final JoinType getJoinType() {
        return joinType;
    }

    @Override
    public final RealTableImpl realTable(JoinTypeMergeScope scope) {
        return realTable0(scope, parent);
    }

    private RealTableImpl realTable0(
            JoinTypeMergeScope scope,
            TableImpl<?> parent
    ) {
        RealTableImpl realTable = this.realTable;
        if (realTable == null) {
            if (parent == null) {
                realTable = new RealTableImpl(this);
            } else {
                realTable = parent.realTable(scope).child(scope, this);
            }
            this.realTable = realTable;
        }
        return realTable;
    }

    public RealTableImpl tryGetRealTable() {
        return realTable;
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (other.getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        ImmutableProp idProp = immutableType.getIdProp();
        return this.get(idProp).eq(other.get(idProp));
    }

    @Override
    public Predicate eq(Example<E> example) {
        return ((ExampleImpl<E>)example).toPredicate(this);
    }

    @Override
    public Predicate eq(E example) {
        return eq(Example.of(example));
    }

    @Override
    public Predicate eq(View<E> view) {
        return eq(Example.of(view));
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
        if (immutableType instanceof AssociationType) {
            return this.get(((AssociationType)immutableType).getSourceProp().getName()).count();
        }
        return this.get(immutableType.getIdProp().getName()).count(distinct);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> PropExpression<X> get(String prop) {
        return get(immutableType.getProp(prop));
    }

    @Override
    public <X> PropExpression<X> get(ImmutableProp prop) {
        return get(prop, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> PropExpression<X> get(ImmutableProp prop, boolean rawId) {
        if (isRemote() && immutableType.getIdProp() != prop) {
            throw new IllegalArgumentException(
                    "The current table is remote so that only the id property \"" +
                            immutableType.getIdProp() +
                            "\" can be accessed"
            );
        }
        if (prop.getDeclaringType() != immutableType) {
            if (!prop.getDeclaringType().isAssignableFrom(immutableType)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current type \"" +
                                immutableType +
                                "\""
                );
            }
            prop = immutableType.getProp(prop.getName());
        }
        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        if (idViewBaseProp != null && idViewBaseProp.isReference(TargetLevel.ENTITY)) {
            return joinImplementor(idViewBaseProp.getName(), idViewBaseProp.isNullable() ? JoinType.LEFT : JoinType.INNER)
                    .get(idViewBaseProp.getTargetType().getIdProp(), true);
        }
        return (PropExpression<X>) PropExpressionImpl.of(this, prop, rawId);
    }

    @Override
    public <X> PropExpression<X> getId() {
        return get(immutableType.getIdProp());
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return getAssociatedId(immutableProp);
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(ImmutableProp prop) {
        TableImplementor<?> joinedTable = joinImplementor(prop, prop.isNullable() ? JoinType.LEFT : JoinType.INNER);
        return joinedTable.get(joinedTable.getImmutableType().getIdProp(), true);
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop) {
        return TableProxies.wrap(joinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        return TableProxies.wrap(joinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType) {
        return TableProxies.wrap(joinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        return TableProxies.wrap(joinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs) {
        return TableProxies.wrap(joinImplementor(prop, joinType, treatedAs));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        return TableProxies.wrap(joinImplementor(prop, joinType, treatedAs));
    }

    @Override
    public <X> PropExpression<X> inverseGetAssociatedId(ImmutableProp prop) {
        ImmutableProp oppositeProp = prop.getOpposite();
        TableImplementor<?> joinedTable = inverseJoinImplementor(
                prop,
                oppositeProp != null && oppositeProp.isNullable() ? JoinType.LEFT : JoinType.INNER
        );
        return joinedTable.get(joinedTable.getImmutableType().getIdProp(), true);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        return TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        return TableProxies.wrap(inverseJoinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        return TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return TableProxies.wrap(inverseJoinImplementor(prop, joinType));
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
        return joinImplementor(immutableType.getProp(prop), JoinType.INNER, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop) {
        return joinImplementor(prop, JoinType.INNER, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType) {
        return joinImplementor(immutableType.getProp(prop), joinType, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType) {
        return joinImplementor(prop, joinType, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType, ImmutableType treatedAs) {
        return joinImplementor(immutableType.getProp(prop), joinType, treatedAs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs) {
        if (prop.getDeclaringType() != immutableType) {
            if (!prop.getDeclaringType().isAssignableFrom(immutableType)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current type \"" +
                                immutableType +
                                "\""
                );
            }
            prop = immutableType.getProp(prop.getName());
        }
        ImmutableProp manyToManyViewBaseProp = prop.getManyToManyViewBaseProp();
        if (manyToManyViewBaseProp != null) {
            return (TableImplementor<X>)
                    ((TableImpl<Object>)join0(false, manyToManyViewBaseProp, joinType))
                            .join0(false, prop.getManyToManyViewBaseDeeperProp(), joinType);
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            if (isRemote()) {
                throw new IllegalStateException(
                        "The current table is remote so that join is not supported"
                );
            }
            if (prop.isTransient()) {
                throw new IllegalArgumentException(
                        "\"" + prop + "\" cannot be transient"
                );
            }
            if (prop.isRemote() && prop.getMappedBy() != null) {
                throw new IllegalArgumentException(
                        "\"" + prop + "\" cannot be remote and reversed(with `mappedBy`)"
                );
            }
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not association property of \"" +
                            this.immutableType +
                            "\""
            );
        }
        return (TableImplementor<X>) join0(false, prop, joinType);
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
        ImmutableProp manyToManyViewBaseProp = backProp.getManyToManyViewBaseProp();
        if (manyToManyViewBaseProp != null) {
            return (TableImplementor<X>)
                    ((TableImpl<?>)join0(true, backProp.getManyToManyViewBaseDeeperProp(), joinType))
                            .join0(true, manyToManyViewBaseProp, joinType);
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
                    prop.getMappedBy(),
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
        Key key = new Key(joinName, joinType, null, false);
        TableImpl<?> joinedTable = (TableImpl<?>) getValue(key);
        if (joinedTable != null) {
            return joinedTable;
        }
        joinedTable = new TableImpl<>(
                statement,
                isInverse ? prop.getDeclaringType() : prop.getTargetType(),
                this,
                isInverse,
                prop,
                null,
                joinType,
                false
        );
        putValue(key, joinedTable);
        return joinedTable;
    }

    @Override
    public <X> TableImplementor<X> weakJoinImplementor(Class<? extends WeakJoin<?, ?>> weakJoinType, JoinType joinType) {
        return weakJoinImplementor(WeakJoinHandle.of(weakJoinType), joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> weakJoinImplementor(
            Class<? extends Table<?>> targetTableType,
            JoinType joinType,
            WeakJoin<?, ?> weakJoinLambda
    ) {
        return weakJoinImplementor(
                WeakJoinHandle.of(
                        JWeakJoinLambdaFactory.get(weakJoinLambda),
                        true,
                        true,
                        (WeakJoin<TableLike<?>, TableLike<?>>) weakJoinLambda
                ),
                joinType
        );
    }

    @Override
    public <X> TableImplementor<X> weakJoinImplementor(WeakJoinHandle handle, JoinType joinType) {
        return new TableImpl<>(
                statement,
                ((WeakJoinHandle.EntityTableHandle)handle).getTargetType(),
                this,
                isInverse,
                null,
                handle,
                joinType,
                false
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X extends BaseTable> X weakJoinImplementor(X targetBaseTable, WeakJoinHandle handle, JoinType joinType) {
        return (X) BaseTableSymbols.of((BaseTableSymbol) targetBaseTable, this, handle, joinType, null);
    }

    @Override
    public TableImplementor<?> joinFetchImplementor(ImmutableProp prop, BaseTableOwner baseTableOwner) {
        if (!prop.isAssociation(TargetLevel.PERSISTENT) || prop.isReferenceList(TargetLevel.PERSISTENT)) {
            throw new IllegalArgumentException(
                    "Cannot join fetch \"" +
                            prop +
                            "\" because it is not decorated by \"@" +
                            ManyToOne.class.getName() +
                            "\" or \"@" +
                            OneToOne.class.getName() +
                            "\""
            );
        }
        JoinType joinType = prop.isNullable() ? JoinType.LEFT : JoinType.INNER;

        Key key = new Key(prop.getName(), joinType, null, true);
        TableImpl<?> joinedTable = (TableImpl<?>) getValue(key);
        if (joinedTable != null) {
            return joinedTable;
        }
        ImmutableProp mappedBy = prop.getMappedBy();
        joinedTable = new TableImpl<>(
                statement,
                prop.getTargetType(),
                this,
                mappedBy != null,
                mappedBy != null ? mappedBy : prop,
                null,
                joinType,
                true
        );
        putValue(key, joinedTable);
        return joinedTable;
    }

    @Override
    public boolean hasBaseTable() {
        return hasBaseTable;
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        if (fetcher == null) {
            return this;
        }
        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalArgumentException(
                    "Illegal fetcher type, the entity type of current table is \"" +
                            this +
                            "\" but the fetcher type is \"" +
                            fetcher.getImmutableType() +
                            "\""
            );
        }
        return new FetcherSelectionImpl<>(this, fetcher);
    }

    @Override
    public <V extends View<E>> Selection<V> fetch(Class<V> viewType) {
        if (viewType == null) {
            throw new IllegalArgumentException("The argument `staticType` cannot be null");
        }
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        Fetcher<E> fetcher = metadata.getFetcher();
        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalArgumentException(
                    "Illegal fetcher type, the entity type of current table is \"" +
                            this +
                            "\" but the static type is based on \"" +
                            fetcher.getImmutableType() +
                            "\""
            );
        }
        return new FetcherSelectionImpl<>(this, fetcher, metadata.getConverter());
    }

    @Override
    public TableEx<E> asTableEx() {
        return TableProxies.wrap(this);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(realTable(visitor.getAstContext()), null, false);
    }

    @Override
    public void renderJoinAsFrom(SqlBuilder builder, RenderMode mode) {
        realTable(builder.getAstContext()).renderJoinAsFrom(builder, mode);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        AstContext astContext;
        if (builder instanceof SqlBuilder) {
            astContext = ((SqlBuilder)builder).getAstContext();
        } else {
            astContext = null;
        }
        realTable(astContext).renderTo(builder, false);
    }

    @Override
    public void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    ) {
        JoinTypeMergeScope scope;
        if (builder instanceof SqlBuilder) {
            scope = ((SqlBuilder)builder).getAstContext().getJoinTypeMergeScope();
        } else {
            scope = null;
        }
        realTable(scope).renderSelection(
                prop,
                rawId,
                builder,
                optionalDefinition,
                withPrefix,
                asBlock
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();;
        if (parent == null) {
            builder.append(immutableType.getJavaClass().getSimpleName());
        } else {
            builder.append(parent);
            if (isInverse) {
                ImmutableProp opposite = joinProp.getOpposite();
                if (opposite != null) {
                    builder.append('.').append(opposite.getName());
                } else {
                    builder.append("[← ").append(joinProp.getName()).append(']');
                }
            } else {
                if (joinProp != null) {
                    builder.append('.').append(joinProp.getName());
                }
                if (weakJoinHandle != null) {
                    builder.append('[').append(weakJoinHandle).append(']');
                }
            }
        }
        JoinType joinType = this.joinType;
        if (joinType != JoinType.INNER) {
            builder.append('(').append(joinType.name().toLowerCase()).append(')');
        }
        if (baseTableOwner != null) {
            builder.append(':').append(baseTableOwner);
        }
        return builder.toString();
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
        if (prop.isReferenceList(TargetLevel.PERSISTENT)) {
            return TableRowCountDestructive.BREAK_REPEATABILITY;
        }
        if (prop.isNullable() && joinType != JoinType.LEFT) {
            return TableRowCountDestructive.BREAK_ROW_COUNT;
        }
        return TableRowCountDestructive.NONE;
    }

    @Override
    public <XT extends Table<?>> Predicate exists(String prop, Function<XT, Predicate> block) {
        ImmutableProp joinProp = immutableType.getProps().get(prop);
        if (joinProp == null) {
            throw new IllegalArgumentException(
                    "Illegal property name \"" +
                            prop +
                            "\", there is no such property \"" +
                            immutableType +
                            "\""
            );
        }
        return exists(joinProp, block);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> Predicate exists(ImmutableProp prop, Function<XT, Predicate> block) {
        return new AssociatedPredicate(
                this,
                prop,
                (Function<Table<?>, Predicate>) block
        );
    }

    @Override
    public boolean hasVirtualPredicate() {
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Nullable
    @Override
    public  BaseTableOwner getBaseTableOwner() {
        return baseTableOwner;
    }

    @Override
    public TableImpl<E> baseTableOwner(@Nullable BaseTableOwner baseTableOwner) {
        if (Objects.equals(this.baseTableOwner, baseTableOwner)) {
            return this;
        }
        Map<BaseTableOwner, TableImpl<E>> neighborMap = this.neighborMap;
        if (neighborMap == null) {
            neighborMap = new HashMap<>();
            neighborMap.put(this.baseTableOwner, this);
            this.neighborMap = neighborMap;
        }
        TableImpl<E> neighbor = neighborMap.get(baseTableOwner);
        if (neighbor != null) {
            return neighbor;
        }
        TableImpl<?> neighborParent = null;
        if (parent != null && baseTableOwner != null) {
            neighborParent = parent.baseTableOwner(baseTableOwner);
        }
        neighbor = new TableImpl<>(this, neighborParent, baseTableOwner);
        neighborMap.put(baseTableOwner, neighbor);
        return neighbor;
    }

    @SuppressWarnings("unchecked")
    public <T extends TableLikeImplementor<?>> T computedIfAbsent(Key key, Supplier<T> tableLikeSupplier) {
        T child = (T) getValue(key);
        if (child == null) {
            child = tableLikeSupplier.get();
            putValue(key, child);
        }
        return child;
    }

    @Override
    public void setHasBaseTable() {
        if (!hasBaseTable) {
            hasBaseTable = true;
            if (parent != null) {
                parent.setHasBaseTable();
            }
        }
    }

    static class Key {

        final String joinName;

        final JoinType joinType;

        final WeakJoinHandle weakJoinHandle;

        final boolean fetch;

        Key(String joinName, JoinType joinType, WeakJoinHandle weakJoinHandle, boolean fetch) {
            this.joinName = joinName;
            this.joinType = joinType;
            this.weakJoinHandle = weakJoinHandle;
            this.fetch = fetch;
        }

        @Override
        public int hashCode() {
            int result = joinName.hashCode();
            result = 31 * result + joinType.hashCode();
            result = 32 * result + Boolean.hashCode(fetch);
            result = 31 * result + Objects.hashCode(weakJoinHandle);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key other = (Key) o;

            if (fetch != other.fetch) {
                return false;
            }
            if (!joinName.equals(other.joinName)) {
                return false;
            }
            if (joinType != other.joinType) {
                return false;
            }
            return Objects.equals(weakJoinHandle, other.weakJoinHandle);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "joinName='" + joinName + '\'' +
                    ", joinType = " + joinType +
                    ", weakJoinHandle=" + weakJoinHandle +
                    ", fetch = " + fetch +
                    "}";
        }
    }
}