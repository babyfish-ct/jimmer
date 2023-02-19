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

    /**
     * allScalars + allForeignKeys
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> allTableFields();

    @NewChain
    Fetcher<E> allScalarFields();

    /**
     * Fetch a property without child fetcher,
     * for associated property, that means fetch id-only object
     * @param prop Propery name
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> add(String prop);

    /**
     * Unfetch a property
     * @param prop Property name
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> remove(String prop);

    /**
     * Fetch a property with child fetcher,
     * error will be raised if the specified property is not association
     * @param prop Property name
     * @param childFetcher Deeper child fetcher for associated objects
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher
    );

    /**
     * Fetch a property with child fetcher and more configuration,
     * error will be raised if the specified property is not association
     * @param prop Property name
     * @param childFetcher Deeper child fetcher for associated objects
     * @param loaderBlock An optional lambda expression that lets the user set more configurations
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    );

    /**
     * Are all fetched properties simple fields?
     * @return Checked result
     */
    boolean isSimpleFetcher();
}
