package org.babyfish.jimmer.sql.kt.filter.impl

import org.apache.commons.lang3.reflect.TypeUtils
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.FilterArgs
import org.babyfish.jimmer.sql.filter.impl.AbstractFilterArgsImpl
import org.babyfish.jimmer.sql.filter.impl.TypeAwareFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter

internal open class JavaFilter constructor(
    protected val ktFilter: KFilter<*>
) : TypeAwareFilter {

    private val immutableType: ImmutableType = TypeUtils
        .getTypeArguments(ktFilter::class.java, KFilter::class.java)
        .values
        .first()
        .let {
            if (it !is Class<*>) {
                throw IllegalArgumentException(
                    "\"${ktFilter::class.qualifiedName}\" is illegal, " +
                        "the type argument of \"${KFilter::class.qualifiedName}\" " +
                        "is not specified"
                )
            }
            ImmutableType.get(it)
        }

    @Suppress("UNCHECKED_CAST")
    override fun filter(args: FilterArgs<Props>?) {
        val javaQuery = (args as AbstractFilterArgsImpl<*>).unwrap()
        (ktFilter as KFilter<Any>).filter(KFilterArgsImpl(javaQuery))
    }

    override fun getImmutableType(): ImmutableType =
        immutableType

    override fun hashCode(): Int =
        ktFilter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is JavaFilter) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}