package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class BaseQueryExportUsages {

    static final BaseQueryExportUsages EMPTY =
            new BaseQueryExportUsages(Collections.emptySet());

    private final Set<BaseTableOwner> fullRowExports;

    BaseQueryExportUsages(Set<BaseTableOwner> fullRowExports) {
        this.fullRowExports = fullRowExports;
    }

    boolean isFullRowExportRequired(BaseTableOwner baseTableOwner) {
        return fullRowExports.contains(baseTableOwner);
    }

    static final class Builder {

        private final Set<BaseTableOwner> fullRowExports = new HashSet<>();

        void requireFullRowExport(BaseTableOwner baseTableOwner) {
            fullRowExports.add(baseTableOwner);
        }

        BaseQueryExportUsages build() {
            if (fullRowExports.isEmpty()) {
                return EMPTY;
            }
            return new BaseQueryExportUsages(new HashSet<>(fullRowExports));
        }
    }
}
