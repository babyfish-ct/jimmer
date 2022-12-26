package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;

public interface JimmerInitializer {

    void initialize(JSqlClient sqlClient);
}
