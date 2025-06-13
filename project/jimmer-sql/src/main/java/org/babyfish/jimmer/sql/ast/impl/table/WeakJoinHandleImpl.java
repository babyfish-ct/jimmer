package org.babyfish.jimmer.sql.ast.impl.table;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;

import java.lang.reflect.*;
import java.util.Map;

abstract class WeakJoinHandleImpl implements WeakJoinHandle {

    private static final String JOIN_ERROR_REASON =
            "it is forbidden in the implementation of \"" +
                    WeakJoin.class.getName() +
                    "\"";

    private static final ClassCache<WeakJoinHandle> CACHE =
            new ClassCache<>(WeakJoinHandleImpl::create, false);

    final WeakJoin<TableLike<?>, TableLike<?>> weakJoin;

    WeakJoinHandleImpl(WeakJoin<TableLike<?>, TableLike<?>> weakJoin) {
        this.weakJoin = weakJoin;
    }

    static WeakJoinHandle get(Class<? extends WeakJoin<?, ?>> weakJoinType) {
        return CACHE.get(weakJoinType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends WeakJoin<?, ?>> getWeakJoinType() {
        return (Class<? extends WeakJoin<?, ?>>) weakJoin.getClass();
    }

    @SuppressWarnings("unchecked")
    private static WeakJoinHandle create(Class<?> weakJoinType) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(weakJoinType, WeakJoin.class);
        if (typeArguments == null || typeArguments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Illegal class \"" + weakJoinType.getName() + "\", generic arguments are missing"
            );
        }
        Type sourceTableType = typeArguments.get(WeakJoin.class.getTypeParameters()[0]);
        Type targetTableType = typeArguments.get(WeakJoin.class.getTypeParameters()[1]);
        if (isBaseTable(sourceTableType) && isBaseTable(targetTableType)) {
            return new BaseTableHandleImpl(createWeakJoin(weakJoinType));
        }
        Type sourceType = TypeUtils.getTypeArguments(sourceTableType, TableLike.class).values().iterator().next();
        Type targetType = TypeUtils.getTypeArguments(targetTableType, TableLike.class).values().iterator().next();
        if (sourceType instanceof TypeVariable<?>) {
            sourceType = typeArguments.get((TypeVariable<?>) sourceType);
        }
        if (targetType instanceof TypeVariable<?>) {
            targetType = typeArguments.get((TypeVariable<?>) targetType);
        }
        if (!(sourceType instanceof Class<?>) || !((Class<?>)sourceType).isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                    "Illegal class \"" +
                            weakJoinType.getName() +
                            "\", the source type is not entity"
            );
        }
        if (!(targetType instanceof Class<?>) || !((Class<?>)targetType).isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                    "Illegal class \"" +
                            weakJoinType.getName() +
                            "\", the target type is not entity"
            );
        }
        boolean hasSourceWrapper = TableProxies.tableWrapperClass((Class<?>)sourceType) != null;
        boolean hasTargetWrapper = TableProxies.tableWrapperClass((Class<?>)targetType) != null;
        return new EntityTableHandleImpl(
                ImmutableType.get((Class<?>) sourceType),
                ImmutableType.get((Class<?>) targetType),
                hasSourceWrapper,
                hasTargetWrapper,
                createWeakJoin(weakJoinType)
        );
    }

    @SuppressWarnings("unchecked")
    static WeakJoin<TableLike<?>, TableLike<?>> createWeakJoin(Class<?> weakJoinType) {
        Constructor<WeakJoin<TableLike<?>, TableLike<?>>> constructor;
        try {
            constructor = (Constructor<WeakJoin<TableLike<?>, TableLike<?>>>) weakJoinType.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "No default constructor can be found in \"" +
                            weakJoinType.getName() +
                            "\""
            );
        }
        constructor.setAccessible(true);
        WeakJoin<TableLike<?>, TableLike<?>> weakJoin;
        try {
            weakJoin = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalArgumentException(
                    "Cannot create instance of \"" +
                            weakJoinType.getName() +
                            "\"",
                    ex
            );
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(
                    "Cannot create instance of \"" +
                            weakJoinType.getName() +
                            "\"",
                    ex.getTargetException()
            );
        }
        return weakJoin;
    }

    private static boolean isBaseTable(Type type) {
        if (type instanceof Class<?>) {
            return BaseTable.class.isAssignableFrom((Class<?>) type);
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        return BaseTable.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
    }

    private static final class EntityTableHandleImpl extends WeakJoinHandleImpl implements WeakJoinHandle.EntityTableHandle {

        private final ImmutableType sourceType;

        private final ImmutableType targetType;

        private final boolean hasSourceWrapper;

        private final boolean hasTargetWrapper;

        private EntityTableHandleImpl(
                ImmutableType sourceType,
                ImmutableType targetType,
                boolean hasSourceWrapper,
                boolean hasTargetWrapper,
                WeakJoin<TableLike<?>, TableLike<?>> weakJoin
        ) {
            super(weakJoin);
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.hasSourceWrapper = hasSourceWrapper;
            this.hasTargetWrapper = hasTargetWrapper;
        }

        @Override
        public ImmutableType getSourceType() {
            return sourceType;
        }

        @Override
        public ImmutableType getTargetType() {
            return targetType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Predicate createPredicate(
                TableLike<?> source,
                TableLike<?> target,
                AbstractMutableStatementImpl statement
        ) {
            if (weakJoin instanceof KWeakJoinImplementor) {
                KWeakJoinImplementor<Object, Object> implementor =
                        (KWeakJoinImplementor<Object, Object>) weakJoin;
                return implementor.on(
                        source instanceof TableProxy<?> ?
                                (Table<Object>)((TableProxy<?>)source).__unwrap() :
                                (TableLike<Object>)source,
                        target instanceof TableProxy<?> ?
                                (Table<Object>)((TableProxy<?>)target).__unwrap() :
                                (TableLike<Object>)target,
                        statement
                );
            }
            return weakJoin.on(
                    hasSourceWrapper ?
                            ((TableProxy<?>)TableProxies.wrap((Table<?>) source)).__disableJoin(JOIN_ERROR_REASON) :
                            new UntypedJoinDisabledTableProxy<>((TableImplementor<?>) source, JOIN_ERROR_REASON),
                    hasTargetWrapper ?
                            ((TableProxy<?>)TableProxies.wrap((Table<?>) target)).__disableJoin(JOIN_ERROR_REASON) :
                            new UntypedJoinDisabledTableProxy<>((TableImplementor<?>) target, JOIN_ERROR_REASON)
            );
        }

        @Override
        public String toString() {
            return "WeakEntityTableJoinHandle{" +
                    "weakJoin=" + weakJoin +
                    '}';
        }
    }

    private static class BaseTableHandleImpl extends WeakJoinHandleImpl implements WeakJoinHandle.BaseTableHandle {

        BaseTableHandleImpl(
                WeakJoin<TableLike<?>, TableLike<?>> weakJoin
        ) {
            super(weakJoin);
        }

        @Override
        public Predicate createPredicate(TableLike<?> source, TableLike<?> target, AbstractMutableStatementImpl statement) {
            return weakJoin.on(source, target);
        }

        @Override
        public String toString() {
            return "WeakBaseTableJoinHandle{" +
                    "weakJoin=" + weakJoin +
                    '}';
        }
    }
}
