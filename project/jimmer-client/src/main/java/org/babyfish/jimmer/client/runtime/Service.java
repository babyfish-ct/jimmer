package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Service {

    Class<?> getJavaType();

    List<Operation> getOperations();

    @Nullable
    Doc getDoc();
}
