package org.babyfish.jimmer.sql.dialect;

public class SqlServerDialect implements Dialect {

    @Override
    public boolean isTupleSupported() {
        return false;
    }

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .sql(" offset ")
                .variable(ctx.getOffset())
                .sql(" rows fetch next ")
                .variable(ctx.getLimit())
                .sql(" rows only");
    }
}
