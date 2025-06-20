package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class BaseQueryScope {

    final AstContext astContext;

    private final Map<Key, BaseSelectionMapper> mapperMap =
            new LinkedHashMap<>();

    private int colNoSequence;

    public BaseQueryScope(AstContext astContext) {
        this.astContext = astContext;
    }

    public BaseSelectionMapper mapper(BaseTableOwner baseTableOwner) {
        BaseTableImplementor baseTable = astContext.resolveBaseTable(baseTableOwner.getBaseTable());
        RealTable realBaseTable = baseTable.realTable(astContext);
        return mapperMap
                .computeIfAbsent(
                        new Key(realBaseTable, baseTableOwner.index),
                        it -> new BaseSelectionMapper(this, it.realTable, it.selectionIndex));
    }

    int colNo() {
        return ++colNoSequence;
    }

    public BaseSelectionAliasRender toBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        return new BaseSelectionAliasRenderImpl(mapperMap, (BaseTableSymbol) query.asBaseTable());
    }

    private static class BaseSelectionAliasRenderImpl implements BaseSelectionAliasRender {

        private final Map<Key, BaseSelectionMapper> mapperMap;

        private final BaseTableSymbol baseTableSymbol;

        private BaseSelectionAliasRenderImpl(Map<Key, BaseSelectionMapper> mapperMap, BaseTableSymbol baseTableSymbol) {
            this.mapperMap = mapperMap;
            this.baseTableSymbol = baseTableSymbol;
        }

        @Override
        public void render(int index, Selection<?> selection, SqlBuilder builder) {
            RealTable realBaseTable = builder.getAstContext().getRenderedRealBaseTable();
            BaseSelectionMapper mapper = mapperMap.get(new Key(realBaseTable, index));
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
                    .realTable(builder.getAstContext());
            for (Map.Entry<BaseSelectionMapper.QualifiedColumn, Integer> e : mapper.columnIndexMap.entrySet()) {
                BaseSelectionMapper.QualifiedColumn qualifiedColumn = e.getKey();
                String alias = childTableByKeys(realTable, qualifiedColumn.keys).getAlias();
                builder.separator();
                if (qualifiedColumn.formula != null) {
                    builder.sql(qualifiedColumn.formula.toSql(alias));
                } else {
                    builder
                            .sql(alias)
                            .sql(".")
                            .sql(qualifiedColumn.name);
                }
                builder.sql(" c").sql(Integer.toString(e.getValue()));
            }
        }
    }

    private static RealTable childTableByKeys(RealTable table, List<RealTable.Key> keys) {
        for (RealTable.Key key : keys) {
            table = table.getChild(key);
        }
        return table;
    }

    private static final class Key {
        final RealTable realTable;
        final int selectionIndex;

        private Key(RealTable realTable, int selectionIndex) {
            this.realTable = realTable;
            this.selectionIndex = selectionIndex;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(realTable);
            result = 31 * result + selectionIndex;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return realTable == key.realTable && selectionIndex == key.selectionIndex;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "realTable=" + realTable +
                    ", selectionIndex=" + selectionIndex +
                    '}';
        }
    }
}
