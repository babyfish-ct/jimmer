package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PropConfig<P extends BaseProp> {

    @Nullable
    Predicate getPredicate();

    List<OrderItem<P>> getOrderItems();

    @Nullable
    String getFilterClassName();

    @Nullable
    String getRecursionClassName();

    String getFetchType();

    int getLimit();

    int getOffset();

    int getBatch();

    int getDepth();

    interface Predicate {
        interface And extends Predicate {
            List<Predicate> getPredicates();
        }

        interface Or extends Predicate {
            List<Predicate> getPredicates();
        }

        interface Cmp<P extends BaseProp> extends Predicate {
            List<P> getPath();
            String getOperator();
            Object getValue();
        }

        interface Nullity<P extends BaseProp> extends Predicate {
            List<P> getPath();
            boolean isNegative();
        }
    }

    interface OrderItem<P extends BaseProp> {
        List<P> getPath();
        boolean isDesc();
    }
}
