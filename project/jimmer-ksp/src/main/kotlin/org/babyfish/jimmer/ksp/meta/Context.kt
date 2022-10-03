package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass

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
        val entity = classDeclaration.annotation(Entity::class)
        val mappedSuperClass = classDeclaration.annotation(MappedSuperclass::class)
        if (entity != null && mappedSuperClass != null) {
            throw MetaException(
                "${classDeclaration.qualifiedName!!.asString()} cannot be decorated by both @Entity and @MappedSuperClass"
            )
        }
        return entity
            ?: mappedSuperClass
            ?: classDeclaration.annotation(Immutable::class)
    }
}