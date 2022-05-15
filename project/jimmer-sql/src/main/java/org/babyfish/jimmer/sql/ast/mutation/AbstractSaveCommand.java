package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.Consumer;
import java.util.function.Function;

public interface AbstractSaveCommand<C extends AbstractSaveCommand<C>> {

    C configure(Consumer<Cfg> block);

    interface Cfg {

        Cfg setMode(SaveMode mode);

        Cfg setKeyProps(ImmutableProp ... props);

        Cfg setKeyProps(Class<?> entityType, String ... props);

        <T extends Table<?>> Cfg setKeyProps(
                Class<T> tableType,
                Consumer<KeyPropCfg<T>> block
        );

        Cfg setAutoAttaching(ImmutableProp prop);

        Cfg setAutoAttaching(Class<?> entityType, String prop);

        <T extends Table<?>> Cfg setAutoAttaching(
                Class<T> tableType,
                Function<T, Table<?>> block
        );

        Cfg setAutoDetaching(ImmutableProp prop);

        Cfg setAutoDetaching(Class<?> entityType, String prop);

        <T extends TableEx<?>> Cfg setAutoDetaching(
                Class<T> tableType,
                Function<T, Table<?>> block
        );
    }

    interface KeyPropCfg<T extends Table<?>> {
        KeyPropCfg<T> add(Function<T, PropExpression<?>> block);
    }
}
