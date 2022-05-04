package org.babyfish.jimmer.sql.ast;

public interface StringExpression extends Expression<String> {

    Predicate like(String pattern);

    Predicate ilike(String pattern);
}
