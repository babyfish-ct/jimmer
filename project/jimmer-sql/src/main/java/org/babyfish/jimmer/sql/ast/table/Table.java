package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;

public interface Table<E> extends Selection<E> {

    <XE extends Expression<?>> XE get(String prop);

    <XT extends Table<?>> XT join(String prop);

    <XT extends Table<?>> XT join(String prop, boolean left);
}
