package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface ProductId {

    String alpha();

    String beta();
}
