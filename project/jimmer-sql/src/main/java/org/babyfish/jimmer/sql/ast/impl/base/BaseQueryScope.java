package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class BaseQueryScope {

    private final BaseTableImplementor table;

    private final Map<Integer, BaseSelectionMapper> mapperMap =
            new LinkedHashMap<>();

    private int colNoSequence;

    public BaseQueryScope(BaseTableImplementor table) {
        this.table = table;
    }

    public BaseTableImplementor table() {
        return table;
    }

    public BaseSelectionMapper mapper(BaseTableOwner baseTableOwner) {
        return mapperMap
                .computeIfAbsent(baseTableOwner.index, it -> new BaseSelectionMapper(this, it));
    }

    int colNo() {
        return ++colNoSequence;
    }

    public BaseSelectionAliasRender toBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return new BaseSelectionAliasRenderImpl(mapperMap, query);
    }

    private static class BaseSelectionAliasRenderImpl implements BaseSelectionAliasRender {

        private final Map<Integer, BaseSelectionMapper> map;

        private final ConfigurableBaseQuery<?> query;

        private BaseSelectionAliasRenderImpl(Map<Integer, BaseSelectionMapper> map, ConfigurableBaseQuery<?> query) {
            this.map = map;
            this.query = query;
        }

        @Override
        public void render(int index, Selection<?> selection, SqlBuilder builder) {
            BaseSelectionMapper mapper = map.get(index);
            if (mapper == null) {
                return;
            }

            if (selection instanceof Expression<?>) {
                builder.separator();
                ((Ast) selection).renderTo(builder);
                builder.sql(" c").sql(Integer.toString(mapper.expressionIndex));
                return;
            }
            RealTable realTable = TableProxies.resolve((Table<?>) selection, builder.getAstContext())
                    .realTable(builder.getAstContext().getJoinTypeMergeScope());
            for (Map.Entry<BaseSelectionMapper.QualifiedColumn, Integer> e : mapper.columnIndexMap.entrySet()) {
                builder.separator()
                        .sql(table(realTable, e.getKey().keys).getAlias())
                        .sql(".")
                        .sql(e.getKey().name)
                        .sql(" c").sql(Integer.toString(e.getValue()));
            }
        }
    }

    private static RealTable table(RealTable table, List<RealTable.Key> keys) {
        for (RealTable.Key key : keys) {
            table = table.getChild(key);
        }
        return table;
    }
}
