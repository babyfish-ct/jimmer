package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

class ChildTableOperator {

    private final SaveContext ctx;

    private final String tableName;

    private final List<ValueGetter> idGetters;

    private final List<ValueGetter> backRefGetters;

    ChildTableOperator(SaveContext ctx) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        this.ctx = ctx;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.idGetters = ValueGetter.valueGetters(sqlClient, ctx.path.getType().getIdProp());
        this.backRefGetters = ValueGetter.valueGetters(sqlClient, ctx.backReferenceProp);
    }
}
