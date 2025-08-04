package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class TableProxies {

    private static final ClassCache<Constructor<?>> WRAPPER_CACHE =
            new ClassCache<>(TableProxies::createWrapperConstructor);

    private static final TypeCache<Constructor<?>> FLUENT_CACHE =
            new TypeCache<>(TableProxies::createFluentConstructor);

    private static final ClassCache<TableProxy<?>> ROOT_PROXY_CACHE =
            new ClassCache<>(TableProxies::createRootProxy);

    private TableProxies() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>> T wrap(Table<?> table) {
        ImmutableType immutableType = table.getImmutableType();
        if (immutableType instanceof AssociationType || immutableType.isKotlinClass() || table instanceof AbstractTypedTable<?>) {
            return (T) table;
        }
        Class<?> javaClass = immutableType.getJavaClass();
        Constructor<?> constructor = WRAPPER_CACHE.get(javaClass);
        if (constructor == null) {
            return (T) table;
        }
        return invokeConstructor(constructor, table);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>> T fluent(Class<?> type) {
        return (T) ROOT_PROXY_CACHE.get(type);
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

    private static TableProxy<?> createRootProxy(Class<?> javaClass) {
        Class<?> tableClass = tableWrapperClass(javaClass);
        if (tableClass == null) {
            return null;
        }
        Field field;
        try {
            field = tableClass.getField("$");
        } catch (NoSuchFieldException ex) {
            return null;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            try {
                return (TableProxy<?>) field.get(null);
            } catch (IllegalAccessException ex) {
                return null;
            }
        }
        return null;
    }

    static Class<?> tableWrapperClass(Class<?> entityType) {
        Class<?> wrapperClass;
        try {
            wrapperClass = Class.forName(
                    entityType.getName() + "TableEx",
                    true,
                    entityType.getClassLoader()
            );
        } catch (ClassNotFoundException ex) {
            return null;
        }
        if (!AbstractTypedTable.class.isAssignableFrom(wrapperClass)) {
            throw new ModelException(
                    "\"" +
                            wrapperClass +
                            "\" is not derived type of \"" +
                            AbstractTypedTable.class.getName() +
                            "\""
            );
        }
        return wrapperClass;
    }

    private static Constructor<?> createConstructor(ImmutableType type, Class<?>[] parameterTypes) {
        Class<?> tableClass = tableWrapperClass(type.getJavaClass());
        if (tableClass == null) {
            return null;
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
