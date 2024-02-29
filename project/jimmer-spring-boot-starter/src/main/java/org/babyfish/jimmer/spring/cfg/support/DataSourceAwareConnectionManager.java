package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;

public interface DataSourceAwareConnectionManager extends ConnectionManager {

    @NotNull
    DataSource getDataSource();
}
