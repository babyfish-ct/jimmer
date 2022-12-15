package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

public interface Property {

    String getName();

    Type getType();

    @Nullable
    Document getDocument();
}
