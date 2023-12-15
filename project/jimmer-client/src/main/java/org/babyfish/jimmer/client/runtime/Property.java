package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;
import org.jetbrains.annotations.Nullable;

public interface Property {

    String getName();

    Type getType();

    @Nullable
    Doc getDoc();
}
