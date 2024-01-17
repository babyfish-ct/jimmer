package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.sql.Embeddable;
import org.jetbrains.annotations.Nullable;

@Embeddable
public interface Contact {

    String email();

    @Nullable
    String phone();
}
