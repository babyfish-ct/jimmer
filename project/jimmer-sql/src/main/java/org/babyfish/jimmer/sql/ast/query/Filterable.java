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
     * This method is deprecated, using {@code Dynamic Predicates}
     * is a more convenient approach.
     *
     * <p>Please look at this example:</p>
     * <pre>{@code
     * whereIf(name != null, table.name().eq(name))
     * }</pre>
     * When {@code name} is null, this code works because
     * {@code eq(null)} is automatically translated by Jimmer
     * to {@code isNull()}, which doesn't cause an exception.
     *
     * <p>Let's look at another example:</p>
     * <pre>{@code
     * whereIf(minPrice != null, table.price().ge(minPrice))
     * }</pre>
     * Except RUST marco, almost all programming languages
     * calculate all parameters first and then call the function.
     * Therefore, before {@code whereIf} is executed, {@code ge(null)}
     * already causes an exception.
     *
     * <p>For this reason, whereIf provides an overloaded form with a lambda parameter:</p>
     * <pre>{@code
     * whereIf(minPrice != null, () -> table.price().ge(minPrice))
     * }</pre>
     * Although this overloaded form can solve this problem,
     * it ultimately adds a mental burden during development.
     *
     * <p>Therefore, Jimmer provides {@code Dynamic Predicates}</p>
     * <table>
     *     <tr>
     *         <td><b>Java</b></td>
     *         <td><b>Kotlin</b></td>
     *     </tr>
     *     <tr>
     *         <td>eqIf</td>
     *         <td>eq?</td>
     *     </tr>
     *     <tr>
     *         <td>neIf</td>
     *         <td>ne?</td>
     *     </tr>
     *     <tr>
     *         <td>ltIf</td>
     *         <td>lt?</td>
     *     </tr>
     *     <tr>
     *         <td>leIf</td>
     *         <td>le?</td>
     *     </tr>
     *     <tr>
     *         <td>gtIf</td>
     *         <td>gt?</td>
     *     </tr>
     *     <tr>
     *         <td>geIf</td>
     *         <td>ge?</td>
     *     </tr>
     *     <tr>
     *         <td>likeIf</td>
     *         <td>like?</td>
     *     </tr>
     *     <tr>
     *         <td>ilikeIf</td>
     *         <td>ilike?</td>
     *     </tr>
     *     <tr>
     *         <td>betweenIf</td>
     *         <td>bwtween?</td>
     *     </tr>
     * </table>
     *
     * Taking Java's {@code geIf} as an example, this functionality
     * is ultimately implemented like this.
     * <pre>{@code
     * where(table.price().geIf(minPrice))
     * }</pre>
     */
    @Deprecated
    @OldChain
    default Filterable whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
        }
        return this;
    }

    /**
     * Avoid using this method whenever possible, using
     * {@code Dynamic Predicates} are a more convenient approach.
     *
     * <p>Please look at this example:</p>
     * <pre>{@code
     * whereIf(name != null, table.name().eq(name))
     * }</pre>
     * When {@code name} is null, this code works because
     * {@code eq(null)} is automatically translated by Jimmer
     * to {@code isNull()}, which doesn't cause an exception.
     *
     * <p>Let's look at another example:</p>
     * <pre>{@code
     * whereIf(minPrice != null, table.price().ge(minPrice))
     * }</pre>
     * Except RUST marco, almost all programming languages
     * calculate all parameters first and then call the function.
     * Therefore, before {@code whereIf} is executed, {@code ge(null)}
     * already causes an exception.
     *
     * <p>For this reason, whereIf provides an overloaded form with a lambda parameter:</p>
     * <pre>{@code
     * whereIf(minPrice != null, () -> table.price().ge(minPrice))
     * }</pre>
     * Although this overloaded form can solve this problem,
     * it ultimately adds a mental burden during development.
     *
     * <p>Therefore, Jimmer provides {@code Dynamic Predicate}</p>
     * <table>
     *     <tr>
     *         <td><b>Java</b></td>
     *         <td><b>Kotlin</b></td>
     *     </tr>
     *     <tr>
     *         <td>eqIf</td>
     *         <td>eq?</td>
     *     </tr>
     *     <tr>
     *         <td>neIf</td>
     *         <td>ne?</td>
     *     </tr>
     *     <tr>
     *         <td>ltIf</td>
     *         <td>lt?</td>
     *     </tr>
     *     <tr>
     *         <td>leIf</td>
     *         <td>le?</td>
     *     </tr>
     *     <tr>
     *         <td>gtIf</td>
     *         <td>gt?</td>
     *     </tr>
     *     <tr>
     *         <td>geIf</td>
     *         <td>ge?</td>
     *     </tr>
     *     <tr>
     *         <td>likeIf</td>
     *         <td>like?</td>
     *     </tr>
     *     <tr>
     *         <td>ilikeIf</td>
     *         <td>ilike?</td>
     *     </tr>
     *     <tr>
     *         <td>betweenIf</td>
     *         <td>bwtween?</td>
     *     </tr>
     * </table>
     *
     * Taking Java's {@code geIf} as an example, this functionality
     * is ultimately implemented like this.
     * <pre>{@code
     * where(table.price().geIf(minPrice))
     * }</pre>
     *
     * @param condition The condition
     * @param block A lambda to create predicate when condition is true
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
