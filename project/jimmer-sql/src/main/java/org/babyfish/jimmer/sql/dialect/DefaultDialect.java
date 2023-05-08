package org.babyfish.jimmer.sql.dialect;

public class DefaultDialect implements Dialect {

    public static final DefaultDialect INSTANCE = new DefaultDialect();

    protected DefaultDialect() {}

    @Override
    public void paginate(PaginationContext ctx) {
        ctx.origin().space().sql("limit ").variable(ctx.getLimit());
        if (ctx.getOffset() > 0) {
            ctx.sql(" offset ").variable(ctx.getOffset());
        }
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
