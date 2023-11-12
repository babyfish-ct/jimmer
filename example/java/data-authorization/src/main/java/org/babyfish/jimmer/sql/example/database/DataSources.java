package org.babyfish.jimmer.sql.example.database;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSources {

    public static final DataSource DATA_SOURCE;

    private DataSources() {}

    static {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:data-authorization;database_to_upper=true");
        DATA_SOURCE = dataSource;
    }
}
