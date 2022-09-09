package org.babyfish.jimmer.sql.fluent.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.fluent.FluentDelete;

import java.sql.Connection;

class FluentDeleteImpl implements FluentDelete {

    private final MutableDeleteImpl raw;

    private final Runnable onTerminate;

    public FluentDeleteImpl(MutableDeleteImpl raw, Runnable onTerminate) {
        this.raw = raw;
        this.onTerminate = onTerminate;
    }

    @Override
    public FluentDelete where(Predicate... predicates) {
        raw.where(predicates);
        return this;
    }

    @Override
    public Integer execute() {
        raw.freeze();
        onTerminate.run();
        return raw.execute();
    }

    @Override
    public Integer execute(Connection con) {
        raw.freeze();
        onTerminate.run();
        return raw.execute(con);
    }
}
