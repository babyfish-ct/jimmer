package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.sql.ast.impl.table.MapperSelection;
import org.babyfish.jimmer.sql.ast.mapper.BaseTableMapper;
import org.babyfish.jimmer.sql.ast.mapper.TypedTupleMapper;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

import java.lang.reflect.Field;
import java.util.function.Function;

class BaseTables {

    private BaseTables() {}

    private static final ClassCache<Function<BaseTableQueryImplementor<?, ?>, BaseTable<?>>> FACTORY_CACHE =
            new ClassCache<>(BaseTables::createFactory);

    @SuppressWarnings("unchecked")
    public static <B extends BaseTable<?>> B create(BaseTableQueryImplementor<?, ?> query) {
        MapperSelection<?> selection = (MapperSelection<?>) query.getSelections().get(0);
        BaseTableMapper<?, ?> mapper = (BaseTableMapper<?, ?>) selection.getMapper();
        Function<BaseTableQueryImplementor<?, ?>, BaseTable<?>> factory =
                FACTORY_CACHE.get(mapper.getBaseTableType());
        return (B) factory.apply(query);
    }

    @SuppressWarnings("unchecked")
    private static Function<BaseTableQueryImplementor<?, ?>, BaseTable<?>> createFactory(
            Class<?> baseTableType
    ) {
        Field field;
        try {
            field = baseTableType.getDeclaredField("FACTORY");
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("Internal bug", ex);
        }
        field.setAccessible(true);
        try {
            return (Function<BaseTableQueryImplementor<?, ?>, BaseTable<?>>) field.get(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }
}
