package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;

public interface JimmerCustomizer {

    void customize(JSqlClient.Builder builder);
}
