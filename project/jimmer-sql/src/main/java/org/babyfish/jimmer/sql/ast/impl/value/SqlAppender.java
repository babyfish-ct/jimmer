package org.babyfish.jimmer.sql.ast.impl.value;

public interface SqlAppender {

    SqlAppender sql(String text);

    SqlAppender variable(Object variable);
}
