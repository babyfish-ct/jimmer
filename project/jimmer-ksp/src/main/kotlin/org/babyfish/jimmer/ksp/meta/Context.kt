package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import kotlin.reflect.KClass

class Context(
    val resolver: Resolver
) {
    val intType: KSType = resolver.builtIns.intType

    val collectionType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.Collection")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    val listType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.List")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    val mapType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.Map")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    private val typeMap = mutableMapOf<KSClassDeclaration, ImmutableType>()

    fun typeOf(classDeclaration: KSClassDeclaration): ImmutableType =
        typeMap[classDeclaration] ?:
            ImmutableType(this, classDeclaration).also {
                typeMap[classDeclaration] = it
            }

    fun typeAnnotationOf(classDeclaration: KSClassDeclaration): KSAnnotation? {
        var sqlAnnotation: KSAnnotation? = null
        for (sqlAnnotationType in SQL_ANNOTATION_TYPES) {
            val anno = classDeclaration.annotation(sqlAnnotationType) ?: continue
            if (sqlAnnotation !== null) {
                throw MetaException(
                    "${classDeclaration.qualifiedName!!.asString()} cannot be decorated by both " +
                        "@${sqlAnnotation.fullName} and ${anno.fullName}"
                )
            }
            sqlAnnotation = anno
        }
        return sqlAnnotation ?: classDeclaration.annotation(Immutable::class)
    }

    companion object {
        private val SQL_ANNOTATION_TYPES = listOf(
            Entity::class,
            MappedSuperclass::class,
            Embeddable::class
        )
    }
}