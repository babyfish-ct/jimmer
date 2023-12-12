package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;
import org.jetbrains.annotations.Nullable;

public interface Service {

    Class<?> getJavaType();

    @Nullable
    Doc getDoc();
}
