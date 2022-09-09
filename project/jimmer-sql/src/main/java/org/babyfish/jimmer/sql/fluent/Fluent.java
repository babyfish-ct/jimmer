package org.babyfish.jimmer.sql.fluent;

/**
 * A query created by a Fluent object cannot be used to create sub queries,
 * but needs to call the subQuery of the Fluent object.
 *
 * Fluent has a short life cycle, it needs to be created for each query.
 */
public interface Fluent {

    <T extends FluentTable<?>> FluentRootQuery<T> query(T table);

    FluentSubQuery subQuery(FluentTable<?> table);

    FluentUpdate update(FluentTable<?> table);

    FluentDelete delete(FluentTable<?> table);
}
