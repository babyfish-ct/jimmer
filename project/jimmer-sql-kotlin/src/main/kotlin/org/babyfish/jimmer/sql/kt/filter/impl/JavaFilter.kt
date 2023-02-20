package org.babyfish.jimmer.sql.kt.filter.impl

import org.apache.commons.lang3.reflect.TypeUtils
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.FilterArgs
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.filter.impl.TypeAware
import org.babyfish.jimmer.sql.kt.filter.KFilter

internal open class JavaFilter constructor(
    protected val kFilter: KFilter<*>
) : TypeAware, Filter<Props> {

    private val immutableType: ImmutableType =
        if (kFilter is TypeAware) {
            kFilter.immutableType
        } else {
            TypeUtils
                .getTypeArguments(kFilter::class.java, KFilter::class.java)
                .values
                .first()
                .let {
                    if (it !is Class<*>) {
                        throw IllegalArgumentException(
                            "\"${kFilter::class.qualifiedName}\" is illegal, " +
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
        (kFilter as KFilter<Any>).filter(
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
        if (kFilter is TypeAware) {
            kFilter.filterType
        } else {
            kFilter.javaClass
        }

    override fun unwrap(): Any =
        kFilter

    override fun toString(): String =
        "JavaFilter(ktFilter=$kFilter)"
}