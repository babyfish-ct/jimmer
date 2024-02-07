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
    Fetcher<E> allScalarFields();

    @NewChain
    Fetcher<E> allReferenceIds();

    /**
     * allScalars + allForeignKeys
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> allTableFields();

    /**
     * Fetch a property without child fetcher,
     * for associated property, that means fetch id-only object
     * @param prop Propery name
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> add(String prop);

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

    @NewChain
    Fetcher<E> addRecursion(
            String prop,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    );

    /**
     * Fetch association directly based on foreign key, the associated object has only id property
     * @param prop Property name
     * @param referenceType Reference type which has 2 choices
     *                      <ul>
     *                          <li>DEFAULT: The associated will filtered by global filters(include built-lt logical deleted filter)</li>
     *                          <li>RAW: Raw value of foreign key</li>
     *                      </ul>
     *                      <p>If the property is not an association directly based on foreign key, this argument will be ignored</p>
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> add(String prop, IdOnlyFetchType referenceType);

    /**
     * Unfetch a property
     * @param prop Property name
     * @return A new fetcher
     */
    @NewChain
    Fetcher<E> remove(String prop);

    String toString(boolean multiLine);
}
