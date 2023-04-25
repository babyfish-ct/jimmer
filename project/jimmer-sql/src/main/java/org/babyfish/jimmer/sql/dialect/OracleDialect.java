package org.babyfish.jimmer.sql.dialect;

import org.jetbrains.annotations.Nullable;

public class OracleDialect implements Dialect {

    public static final String OPTIMIZE_CORE_ROW_NUMBER_ALIAS = "optimize_rn__";

    @Override
    public void paginate(PaginationContext ctx) {
        int offset = ctx.getOffset();
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
    public String toString() {
        return getClass().getName();
    }
}
