package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.asClassName
import org.babyfish.jimmer.DraftConsumer
import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.UnloadedException
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.ImmutablePropCategory
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.runtime.Internal

internal const val DRAFT_SUFFIX = "Draft"
internal const val PRODUCER = "Producer"
internal const val IMPLEMENTOR = "Implementor"
internal const val IMPL = "Impl"
internal const val DRAFT_IMPL = "DraftImpl"

internal val INTERNAL_TYPE_CLASS_NAME = Internal::class.asClassName()
internal val IMMUTABLE_PROP_CATEGORY_CLASS_NAME = ImmutablePropCategory::class.asClassName()
internal val IMMUTABLE_TYPE_CLASS_NAME = ImmutableType::class.asClassName()
internal val DRAFT_CONSUMER_CLASS_NAME = DraftConsumer::class.asClassName()
internal val IMMUTABLE_SPI_CLASS_NAME = ImmutableSpi::class.asClassName()
internal val IMMUTABLE_OBJECTS_CLASS_NAME = ImmutableObjects::class.asClassName()
internal val UNLOADED_EXCEPTION_CLASS_NAME = UnloadedException::class.asClassName()
internal val SYSTEM_CLASS_NAME = System::class.asClassName()
