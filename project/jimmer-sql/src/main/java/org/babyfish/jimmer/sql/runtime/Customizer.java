package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;

public interface Customizer {

    void customize(JSqlClient.Builder builder) throws Exception;
}
