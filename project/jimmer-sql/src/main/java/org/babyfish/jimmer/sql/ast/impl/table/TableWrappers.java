package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;
import org.babyfish.jimmer.util.StaticCache;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TableWrappers {

    private static final StaticCache<Class<?>, Constructor<?>> CACHE =
            new StaticCache<>(TableWrappers::createConstructor);

    private TableWrappers() {}

    @SuppressWarnings("unchecked")
    public static <T extends TableEx<?>> T wrap(TableImplementor<?> table) {
        if (table.getImmutableType() instanceof AssociationType || table instanceof AbstractTableWrapper<?>) {
            return (T)table;
        }
        Class<?> javaClass = table.getImmutableType().getJavaClass();
        Constructor<?> constructor = CACHE.get(javaClass);
        if (constructor == null) {
            return (T) table;
        }
        try {
            return (T) constructor.newInstance(table);
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
            return null;
        }
        try {
            return tableClass.getConstructor(TableEx.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static TableImplementor<?> unwrap(Table<?> table) {
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<?>) table;
        }
        if (table instanceof AbstractTableWrapper<?>) {
            return unwrap(((AbstractTableWrapper<?>) table).__unwrap());
        }
        throw new IllegalArgumentException("Unknown table implementation");
    }
}
