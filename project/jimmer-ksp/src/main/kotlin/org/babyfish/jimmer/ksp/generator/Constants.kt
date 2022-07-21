package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import org.babyfish.jimmer.CircularReferenceException
import org.babyfish.jimmer.DraftConsumer
import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.UnloadedException
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutablePropCategory
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.regex.Pattern

internal const val DRAFT = "Draft"
internal const val PRODUCER = "$"
internal const val IMPLEMENTOR = "Implementor"
internal const val IMPL = "Impl"
internal const val DRAFT_IMPL = "DraftImpl"
internal const val DRAFT_FIELD_EMAIL_PATTERN = "__email_pattern"

internal val INTERNAL_TYPE_CLASS_NAME = Internal::class.asClassName()
internal val IMMUTABLE_PROP_CATEGORY_CLASS_NAME = ImmutablePropCategory::class.asClassName()
internal val IMMUTABLE_TYPE_CLASS_NAME = ImmutableType::class.asClassName()
internal val DRAFT_CONSUMER_CLASS_NAME = DraftConsumer::class.asClassName()
internal val IMMUTABLE_SPI_CLASS_NAME = ImmutableSpi::class.asClassName()
internal val IMMUTABLE_OBJECTS_CLASS_NAME = ImmutableObjects::class.asClassName()
internal val UNLOADED_EXCEPTION_CLASS_NAME = UnloadedException::class.asClassName()
internal val SYSTEM_CLASS_NAME = System::class.asClassName()
internal val DRAFT_SPI_CLASS_NAME = DraftSpi::class.asClassName()
internal val DRAFT_CONTEXT_CLASS_NAME = DraftContext::class.asClassName()
internal val NON_SHARED_LIST_CLASS_NAME = NonSharedList::class.asClassName()
internal val CIRCULAR_REFERENCE_EXCEPTION_CLASS_NAME = CircularReferenceException::class.asClassName()
internal val IMMUTABLE_CREATOR_CLASS_NAME = ClassName("org.babyfish.jimmer.kt", "ImmutableCreator")
internal val DRAFT_SCOPE_CLASS_NAME = ClassName("org.babyfish.jimmer.kt", "DraftScope")
internal val BIG_DECIMAL_CLASS_NAME = BigDecimal::class.asClassName()
internal val BIG_INTEGER_CLASS_NAME = BigInteger::class.asClassName()
internal val PATTERN_CLASS_NAME = Pattern::class.asClassName()

internal const val CURRENT_IMPLEMENTOR = "(__modified ?: __base)"
internal const val CURRENT_IMPL = "(__modified ?: $IMPL(__base).also { __modified = it })"

internal const val EMAIL_PATTERN = "^[^@]+@[^@]+\$"