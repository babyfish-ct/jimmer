package org.babyfish.jimmer.sql.kt.filter.impl

import org.apache.commons.lang3.reflect.TypeUtils
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.filter.FilterArgs
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
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
        val javaQuery = (args as FilterArgsImpl<*>).unwrap()
        (ktFilter as KFilter<Any>).filter(
            KFilterArgsImpl(
                javaQuery,
                TableWrappers.unwrap(args.getTable() as Table<Any>)
            )
        )
    }

    override fun getImmutableType(): ImmutableType =
        immutableType

    override fun getFilterType(): Class<*> =
        ktFilter::class.java

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