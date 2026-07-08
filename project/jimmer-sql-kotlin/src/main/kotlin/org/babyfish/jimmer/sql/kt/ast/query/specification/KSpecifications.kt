package org.babyfish.jimmer.sql.kt.ast.query.specification

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.Predicate
import org.babyfish.jimmer.sql.ast.query.specification.PredicateApplier
import java.util.*

internal fun <E : Any> compose(
    operator: Operator,
    specifications: Iterable<KSpecification<out E>?>
): KSpecification<E>? {
    val filteredSpecifications = mutableListOf<KSpecification<out E>>()
    var entityType: Class<*>? = null
    for (specification in specifications) {
        if (specification === null) {
            continue
        }
        filteredSpecifications += specification
        entityType = entityType?.let {
            commonEntityType(it, specification.entityType())
        } ?: specification.entityType()
        if (entityType === null) {
            throw IllegalArgumentException(
                "The specifications cannot be composed because their entity types do not share " +
                        "a common inheritance hierarchy"
            )
        }
    }
    return when (filteredSpecifications.size) {
        0 -> null
        else -> CompositeKSpecification(castEntityType(entityType!!), operator, filteredSpecifications)
    }
}

internal fun <E : Any> negate(specification: KSpecification<out E>?): KSpecification<E>? =
    specification?.let {
        NotKSpecification(castEntityType(it.entityType()), it)
    }

internal enum class Operator {
    AND,
    OR
}

private class CompositeKSpecification<E : Any>(
    private val entityType: Class<E>,
    private val operator: Operator,
    private val specifications: List<KSpecification<out E>>
) : KSpecification<E> {

    override fun entityType(): Class<E> = entityType

    override fun applyTo(args: KSpecificationArgs<E>) {
        val predicates = specifications.map {
            capture(args, it)
        }.toTypedArray()
        args.applier.where(
            if (operator == Operator.AND) {
                Predicate.and(*predicates)
            } else {
                Predicate.or(*predicates)
            }
        )
    }
}

private class NotKSpecification<E : Any>(
    private val entityType: Class<E>,
    private val specification: KSpecification<out E>
) : KSpecification<E> {

    override fun entityType(): Class<E> = entityType

    override fun applyTo(args: KSpecificationArgs<E>) {
        args.applier.where(Predicate.not(capture(args, specification)))
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> PredicateApplier.applyKSpecification(specification: KSpecification<out E>) {
    if (specification is CompositeKSpecification<*> || specification is NotKSpecification<*>) {
        (specification as KSpecification<E>).applyTo(KSpecificationArgs(this))
    } else {
        apply((specification as KSpecification<E>).toJavaSpecification())
    }
}

@Suppress("UNCHECKED_CAST")
private fun <E : Any> capture(
    args: KSpecificationArgs<E>,
    specification: KSpecification<out E>
): Predicate? =
    args.applier.capture {
        args.applier.applyKSpecification(specification)
    }

@Suppress("UNCHECKED_CAST")
private fun <E : Any> castEntityType(type: Class<*>): Class<E> =
    type as Class<E>

private fun commonEntityType(a: Class<*>, b: Class<*>): Class<*>? =
    when {
        isCommonEntityType(a, b) -> a
        isCommonEntityType(b, a) -> b
        else -> allSuperTypes(a).firstOrNull {
            it != Any::class.java && isCommonEntityType(it, b)
        }
    }

private fun isCommonEntityType(candidate: Class<*>, type: Class<*>): Boolean {
    val candidateType = ImmutableType.tryGet(candidate) ?: return false
    val otherType = ImmutableType.tryGet(type) ?: return false
    return candidateType.isEntity &&
            otherType.isEntity &&
            candidateType.isAssignableFrom(otherType)
}

private fun allSuperTypes(type: Class<*>): Set<Class<*>> {
    val types = linkedSetOf<Class<*>>()
    val deque = ArrayDeque<Class<*>>()
    deque += type
    while (deque.isNotEmpty()) {
        val current = deque.removeFirst()
        if (!types.add(current)) {
            continue
        }
        current.superclass?.let(deque::addLast)
        for (interfaceType in current.interfaces) {
            deque += interfaceType
        }
    }
    return types
}
