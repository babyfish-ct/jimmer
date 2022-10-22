package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;
import org.babyfish.jimmer.sql.ast.table.spi.TableWrapper;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class TableWrappers {

    private static final StaticCache<Class<?>, Constructor<?>> CACHE =
            new StaticCache<>(TableWrappers::createConstructor);

    private TableWrappers() {}

    public static <T extends TableEx<?>> T wrap(TableImplementor<?> table) {
        return wrap(table, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TableEx<?>> T wrap(Table<?> table, String joinDisabledReason) {
        if (table.getImmutableType() instanceof AssociationType) {
            return (T)table;
        }
        if (table instanceof TableWrapper<?>) {
            TableWrapper<?> wrapper = (TableWrapper<?>) table;
            if (Objects.equals(wrapper.getJoinDisabledReason(), joinDisabledReason)) {
                return (T) table;
            }
            table = wrapper.unwrap();
        }
        Class<?> javaClass = table.getImmutableType().getJavaClass();
        Constructor<?> constructor = CACHE.get(javaClass);
        if (constructor == null) {
            return (T) table;
        }
        try {
            return (T) constructor.newInstance(table, joinDisabledReason);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new AssertionError(
                    "Internal bug: Can not create instance of " +
                            constructor.getDeclaringClass().getName()
            );
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException)target;
            }
            if (target instanceof Error) {
                throw (Error)target;
            }
            throw new AssertionError(
                    "Internal bug: Can not create instance of " +
                            constructor.getDeclaringClass().getName()
            );
        }
    }

    private static Constructor<?> createConstructor(Class<?> javaClass) {
        Class<?> tableClass;
        try {
            tableClass = Class.forName(
                    javaClass.getName() + "TableEx",
                    true,
                    javaClass.getClassLoader()
            );
        } catch (ClassNotFoundException ex) {
            return null;
        }
        if (!AbstractTableWrapper.class.isAssignableFrom(tableClass)) {
            throw new ModelException(
                    "\"" +
                            tableClass +
                            "\" is not derived type of \"" +
                            AbstractTableWrapper.class.getName() +
                            "\""
            );
        }
        try {
            return tableClass.getConstructor(TableImplementor.class, String.class);
        } catch (NoSuchMethodException ex) {
            throw new ModelException(
                    "\"" +
                            tableClass +
                            "\" dose not have constructor whose argument types are \"" +
                            TableImplementor.class.getName() +
                            "\" and \"" +
                            String.class.getName() +
                            "\""
            );
        }
    }

    public static <E> TableImplementor<E> unwrap(Table<E> table) {
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<E>) table;
        }
        if (table instanceof TableWrapper<?>) {
            return ((TableWrapper<E>) table).unwrap();
        }
        throw new IllegalArgumentException("Unknown table implementation");
    }
}
