package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class BaseQueryScope {

    private final BaseTableImplementor<?> table;

    private final Map<String, BaseSelectionMapper> mapperMap = new LinkedHashMap<>();

    private int colNoSequence;

    public BaseQueryScope(BaseTableImplementor<?> table) {
        this.table = table;
    }

    public BaseTableImplementor<?> table() {
        return table;
    }

    public BaseSelectionMapper mapper(BaseTableOwner baseTableOwner) {
        BaseTableOwner parent = baseTableOwner.getParent();
        BaseSelectionMapper parentMapper = parent != null ?
                mapper(parent) :
                null;
        return mapperMap.computeIfAbsent(baseTableOwner.getPath(), it -> {
            return new BaseSelectionMapper(this, baseTableOwner, parentMapper);
        });
    }

    int colNo() {
        return ++colNoSequence;
    }

    public BaseSelectionRender toBaseSelectionRender() {
        return new BaseSelectionRenderImpl(mapperMap);
    }

    private static class BaseSelectionRenderImpl implements BaseSelectionRender {

        private final Map<String, BaseSelectionMapper> mapperMap;

        private BaseSelectionRenderImpl(Map<String, BaseSelectionMapper> mapperMap) {
            this.mapperMap = mapperMap;
        }

        @Override
        public void render(int index, Selection<?> selection, SqlBuilder builder) {
            List<BaseSelectionMapper> mappers = mapperMap
                    .values()
                    .stream()
                    .filter(m -> m.getBaseTableOwner().getIndex() == index)
                    .collect(Collectors.toList());
            if (mappers.isEmpty()) {
                return;
            }
            if (selection instanceof Expression<?>) {
                builder.separator();
                ((Ast) selection).renderTo(builder);
                builder.sql(" c").sql(Integer.toString(mappers.get(0).expressionIndex));
                return;
            }
            TableImplementor<?> tableImplementor = TableProxies.resolve((Table<?>) selection, builder.getAstContext());
            RealTableManager realTableManager = new RealTableManager(
                    tableImplementor.realTable(builder.getAstContext().getJoinTypeMergeScope())
            );
            for (BaseSelectionMapper mapper : mappers) {
                RealTable realTable = realTableManager.realTable(mapper);
                for (Map.Entry<String, Integer> e : mapper.indexMap.entrySet()) {
                    builder.separator()
                            .sql(realTable.getAlias()).sql(".").sql(e.getKey())
                            .sql(" c").sql(Integer.toString(e.getValue()));
                }
            }
        }
    }

    private static class RealTableManager {

        private final RealTable realTable;

        private final Map<BaseSelectionMapper, RealTable> realTableMap = new IdentityHashMap<>();

        private RealTableManager(RealTable realTable) {
            this.realTable = realTable;
        }

        public RealTable realTable(BaseSelectionMapper mapper) {
            return realTableMap.computeIfAbsent(mapper, this::createRealTable);
        }

        private RealTable createRealTable(BaseSelectionMapper mapper) {
            BaseSelectionMapper parentMapper = mapper.getParent();
            if (parentMapper != null) {
                RealTable parentRealTable = realTable(parentMapper);
                return parentRealTable.getChild(mapper.getBaseTableOwner().getChildKey());
            }
            return realTable;
        }
    }
}
