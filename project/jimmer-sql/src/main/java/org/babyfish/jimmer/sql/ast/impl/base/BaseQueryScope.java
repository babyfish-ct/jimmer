package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
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
                .computeIfAbsent(baseTableOwner.index, it -> new BaseSelectionMapper(this));
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

        private final Map<Integer, Integer> aliasOffsetMap = new HashMap<>();

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
            int aliasOffset = aliasOffset(index, (Table<?>) selection, builder);
            for (Map.Entry<String, Integer> e : mapper.indexMap.entrySet()) {
                builder.separator()
                        .sql(replaceAlias(e.getKey(), aliasOffset))
                        .sql(" c").sql(Integer.toString(e.getValue()));
            }
        }

        private int aliasOffset(int index, Table<?> table, SqlBuilder builder) {
            return aliasOffsetMap.computeIfAbsent(index, it -> aliasOffset0(it, table, builder));
        }

        private int aliasOffset0(int index, Table<?> table, SqlBuilder builder) {
            TableImplementor<?> tableImplementor = TableProxies.resolve(table, builder.getAstContext());
            String alias = tableImplementor.realTable(builder.getAstContext().getJoinTypeMergeScope()).getAlias();
            String projection = map.get(index).indexMap.keySet().iterator().next();
            int dotIndex = projection.indexOf('.');
            String oldAlias = projection.substring(0, dotIndex);
            if (alias.equals(oldAlias)) {
                return 0;
            }
            return aliasNo(alias) - aliasNo(oldAlias);
        }

        private static String replaceAlias(String projection, int offset) {
            if (offset == 0) {
                return projection;
            }
            int dot = projection.indexOf('.');
            String columnSuffix = projection.substring(dot);
            String alias = projection.substring(0, dot);
            int aliasNo = aliasNo(alias);
            return "tb_" + (aliasNo + offset) + '_' + columnSuffix;
        }

        private static int aliasNo(String alias) {
            String noText = alias.substring(3, alias.length() - 1);
            return Integer.parseInt(noText);
        }
    }
}
