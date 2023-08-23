package org.babyfish.jimmer.sql.dialect;

import org.jetbrains.annotations.Nullable;

public class OracleDialect implements Dialect {

    public static final String OPTIMIZE_CORE_ROW_NUMBER_ALIAS = "optimize_rn__";

    @Override
    public void paginate(PaginationContext ctx) {
        long offset = ctx.getOffset();
        if (offset == 0) {
            limit(ctx);
        } else {
            if (ctx.isIdOnly()) {
                ctx.sql("select limited__.*");
                if (ctx.isIdOnly()) {
                    ctx.sql(", rownum ").sql(OPTIMIZE_CORE_ROW_NUMBER_ALIAS);
                }
            } else {
                ctx.sql("select *");
            }
            ctx.sql(" from (");
            limit(ctx);
            ctx.sql(") limited__ where rn__ > ");
            ctx.variable(offset);
        }
    }

    private void limit(PaginationContext ctx) {
        long offset = ctx.getOffset();
        int limit = ctx.getLimit();
        String rnProjection = offset > 0 ? ", rownum rn__" : "";
        ctx
                .sql("select core__.*" + rnProjection + " from (").newLine()
                .origin()
                .newLine().sql(") core__ where rownum <= ")
                .variable(offset + limit);
    }

    @Override
    public String getSelectIdFromSequenceSql(String sequenceName) {
        return "select " + sequenceName + ".nextval from dual";
    }

    @Nullable
    @Override
    public String getOffsetOptimizationNumField() {
        return "ROWNUM";
    }

    @Override
    public boolean isMultiInsertionSupported() {
        return false;
    }

    @Override
    public @Nullable String getConstantTableName() {
        return "dual";
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(\n" +
                "\tID number generated always as identity,\n" +
                "\tIMMUTABLE_TYPE varchar2(128),\n" +
                "\tIMMUTABLE_PROP varchar2(128),\n" +
                "\tCACHE_KEY varchar2(64) not null,\n" +
                "\tREASON varchar2(32)\n" +
                ")";
    }
}
