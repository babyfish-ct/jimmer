package org.babyfish.jimmer.sql.dialect;

public class MySqlDialect implements Dialect {

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .sql(" limit ")
                .variable(ctx.getOffset())
                .sql(", ")
                .variable(ctx.getLimit());
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(true, UpdateJoin.From.UNNECESSARY);
    }

    @Override
    public boolean needDeletedAlias() {
        return true;
    }
}
