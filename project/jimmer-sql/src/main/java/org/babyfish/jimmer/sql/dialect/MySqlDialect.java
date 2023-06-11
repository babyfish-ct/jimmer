package org.babyfish.jimmer.sql.dialect;

/**
 * For MySQL or TiDB
 */
public class MySqlDialect implements Dialect {

    @Override
    public void paginate(PaginationContext ctx) {
        ctx
                .origin()
                .space()
                .sql("limit ")
                .variable(ctx.getOffset())
                .sql(", ")
                .variable(ctx.getLimit());
    }

    @Override
    public UpdateJoin getUpdateJoin() {
        return new UpdateJoin(true, UpdateJoin.From.UNNECESSARY);
    }

    @Override
    public boolean isDeletedAliasRequired() {
        return true;
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return "create table JIMMER_TRANS_CACHE_OPERATOR(\n" +
                "\tID bigint unsigned not null auto_increment primary key,\n" +
                "\tIMMUTABLE_TYPE varchar(128),\n" +
                "\tIMMUTABLE_PROP varchar(128),\n" +
                "\tCACHE_KEY varchar(64) not null,\n" +
                "\tREASON varchar(32)\n" +
                ") engine=innodb";
    }
}
