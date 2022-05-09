package org.babyfish.jimmer.sql.ast.query;

public interface ConfigurableTypedSubQuery<R> extends TypedSubQuery<R> {

    ConfigurableTypedSubQuery<R> limit(int limit, int offset);

    ConfigurableTypedSubQuery<R> distinct();
}
