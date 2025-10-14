package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.lang.Generics
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.FilterArgs
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.filter.KFilter

internal open class JavaFilter(
    protected val kotlinFilter: KFilter<*>
) : FilterWrapper, Filter<Props> {

    private val immutableType: ImmutableType =
        if (kotlinFilter is FilterWrapper) {
            kotlinFilter.immutableType
        } else {
            Generics
                .getTypeArguments(kotlinFilter::class.java, KFilter::class.java)[0]
                .let {
                    if (it !is Class<*>) {
                        throw IllegalArgumentException(
                            "\"${kotlinFilter::class.qualifiedName}\" is illegal, " +
                                "the type argument of \"${KFilter::class.qualifiedName}\" " +
                                "is not specified"
                        )
                    }
                    ImmutableType.get(it)
                }
        }

    @Suppress("UNCHECKED_CAST")
    override fun filter(args: FilterArgs<Props>?) {
        val javaQuery = (args as FilterArgsImpl<*>).unwrap()
        (kotlinFilter as KFilter<Any>).filter(
            KFilterArgsImpl(
                javaQuery,
                args.getTable().let {
                    if (it is TableImplementor<*>) {
                        it as TableImplementor<Any>
                    } else {
                        (it as TableProxy<Any>).__unwrap()
                    }
                }
            )
        )
    }

    override fun getImmutableType(): ImmutableType =
        immutableType

    override fun getFilterType(): Class<*> =
        if (kotlinFilter is FilterWrapper) {
            kotlinFilter.filterType
        } else {
            kotlinFilter.javaClass
        }

    override fun unwrap(): Any =
        kotlinFilter

    override fun hashCode(): Int =
        FilterWrapper.unwrap(kotlinFilter).hashCode()

    override fun equals(other: Any?): Boolean =
        FilterWrapper.unwrap(kotlinFilter) == FilterWrapper.unwrap(other)

    override fun toString(): String =
        "JavaFilter(kotlinFilter=$kotlinFilter)"
}