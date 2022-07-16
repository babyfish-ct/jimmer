package org.babyfish.jimmer.benchmark.jooq;

import org.babyfish.jimmer.benchmark.BenchmarkExecutor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JooqExecutor extends BenchmarkExecutor {

    private final DSLContext dslContext;

    public JooqExecutor(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public String name() {
        return "JOOQ";
    }

    @Override
    protected List<?> query() {
        return dslContext.selectFrom(DataTable.DATA).getQuery().fetchInto(JooqData.class);
    }
}
