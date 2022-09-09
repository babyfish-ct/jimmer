package org.babyfish.jimmer.sql.fluent.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.fluent.FluentUpdate;

import java.sql.Connection;

public class FluentUpdateImpl implements FluentUpdate {

    private final MutableUpdateImpl raw;

    private final Runnable onTerminate;

    public FluentUpdateImpl(MutableUpdateImpl raw, Runnable onTerminate) {
        this.raw = raw;
        this.onTerminate = onTerminate;
    }

    @Override
    public FluentUpdate where(Predicate... predicates) {
        raw.where(predicates);
        return this;
    }

    @Override
    public <X> FluentUpdate set(PropExpression<X> path, X value) {
        raw.set(path, value);
        return this;
    }

    @Override
    public <X> FluentUpdate set(PropExpression<X> path, Expression<X> value) {
        raw.set(path, value);
        return this;
    }

    @Override
    public Integer execute() {
        onTerminate.run();
        return raw.execute();
    }

    @Override
    public Integer execute(Connection con) {
        onTerminate.run();
        return raw.execute(con);
    }
}
