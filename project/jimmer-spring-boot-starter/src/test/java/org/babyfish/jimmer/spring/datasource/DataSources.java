package org.babyfish.jimmer.spring.datasource;

import org.h2.Driver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

public class DataSources {

    private DataSources() {}

    public static DataSource create(TxCallback callback) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriver(new Driver());
        dataSource.setUrl("jdbc:h2:~/jimmer_spring_test_db;database_to_upper=true");
        return callback == null ? dataSource : new DataSourceProxy(dataSource, callback);
    }
}
