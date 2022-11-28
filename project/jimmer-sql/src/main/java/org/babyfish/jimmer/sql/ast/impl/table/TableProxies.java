package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TableProxies {

    private static final StaticCache<Class<?>, Constructor<?>> WRAPPER_CACHE =
            new StaticCache<>(TableProxies::createWrapperConstructor);

    private static final StaticCache<ImmutableType, Constructor<?>> FLUENT_CACHE =
            new StaticCache<>(TableProxies::createFluentConstructor);

    private TableProxies() {}

    @SuppressWarnings("unchecked")
    public static <T extends TableEx<?>> T wrap(Table<?> table) {
        ImmutableType immutableType = table.getImmutableType();
        if (immutableType instanceof AssociationType || immutableType.isKotlinClass()) {
            return (T)table;
        }
        Class<?> javaClass = immutableType.getJavaClass();
        Constructor<?> constructor = WRAPPER_CACHE.get(javaClass);
        if (constructor == null) {
            return (T) table;
        }
        return invokeConstructor(constructor, table);
    }

    public static <T extends Table<?>> T fluent(
            AbstractTypedTable.DelayedOperation<?> delayedOperation
    ) {
        Constructor<?> constructor = FLUENT_CACHE.get(delayedOperation.targetType());
        return invokeConstructor(constructor, delayedOperation);
    }

    private static Constructor<?> createWrapperConstructor(Class<?> javaClass) {
        return createConstructor(
                ImmutableType.get(javaClass),
                new Class[] {TableImplementor.class}
        );
    }

    private static Constructor<?> createFluentConstructor(ImmutableType type) {
        if (type instanceof AssociationType) {
            throw new IllegalStateException("\"" + type + "\" cannot be AssociationType");
        }
        return createConstructor(type, new Class[]{AbstractTypedTable.DelayedOperation.class});
    }

    private static Constructor<?> createConstructor(ImmutableType type, Class<?>[] parameterTypes) {
        Class<?> tableClass;
        try {
            tableClass = Class.forName(
                    type.getJavaClass().getName() + "TableEx",
                    true,
                    type.getJavaClass().getClassLoader()
            );
        } catch (ClassNotFoundException ex) {
            return null;
        }
        if (!AbstractTypedTable.class.isAssignableFrom(tableClass)) {
            throw new ModelException(
                    "\"" +
                            tableClass +
                            "\" is not derived type of \"" +
                            AbstractTypedTable.class.getName() +
                            "\""
            );
        }
        try {
            return tableClass.getConstructor(parameterTypes);
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

    @SuppressWarnings("unchecked")
    private static <T> T invokeConstructor(Constructor<?> constructor, Object ... args) {
        if (constructor == null) {
            throw new IllegalStateException(
                    "There is no constructor whose parameter list \"(" +
                            AbstractTypedTable.class.getName() +
                            ", " +
                            AbstractTypedTable.DelayedOperation.class.getName() +
                            ")\""
            );
        }
        try {
            return (T) constructor.newInstance(args);
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

    @SuppressWarnings("unchecked")
    public static <E> TableImplementor<E> resolve(Table<E> table, RootTableResolver resolver) {
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<E>) table;
        }
        if (table instanceof TableProxy<?>) {
            return ((TableProxy<E>) table).__resolve(resolver);
        }
        throw new IllegalArgumentException("Unknown table implementation");
    }
}
