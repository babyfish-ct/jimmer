package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Predicate;

import java.util.function.Supplier;

public interface Filterable {

    /**
     * Add some predicates(logical and)
     * @param predicates Predicates, everything one can be null
     * @return Return the current object to support chain programming style
     */
    @OldChain
    Filterable where(Predicate...predicates);

    /**
     * If the condition is true, add a predicate
     * <p>
     *     Please look at this example about predicate is not `eq`, `ne`, `like`, `ilike`
     *     <pre><code>
     *         .whereIf(minPrice != null, table.price().ge(minPrice))
     *     </code></pre>
     * </p>
     *
     * The predicate `ge` which is not `eq`, `ne`, `like`, `ilike` cannot be created by null
     * because `NullPointerException` will be thrown. At this time, you can use {@link #whereIf(boolean, Supplier)}
     *
     * @param condition The condition
     * @param predicate The predicate to be added, can be null
     * @return Return the current object to support chain programming style
     */
    @OldChain
    default Filterable whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    /**
     * If the condition is true, add a predicate
     * @param condition The condition
     * @param block An lambda to create predicate when condition is true
     * @return Return the current object to support chain programming style
     */
    @OldChain
    default Filterable whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }
}
