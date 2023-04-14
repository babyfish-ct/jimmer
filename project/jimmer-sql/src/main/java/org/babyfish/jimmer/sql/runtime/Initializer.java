package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;

public interface Initializer {

    void initialize(JSqlClient sqlClient) throws Exception;
}
