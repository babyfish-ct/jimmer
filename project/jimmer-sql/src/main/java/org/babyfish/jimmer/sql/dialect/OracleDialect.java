package org.babyfish.jimmer.sql.dialect;

public class OracleDialect implements Dialect {

    @Override
    public void paginate(PaginationContext ctx) {
        int offset = ctx.getOffset();
        if (offset == 0) {
            limit(ctx);
        } else {
            ctx.sql("select * from (");
            limit(ctx);
            ctx.sql(") limited__ where rn__ > ");
            ctx.variable(offset);
        }
    }

    private void limit(PaginationContext ctx) {
        int offset = ctx.getOffset();
        int limit = ctx.getLimit();
        String rnProjection = offset > 0 ? ", rownum rn__" : "";
        ctx
                .sql("select core__.*" + rnProjection + " from (")
                .origin()
                .sql(") core__ where rownum <= ")
                .variable(offset + limit);
    }

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return "select " + sequenceName + ".nextval from dual";
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
