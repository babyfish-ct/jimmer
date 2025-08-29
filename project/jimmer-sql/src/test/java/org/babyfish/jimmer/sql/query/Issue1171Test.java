package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.issue1171.SysConfig;
import org.babyfish.jimmer.sql.model.issue1171.SysConfigTable;
import org.babyfish.jimmer.sql.model.issue1171.dto.SysConfigSpecification;
import org.junit.jupiter.api.Test;

public class Issue1171Test extends AbstractQueryTest {
    @Test
    public void test() {
        ConfigurableRootQuery<SysConfigTable, SysConfig> query = getSqlClient(config -> {
            config.setDialect(new PostgresDialect());
        }).createQuery(SysConfigTable.$)
                .where(new SysConfigSpecification())
                .orderBy(SysConfigTable.$.configId().asc())
                .select(SysConfigTable.$);

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> query.fetchPage(0, 10, con),
                ctx -> {
                    ctx.sql(
                            "select count(1) from issue1171_sys_config tb_1_"
                    );
                    ctx.statement(1).sql(
                            // use getSelectableProps in renderIdOnlyQuery, keep id first
                            "select tb_1_.config_id, " +
                                    "tb_1_.create_dept, tb_1_.create_by, tb_1_.create_time, tb_1_.update_by, tb_1_.update_time, " +
                                    "tb_1_.config_name, tb_1_.config_key, tb_1_.config_value, tb_1_.config_type, tb_1_.remark " +
                                    "from issue1171_sys_config tb_1_ order by tb_1_.config_id asc limit ?"
                    ).variables(10);
                }
        );
    }
}
