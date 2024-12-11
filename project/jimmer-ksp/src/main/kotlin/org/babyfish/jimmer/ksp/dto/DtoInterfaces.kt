package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.babyfish.jimmer.dto.compiler.DtoAstException
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.MetaException
import org.babyfish.jimmer.ksp.client.ClientProcessor.Companion.realDeclaration
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.name
import org.babyfish.jimmer.ksp.util.fastResolve

fun abstractPropNames(ctx: Context, dtoType: DtoType<ImmutableType, ImmutableProp>): Set<String> {
    if (dtoType.superInterfaces.isEmpty()) {
        return emptySet()
    }
    val propNames = mutableSetOf<String>()
    val handledTypeNames = mutableSetOf<String>()
    for (typeRef in dtoType.superInterfaces) {
        val declaration = ctx.resolver.getClassDeclarationByName(typeRef.typeName)
            ?: error("Internal bug: super interface \"${typeRef.typeName}\" does not exists")
        if (declaration.classKind != ClassKind.INTERFACE) {
            throw DtoAstException(
                dtoType.dtoFile,
                typeRef.line,
                typeRef.col,
                "The super type \"${typeRef.typeName}\" is not interface"
            )
        }
        collectMembers(declaration, ctx, handledTypeNames, propNames)
    }
    return propNames
}

private fun collectMembers(
    declaration: KSClassDeclaration,
    ctx: Context,
    handledTypeNames: MutableSet<String>,
    propNames: MutableSet<String>
) {
    val qualifiedName = declaration.qualifiedName!!.asString()
    if (!handledTypeNames.add(qualifiedName)) {
        return
    }
    for (func in declaration.getDeclaredFunctions()) {
        val name = func.simpleName.asString()
        when  {
            name == "hashCode" && func.parameters.isEmpty() -> continue
            name == "equals" && func.parameters.size == 1 -> continue
            name == "toString" && func.parameters.isEmpty() -> continue
        }
        if (func.isAbstract) {
            throw MetaException(
                func,
                "Illegal abstract method, the declaring interface \"" +
                    qualifiedName +
                    "\" or its derived interface is used as the super interface of generated DTO type " +
                    "so that this abstract method cannot have generic parameters"
            )
        }
    }
    for (prop in declaration.getDeclaredProperties()) {
        propNames += prop.name
    }
    for (superType in declaration.superTypes) {
        val superDeclaration = superType.fastResolve().declaration as KSClassDeclaration
        collectMembers(superDeclaration, ctx, handledTypeNames, propNames)
    }
}
