package org.babyfish.jimmer.sql.ast.impl.associated;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;

import java.util.List;

public interface VirtualPredicate extends Predicate {

    TableImplementor<?> getTableImplementor(RootTableResolver resolver);

    Object getSubKey();

    Predicate toFinalPredicate(AbstractMutableStatementImpl parent, List<VirtualPredicate> blocks, Op op);

    enum Op { AND, OR }
}
