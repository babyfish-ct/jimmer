package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface Point {

    long x();

    long y();
}
