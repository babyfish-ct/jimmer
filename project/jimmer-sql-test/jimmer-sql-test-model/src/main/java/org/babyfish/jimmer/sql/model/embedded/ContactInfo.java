package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public interface ContactInfo {

    String name();

    @Nullable
    String email();
}
