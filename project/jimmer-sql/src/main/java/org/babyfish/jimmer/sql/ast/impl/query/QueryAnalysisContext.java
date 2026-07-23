package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public final class QueryAnalysisContext {

    private final AstContext astContext;

    public QueryAnalysisContext(AstContext astContext) {
        this.astContext = astContext;
    }

    public JSqlClientImplementor getSqlClient() {
        return astContext.getSqlClient();
    }

    public MetadataStrategy getMetadataStrategy() {
        return astContext.getSqlClient().getMetadataStrategy();
    }

    void pushStatement(AbstractMutableStatementImpl statement) {
        astContext.pushStatement(statement);
    }

    void popStatement() {
        astContext.popStatement();
    }

    public AbstractMutableStatementImpl getStatement() {
        return astContext.getStatement();
    }

    public TableImplementor<?> resolve(Table<?> table) {
        return TableProxies.resolve(table, astContext);
    }

    public TableImplementor<?> resolve(BaseTableOwner owner, Table<?> table) {
        TableImplementor<?>[] resolved = new TableImplementor<?>[1];
        owner.visitOwnerStatementChain(astContext, () -> resolved[0] = resolve(table));
        return resolved[0];
    }

    public BaseTableImplementor resolveBaseTable(BaseTableSymbol baseTable) {
        return astContext.resolveBaseTable(baseTable);
    }

    public RealTable realTable(TableLikeImplementor<?> tableLikeImplementor) {
        return tableLikeImplementor.realTable(astContext);
    }
}
