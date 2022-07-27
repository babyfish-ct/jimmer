package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.DeleteAction
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

internal class KSaveCommandDslImpl(
    private val javaCfg: AbstractEntitySaveCommand.Cfg
): KSaveCommandDsl {

    override fun setMode(mode: SaveMode) {
        javaCfg.setMode(mode)
    }

    override fun <E : Any> setKeyProps(vararg keyProps: KProperty1<E, *>) {
        javaCfg.setKeyProps(
            *keyProps.map {
                ImmutableType
                    .get(it.getter.javaMethod!!.declaringClass)
                    .getProp(it.name)
            }.toTypedArray()
        )
    }

    override fun setAutoAttachingAll() {
        javaCfg.setAutoAttachingAll()
    }

    override fun setAutoAttaching(prop: KProperty1<*, *>) {
        javaCfg.setAutoAttaching(
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name)
        )
    }

    override fun setDeleteAction(prop: KProperty1<*, *>, action: DeleteAction) {
        javaCfg.setDeleteAction(
            ImmutableType
                .get(prop.getter.javaMethod!!.declaringClass)
                .getProp(prop.name),
            action
        )
    }
}

