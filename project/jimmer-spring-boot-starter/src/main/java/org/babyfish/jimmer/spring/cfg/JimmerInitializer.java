package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;
import org.jetbrains.annotations.NotNull;

public interface JimmerInitializer {

    void initialize(@NotNull JSqlClient sqlClient) throws Exception;
}
