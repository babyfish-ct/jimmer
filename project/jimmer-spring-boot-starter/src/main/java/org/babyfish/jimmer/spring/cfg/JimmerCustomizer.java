package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;
import org.jetbrains.annotations.NotNull;

public interface JimmerCustomizer {

    void customize(@NotNull JSqlClient.Builder builder) throws Exception;
}
