package org.babyfish.jimmer.sql.ast.impl.table;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public class WeakJoinHandle {

    private static final String JOIN_ERROR_REASON =
            "it is forbidden in the implementation of \"" +
                    WeakJoin.class.getName() +
                    "\"";

    private static final StaticCache<Class<? extends WeakJoin<?, ?>>, WeakJoinHandle> CACHE =
            new StaticCache<>(WeakJoinHandle::create, false);

    private final ImmutableType sourceType;

    private final ImmutableType targetType;

    private final boolean hasSourceWrapper;

    private final boolean hasTargetWrapper;

    private final WeakJoin<Table<?>, Table<?>> weakJoin;

    private WeakJoinHandle(
            ImmutableType sourceType,
            ImmutableType targetType,
            boolean hasSourceWrapper,
            boolean hasTargetWrapper,
            WeakJoin<Table<?>, Table<?>> weakJoin
    ) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.hasSourceWrapper = hasSourceWrapper;
        this.hasTargetWrapper = hasTargetWrapper;
        this.weakJoin = weakJoin;
    }

    public ImmutableType getSourceType() {
        return sourceType;
    }

    public ImmutableType getTargetType() {
        return targetType;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends WeakJoin<?, ?>> getWeakJoinType() {
        return (Class<? extends WeakJoin<?, ?>>) weakJoin.getClass();
    }

    @Override
    public String toString() {
        return "WeakJoinHandle{" +
                "weakJoin=" + weakJoin +
                '}';
    }

    public Predicate createPredicate(TableImplementor<?> source, TableImplementor<?> target) {
        if (weakJoin instanceof CustomWeakJoinTableExporter) {
            return weakJoin.on(
                    source instanceof TableProxy<?> ?
                            ((TableProxy<?>)source).__unwrap() :
                            source,
                    target instanceof TableProxy<?> ?
                            ((TableProxy<?>)target).__unwrap() :
                            target
            );
        }
        return weakJoin.on(
                hasSourceWrapper ?
                        ((TableProxy<?>)TableProxies.wrap(source)).__disableJoin(JOIN_ERROR_REASON) :
                        new UntypedJoinDisabledTableProxy<>(source, JOIN_ERROR_REASON),
                hasTargetWrapper ?
                        ((TableProxy<?>)TableProxies.wrap(target)).__disableJoin(JOIN_ERROR_REASON) :
                        new UntypedJoinDisabledTableProxy<>(target, JOIN_ERROR_REASON)
        );
    }

    public static WeakJoinHandle of(Class<? extends WeakJoin<?, ?>> weakJoinType) {
        return CACHE.get(weakJoinType);
    }

    @SuppressWarnings("unchecked")
    private static WeakJoinHandle create(Class<? extends WeakJoin<?, ?>> weakJoinType) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(weakJoinType, WeakJoin.class);
        if (typeArguments == null || typeArguments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Illegal class \"" + weakJoinType.getName() + "\", generic arguments are missing"
            );
        }
        Type sourceTableType = typeArguments.get(WeakJoin.class.getTypeParameters()[0]);
        Type targetTableType = typeArguments.get(WeakJoin.class.getTypeParameters()[1]);
        Type sourceType = TypeUtils.getTypeArguments(sourceTableType, Table.class).values().iterator().next();
        Type targetType = TypeUtils.getTypeArguments(targetTableType, Table.class).values().iterator().next();
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
        Constructor<WeakJoin<Table<?>, Table<?>>> constructor;
        try {
            constructor = (Constructor<WeakJoin<Table<?>, Table<?>>>) weakJoinType.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "No default constructor can be found in \"" +
                            weakJoinType.getName() +
                            "\""
            );
        }
        constructor.setAccessible(true);
        WeakJoin<Table<?>, Table<?>> weakJoin;
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
        return new WeakJoinHandle(
                ImmutableType.get((Class<?>) sourceType),
                ImmutableType.get((Class<?>) targetType),
                hasSourceWrapper,
                hasTargetWrapper,
                weakJoin
        );
    }
}
