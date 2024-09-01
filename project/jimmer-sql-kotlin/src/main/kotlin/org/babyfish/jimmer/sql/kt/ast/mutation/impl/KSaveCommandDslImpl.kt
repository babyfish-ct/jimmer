package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.TypedProp
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveCommandImplementor
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.LockMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.TargetTransferMode
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandDsl
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KSaveCommandDslImpl(
    internal var javaCommand: AbstractEntitySaveCommand
): KSaveCommandDsl {

    override fun setMode(mode: SaveMode) {
        javaCommand = javaCommand.setMode(mode)
    }

    override fun setAssociatedModeAll(mode: AssociatedSaveMode) {
        javaCommand = javaCommand.setAssociatedModeAll(mode)
    }

    override fun setAssociatedMode(prop: KProperty1<*, *>, mode: AssociatedSaveMode) {
        javaCommand = javaCommand.setAssociatedMode(prop.toImmutableProp(), mode)
    }

    override fun setAssociatedMode(prop: ImmutableProp, mode: AssociatedSaveMode) {
        javaCommand = javaCommand.setAssociatedMode(prop, mode)
    }

    override fun setAssociatedMode(prop: TypedProp.Association<*, *>, mode: AssociatedSaveMode) {
        javaCommand = javaCommand.setAssociatedMode(prop, mode)
    }

    override fun <E : Any> setKeyProps(vararg keyProps: KProperty1<E, *>) {
        javaCommand = javaCommand.setKeyProps(
            *keyProps.map { it.toImmutableProp() }.toTypedArray()
        )
    }

    override fun <E : Any> setKeyProps(vararg keyProps: TypedProp<E, *>) {
        javaCommand = javaCommand.setKeyProps(*keyProps)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : Any> setOptimisticLock(
        type: KClass<E>,
        block: KSaveCommandDsl.OptimisticLockContext<E>.() -> KNonNullExpression<Boolean>?
    ) {
        javaCommand = (javaCommand as SaveCommandImplementor).setEntityOptimisticLock(
            ImmutableType.get(type.java)
        ) { table, entity ->
            block(
                KSaveCommandDsl.OptimisticLockContext(
                    KNonNullTableExImpl(
                        if (table is TableProxy<*>) {
                            (table as TableProxy<E>).__unwrap()
                        } else {
                            table as TableImplementor<E>
                        },
                        "The table provider by optimistic lock does not support join"
                    ),
                    entity as E
                )
            )?.toJavaPredicate()
        }
    }

    override fun setAutoIdOnlyTargetCheckingAll() {
        javaCommand = javaCommand.setAutoIdOnlyTargetCheckingAll()
    }

    override fun setAutoIdOnlyTargetChecking(prop: KProperty1<*, *>) {
        javaCommand = javaCommand.setAutoIdOnlyTargetChecking(prop.toImmutableProp())
    }

    override fun setDissociateAction(prop: KProperty1<*, *>, action: DissociateAction) {
        javaCommand = javaCommand.setDissociateAction(prop.toImmutableProp(), action)
    }

    override fun setLockMode(lockMode: LockMode) {
        javaCommand = javaCommand.setLockMode(lockMode)
    }

    override fun setTargetTransferMode(prop: KProperty1<*, *>, mode: TargetTransferMode) {
        javaCommand = javaCommand.setTargetTransferMode(prop.toImmutableProp(), mode)
    }

    override fun setTargetTransferModeAll(mode: TargetTransferMode) {
        javaCommand = javaCommand.setTargetTransferModeAll(mode)
    }

    override fun addExceptionTranslator(translator: ExceptionTranslator<*>?) {
        javaCommand = javaCommand.addExceptionTranslator(translator)
    }
}

