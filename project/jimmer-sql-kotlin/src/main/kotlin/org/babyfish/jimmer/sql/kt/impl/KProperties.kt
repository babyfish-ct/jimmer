package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod

internal fun KProperty1<*, *>.toImmutableProp(): ImmutableProp =
    ImmutableType
        .get(getter.javaMethod!!.declaringClass)
        .getProp(name)