package org.babyfish.jimmer.quarkus.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.jetbrains.annotations.NotNull;

public interface Jimmer {

    JSqlClient getDefaultJSqlClient();

    JSqlClient getJSqlClient(@NotNull String dataSourceName);
}
