package org.babyfish.jimmer.ksp.client

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.client.Api
import org.babyfish.jimmer.client.ApiIgnore
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.meta.DefaultFetcherOwner
import org.babyfish.jimmer.client.meta.Doc
import org.babyfish.jimmer.client.meta.TypeDefinition
import org.babyfish.jimmer.client.meta.TypeName
import org.babyfish.jimmer.client.meta.impl.*
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class ClientProcessor(
    private val ctx: Context,
    private val delayedFiles: Collection<KSFile>?
) {
    private val builder = object: SchemaBuilder<KSDeclaration>(null) {

        override fun loadSource(typeName: String): KSDeclaration? =
            ctx.resolver.getClassDeclarationByName(typeName)

        override fun typeNameNotFound(typeName: String) =
            throw MetaException(
                ancestorSource(),
                "Cannot resolve the type name \"$typeName\""
            )

        override fun fillDefinition(source: KSDeclaration?) {
            val declaration = source as KSClassDeclaration
            fillDefinition(
                declaration,
                declaration.annotation(Immutable::class) !== null ||
                    declaration.annotation(Entity::class) !== null ||
                    declaration.annotation(MappedSuperclass::class) !== null ||
                    declaration.annotation(Embeddable::class) !== null
            )
        }
    }

    fun process() {

        for (file in ctx.resolver.getAllFiles()) {
            for (declaration in file.declarations) {
                builder.handleService(declaration)
            }
        }
        if (delayedFiles !== null) {
            for (file in delayedFiles) {
                for (declaration in file.declarations) {
                    builder.handleService(declaration)
                }
            }
        }
        val schema = builder.build()

        ctx.environment.codeGenerator.createNewFile(
            Dependencies(false, *ctx.resolver.getAllFiles().toList().toTypedArray()),
            "META-INF.jimmer",
            "client",
            ""
        ).use {
            Schemas.writeTo(schema, OutputStreamWriter(it, StandardCharsets.UTF_8))
        }
    }

    private fun SchemaBuilder<KSDeclaration>.handleService(declaration: KSDeclaration) {
        if (declaration !is KSClassDeclaration ||
            !isApiService(declaration) ||
            !ctx.include(declaration) ||
            declaration.annotation(ApiIgnore::class) != null
        ) {
            return
        }
        if (declaration.modifiers.contains(Modifier.INNER)) {
            throw MetaException(
                declaration,
                "the API service type cannot be inner type"
            )
        }
        if (declaration.typeParameters.isNotEmpty()) {
            throw MetaException(
                declaration.typeParameters[0],
                "API service cannot declare type parameters"
            )
        }
        val schema = current<SchemaImpl<KSDeclaration>>()
        api(declaration, declaration.fullName) { service ->
            declaration.annotation(Api::class)?.get<List<String>>("groups")?.takeIf { it.isNotEmpty() }.let { groups ->
                service.groups = groups
            }
            declaration.docString?.let {
                service.doc = Doc.parse(it)
            }
            for (func in declaration.getDeclaredFunctions()) {
                if (!func.isPublic() || func.annotation(ApiIgnore::class) != null) {
                    continue
                }
                handleOperation(func)
            }
            schema.addApiService(service)
        }
    }

    private fun SchemaBuilder<KSDeclaration>.handleOperation(func: KSFunctionDeclaration) {
        val service = current<ApiServiceImpl<KSDeclaration>>()
        operation(func, func.simpleName.asString()) { operation ->
            if (func.typeParameters.isNotEmpty()) {
                throw MetaException(
                    func.typeParameters[0],
                    "API function cannot declare type parameters"
                )
            }
            func.annotation(Api::class)?.get<List<String>>("groups")?.takeIf { it.isNotEmpty() }?.let {
                operation.groups = it
            }
            func.docString?.let {
                operation.doc = Doc.parse(it)
            }
            for (param in func.parameters) {
                parameter(null, param.name!!.asString()) { parameter ->
                    typeRef { type ->
                        fillType(param.type)
                        parameter.setType(type)
                    }
                    operation.addParameter(parameter)
                }
            }
            func.returnType?.let { unresolvedType ->
                if (unresolvedType.resolve().declaration.qualifiedName?.asString() != "kotlin.Unit") {
                    typeRef { type ->
                        fillType(unresolvedType)
                        operation.setReturnType(type)
                    }
                }
            }
            service.addOperation(operation)
        }
    }

    private fun SchemaBuilder<KSDeclaration>.fillType(type: KSTypeReference) {
        val resolvedType = type.resolve()
        determineNullity(resolvedType)
        determineFetchBy(type)
        determineTypeAndArguments(resolvedType)
    }

    private fun SchemaBuilder<KSDeclaration>.determineNullity(type: KSType) {
        val typeRef = current<TypeRefImpl<KSDeclaration>>()
        typeRef.isNullable = type.isMarkedNullable
    }

    private fun SchemaBuilder<KSDeclaration>.determineFetchBy(typeReference: KSTypeReference) {
        val typeRef = current<TypeRefImpl<KSDeclaration>>()
        val fetchBy = typeReference
            .annotations
            .firstOrNull { it.fullName == FetchBy::class.qualifiedName }
            ?: return
        val entityType = typeReference.resolve()
        entityType.declaration.run {
            annotation(Immutable::class) !== null ||
                annotation(Entity::class) !== null ||
                annotation(MappedSuperclass::class) !== null ||
                annotation(Embeddable::class) !== null
        }.takeIf { it } ?: throw MetaException(
            ancestorSource(),
            "Illegal type because \"$entityType\" which is decorated by `@FetchBy` is not entity type"
        )
        val constant = fetchBy[FetchBy::value] ?: throw MetaException(
            ancestorSource(),
            "The `value` of `@FetchBy` is required"
        )
        val owner = fetchBy
            .getClassArgument(FetchBy::ownerType)
            ?.takeIf { it.fullName != "kotlin.Unit" }
            ?: ancestorSource(ApiServiceImpl::class.java, TypeDefinitionImpl::class.java).let {
                it
                    ?.annotation(DefaultFetcherOwner::class)
                    ?.getClassArgument(DefaultFetcherOwner::value)
                    ?: it
            } as KSClassDeclaration
        val companionDeclaration = owner
            .declarations
            .firstOrNull { it is KSClassDeclaration && it.isCompanionObject } as KSClassDeclaration?
            ?: throw MetaException(
                ancestorSource(),
                "Illegal `@FetcherBy`, the owner type \"" +
                    owner.fullName +
                    "\" does not have companion object"
            )
        val field = companionDeclaration
            .getDeclaredProperties()
            .firstOrNull { it.name == constant }
            ?: throw MetaException(
                ancestorSource(),
                "Illegal `@FetcherBy`, the companion object of owner type \"" +
                    owner.fullName +
                    "\" does any field named \"" +
                    constant +
                    "\""
            )
        val fieldType = field.type.resolve()
        if (fieldType.declaration.qualifiedName?.asString() != "org.babyfish.jimmer.sql.fetcher.Fetcher") {
            throw MetaException(
                ancestorSource(),
                "Illegal `@FetcherBy`, there is static field \"" +
                    constant +
                    "\" in companion object \"" +
                    companionDeclaration.qualifiedName!!.asString() +
                    "\" but it is not \"org.babyfish.jimmer.sql.fetcher.Fetcher\""
            )
        }
        val argType = fieldType.arguments[0].let {
            if (it.variance != Variance.INVARIANT) {
                throw MetaException(
                    ancestorSource(),
                    "Illegal `@FetcherBy`, there is static field \"" +
                        constant +
                        "\" in companion object \"" +
                        companionDeclaration.qualifiedName!!.asString() +
                        "\" but the variance of its generic argument is not `INVARIANT` (It is *, out, or in...)"
                )
            }
            it.type!!
        }
        val actualEntityTypeName = argType.resolve().toTypeName()
        if (actualEntityTypeName != entityType.toTypeName()) {
            throw MetaException(
                ancestorSource(),
                "Illegal `@FetcherBy`, there is property \"" +
                    constant +
                    "\" companion object type \"\"" +
                    companionDeclaration.qualifiedName!!.asString() +
                    " but it is not fetcher for \"" +
                    entityType.declaration.qualifiedName!!.asString() +
                    "\""
            )
        }
        typeRef.fetchBy = constant
        typeRef.fetcherOwner = owner.qualifiedName!!.asString()
    }

    private fun SchemaBuilder<KSDeclaration>.determineTypeAndArguments(type: KSType) {
        val typeRef = current<TypeRefImpl<KSDeclaration>>()
        (type.declaration as? KSTypeParameter)?.let {
            typeRef.typeName = it.parentDeclaration!!.toTypeName().typeVariable(type.declaration.simpleName.asString())
            return
        }
        typeRef.typeName = processTypeName(type.declaration.toTypeName())
        for (argument in type.arguments) {
            when (argument.variance) {
                Variance.STAR -> throw MetaException(
                    ancestorSource(),
                    "API type system does not accept generic argument <*>"
                )
                Variance.CONTRAVARIANT -> throw MetaException(
                    ancestorSource(),
                    "API type system does not accept generic argument <in...>"
                )
                else -> typeRef { argType ->
                    fillType(argument.type!!)
                    typeRef.addArgument(argType)
                }
            }
        }
    }

    private fun SchemaBuilder<KSDeclaration>.fillDefinition(declaration: KSClassDeclaration, immutable: Boolean) {
        val definition = current<TypeDefinitionImpl<KSDeclaration>>()
        definition.isImmutable = immutable

        if (!immutable || declaration.classKind == ClassKind.INTERFACE) {
            for (propDeclaration in declaration.getDeclaredProperties()) {
                if (!propDeclaration.isPublic()) {
                    continue
                }
                prop(propDeclaration, propDeclaration.name) { prop ->
                    typeRef { type ->
                        fillType(propDeclaration.type)
                        prop.setType(type)
                    }
                    definition.addProp(prop)
                }
            }
        }

        if (declaration.classKind == ClassKind.CLASS || declaration.classKind == ClassKind.INTERFACE) {
            for (superTypeReference in declaration.superTypes) {
                val superName = superTypeReference.resolve().declaration.toTypeName()
                if (TypeDefinition.isGenerationRequired(processTypeName(superName))) {
                    typeRef { superType ->
                        fillType(superTypeReference)
                        definition.addSuperType(superType)
                    }
                }
            }
        }
    }

    companion object {

        fun isApiService(declaration: KSDeclaration): Boolean {
            return declaration.annotation(Api::class) !== null ||
                declaration.annotations {
                    it.fullName == "org.springframework.web.bind.annotation.RestController"
                }.isNotEmpty()
        }

        fun processTypeName(typeName: TypeName): TypeName =
            when (typeName.toString()) {
                "kotlin.Unit" -> TypeName.VOID
                "kotlin.Boolean" -> TypeName.BOOLEAN
                "kotlin.Char" -> TypeName.CHAR
                "kotlin.Byte" -> TypeName.BYTE
                "kotlin.Short" -> TypeName.SHORT
                "kotlin.Int" -> TypeName.INT
                "kotlin.Long" -> TypeName.LONG
                "kotlin.Float" -> TypeName.FLOAT
                "kotlin.Double" -> TypeName.DOUBLE
                "kotlin.Any" -> TypeName.OBJECT
                "kotlin.String" -> TypeName.STRING
                "kotlin.collections.Iterable" -> TypeName.ITERABLE
                "kotlin.collections.Collection" -> TypeName.COLLECTION
                "kotlin.collections.List" -> TypeName.LIST
                "kotlin.collections.Set" -> TypeName.SET
                "kotlin.collections.Map" -> TypeName.MAP
                "kotlin.collections.MutableIterable" -> TypeName.ITERABLE
                "kotlin.collections.MutableCollection" -> TypeName.COLLECTION
                "kotlin.collections.MutableList" -> TypeName.LIST
                "kotlin.collections.MutableSet" -> TypeName.SET
                "kotlin.collections.MutableMap" -> TypeName.MAP
                else -> typeName
            }

        fun KSDeclaration.toTypeName(): TypeName {
            val simpleNames = mutableListOf<String>()
            var d: KSDeclaration? = this
            while (d is KSClassDeclaration) {
                simpleNames += d.simpleName.asString()
                d = d.parentDeclaration
            }
            simpleNames.reverse()
            return TypeName(packageName.asString(), simpleNames)
        }
    }
}