package org.babyfish.jimmer.sql.dialect;

public class DefaultDialect implements Dialect {

    public static final DefaultDialect INSTANCE = new DefaultDialect();

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .sql(" limit ")
                .variable(ctx.getLimit())
                .sql(" offset ")
                .variable(ctx.getOffset());
    }
}
