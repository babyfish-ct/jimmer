package org.babyfish.jimmer.benchmark.mug;

import com.google.mu.safesql.SafeSql;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class MugSafeSql {

    private final DataSource dataSource;

    public MugSafeSql(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void execute() {
        SafeSql.of("SELECT ID, VALUE_1, VALUE_2,  VALUE_3, VALUE_4, VALUE_5, VALUE_6, VALUE_7, VALUE_8, VALUE_9 FROM DATA").query(dataSource, MugSafeSqlData.class);
    }
}
