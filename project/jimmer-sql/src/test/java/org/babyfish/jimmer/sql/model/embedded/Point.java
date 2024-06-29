package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface Point {

    long x();

    long y();

    @Formula(dependencies = {"x", "y"})
    default double distance() {
        return Math.sqrt(x() * x() + y() * y());
    }
}
