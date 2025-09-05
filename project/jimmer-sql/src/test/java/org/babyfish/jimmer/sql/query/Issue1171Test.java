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
        SysConfigSpecification specification = new SysConfigSpecification();
        specification.setConfigName("主框架页");

        ConfigurableRootQuery<SysConfigTable, SysConfig> query = getSqlClient(config -> {
            config.setDialect(new PostgresDialect());
            config.setReverseSortOptimizationEnabled(true);
            config.setInListPaddingEnabled(true);
            config.setOffsetOptimizingThreshold(0);
        }).createQuery(SysConfigTable.$)
                .where(specification)
                .orderBy(SysConfigTable.$.configId().asc())
                .select(SysConfigTable.$);

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> query.fetchPage(0, 10, con),
                ctx -> {
                    ctx.sql(
                            "select count(1) from issue1171_sys_config tb_1_ where tb_1_.config_name like ?"
                    ).variables("%主框架页%");
                    ctx.statement(1).sql(
                            // use getSelectableProps in renderIdOnlyQuery, keep id first
                            "select optimize_.config_id, " +
                                    "optimize_.create_dept, optimize_.create_by, optimize_.create_time, optimize_.update_by, optimize_.update_time, " +
                                    "optimize_.config_name, optimize_.config_key, optimize_.config_value, optimize_.config_type, optimize_.remark from" +
                                    " (select tb_1_.config_id optimize_core_id_ " +
                                    "from issue1171_sys_config tb_1_ " +
                                    "where tb_1_.config_name like ? " +
                                    "/* reverse sorting optimization */ order by tb_1_.config_id desc limit ?) " +
                                    "optimize_core_ inner join issue1171_sys_config optimize_ on optimize_.config_id = optimize_core_.optimize_core_id_"
                    ).variables("%主框架页%", 2);
                }
        );
    }
}
