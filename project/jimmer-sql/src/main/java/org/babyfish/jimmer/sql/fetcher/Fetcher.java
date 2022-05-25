package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Map;
import java.util.function.Consumer;

public interface Fetcher<E> {

    Class<E> getJavaClass();

    ImmutableType getImmutableType();

    Map<String, Field> getFieldMap();

    @NewChain
    Fetcher<E> allTableFields();

    @NewChain
    Fetcher<E> allScalarFields();

    @NewChain
    Fetcher<E> add(String prop);

    @NewChain
    Fetcher<E> remove(String prop);

    @NewChain
    Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher
    );

    @NewChain
    Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    );

    boolean isSimpleFetcher();
}
