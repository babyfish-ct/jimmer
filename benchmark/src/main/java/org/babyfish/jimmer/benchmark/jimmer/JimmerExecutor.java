package org.babyfish.jimmer.benchmark.jimmer;

import org.babyfish.jimmer.benchmark.BenchmarkExecutor;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JimmerExecutor extends BenchmarkExecutor {

    private final SqlClient sqlClient;

    public JimmerExecutor(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }


    @Override
    public String name() {
        return "Jimmer";
    }

    @Override
    protected List<?> query() {
        return sqlClient
                .createQuery(JimmerDataTable.class, RootSelectable::select)
                .execute();
    }
}
