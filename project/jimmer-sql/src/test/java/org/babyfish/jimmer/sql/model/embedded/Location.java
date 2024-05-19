package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface Location {

    String host();

    Integer port();
}
