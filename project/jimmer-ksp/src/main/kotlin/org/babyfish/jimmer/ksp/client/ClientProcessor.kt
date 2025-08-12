package org.babyfish.jimmer.ksp.client

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.ClientException
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.client.ApiIgnore
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.meta.*
import org.babyfish.jimmer.client.meta.impl.*
import org.babyfish.jimmer.error.CodeBasedException
import org.babyfish.jimmer.error.CodeBasedRuntimeException
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.util.fastResolve
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class ClientProcessor(
    private val ctx: Context,
    private val explicitClientApi: Boolean,
    private val delayedClientTypeNames: Collection<String>?
) {
    private val clientExceptionContext = ClientExceptionContext()

    private val docMetadata = DocMetadata(ctx)

    private val builder = object: SchemaBuilder<KSDeclaration>(null) {

        override fun loadSource(typeName: String): KSClassDeclaration? =
            ctx.resolver.getClassDeclarationByName(typeName)

        override fun throwException(source: KSDeclaration, message: String) {
            throw MetaException(source, null, message)
        }

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

    private val jsonValueTypeNameStack = mutableSetOf<TypeName>()

    fun process() {
        for (file in ctx.resolver.getAllFiles()) {
            for (declaration in file.declarations) {
                builder.handleService(declaration)
            }
        }
        if (delayedClientTypeNames != null) {
            for (delayedClientTypeName in delayedClientTypeNames) {
                builder.handleService(ctx.resolver.getClassDeclarationByName(delayedClientTypeName)!!)
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
        if (declaration !is KSClassDeclaration || !isApiService(declaration)) {
            return
        }
        if (declaration.modifiers.contains(Modifier.INNER)) {
            throw MetaException(
                declaration,
                null,
                "Client API service type cannot be inner type"
            )
        }
        if (declaration.typeParameters.isNotEmpty()) {
            throw MetaException(
                declaration.typeParameters[0],
                null,
                "Client API service cannot declare type parameters"
            )
        }
        val schema = current<SchemaImpl<KSDeclaration>>()
        api(declaration, declaration.toTypeName()) { service ->
            declaration.annotation(Api::class)?.get<List<String>>("value")?.takeIf { it.isNotEmpty() }.let { groups ->
                service.groups = groups
            }
            service.doc = docMetadata.getDoc(declaration)
            for (func in declaration.getDeclaredFunctions()) {
                if (isApiOperation(func)) {
                    handleOperation(func)
                }
            }
            schema.addApiService(service)
        }
    }

    private fun SchemaBuilder<KSDeclaration>.handleOperation(func: KSFunctionDeclaration) {
        val service = current<ApiServiceImpl<KSDeclaration>>()
        if (func.typeParameters.isNotEmpty()) {
            throw MetaException(
                func.typeParameters[0],
                null,
                "Client API function cannot declare type parameters"
            )
        }
        val api = func.annotation(Api::class)
        if (api == null && ApiOperation.AUTO_OPERATION_ANNOTATIONS.all { func.annotation(it) == null }) {
            return
        }
        operation(func, func.simpleName.asString()) { operation ->
            api?.get<List<String>>("value")?.takeIf { it.isNotEmpty() }?.let {
                service.groups?.let { parentGroups ->
                    val illegalGroups = parentGroups.toMutableSet().apply {
                        removeAll(it)
                    }
                    if (illegalGroups.isNotEmpty()) {
                        throw MetaException(
                            operation.source,
                            "It cannot be decorated by \"@" +
                                Api::class.java +
                                "\" with `groups` \"" +
                                illegalGroups +
                                "\" because they are not declared in declaring type \"" +
                                service.typeName +
                                "\""
                        )
                    }
                }
                operation.groups = it
            }
            operation.doc = docMetadata.getDoc(func)
            var index = 0
            for (param in func.parameters) {
                parameter(null, param.name!!.asString()) { parameter ->
                    parameter.originalIndex = index++
                    if (param.annotation(ApiIgnore::class) !== null) {
                        operation.addIgnoredParameter(parameter)
                    } else {
                        typeRef { type ->
                            fillType(param.type)
                            parameter.setType(type)
                        }
                        operation.addParameter(parameter)
                    }
                }
            }
            func.returnType?.let { unresolvedType ->
                val qualifiedName = unresolvedType.realDeclaration.qualifiedName?.asString()
                if (qualifiedName != "kotlin.Unit" && qualifiedName != "kotlin.Nothing") {
                    typeRef { type ->
                        fillType(unresolvedType)
                        operation.setReturnType(type)
                    }
                }
            }
            operation.setExceptionTypeNames(getExceptionTypeNames(func))
            service.addOperation(operation)
        }
    }

    private fun getExceptionTypeNames(func: KSFunctionDeclaration): Set<TypeName> {
        val throws = func.annotation("kotlin.Throws")
            ?: func.annotation("kotlin.jvm.Throws")
            ?: return emptySet()
        val declarations = throws.getClassListArgument(Throws::exceptionClasses)
        val exceptionTypeNames = mutableSetOf<TypeName>()
        for (declaration in declarations) {
            if (declaration.annotation(ClientException::class) !== null) {
                collectExceptionTypeNames(clientExceptionContext[declaration], exceptionTypeNames)
            }
        }
        return exceptionTypeNames
    }

    private fun collectExceptionTypeNames(metadata: ClientExceptionMetadata, exceptionTypeNames: MutableSet<TypeName>) {
        if (metadata.code != null) {
            exceptionTypeNames += metadata.declaration.toTypeName()
        }
        for (subMetadata in metadata.subMetadatas) {
            collectExceptionTypeNames(subMetadata, exceptionTypeNames)
        }
    }

    private fun SchemaBuilder<KSDeclaration>.fillType(type: KSTypeReference) {
        val typeRef = current<TypeRefImpl<KSDeclaration>>()
        try {
            val resolvedType = type.fastResolve()
            determineNullity(resolvedType)
            determineFetchBy(type)
            determineTypeNameAndArguments(resolvedType)
            typeRef.removeOptional()
        } catch (ex: JsonValueTypeChangeException) {
            typeRef.replaceBy(
                ex.typeRef,
                typeRef.isNullable || ex.typeRef.isNullable
            )
        }
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
        val entityType = typeReference.fastResolve()
        if (entityType.declaration.annotation(Entity::class) == null) {
            throw MetaException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Illegal type because \"$entityType\" which is decorated by `@FetchBy` is not entity type"
            )
        }
        val constant = fetchBy[FetchBy::value] ?: throw MetaException(
            ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
            ancestorSource(),
            "The `value` of `@FetchBy` is required"
        )
        val owner = fetchBy
            .getClassArgument(FetchBy::ownerType)
            ?.takeIf { it.fullName != "kotlin.Unit" && it.fullName != "kotlin.Nothing" }
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
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Illegal `@FetcherBy`, the owner type \"" +
                    owner.fullName +
                    "\" does not have companion object"
            )
        val field = companionDeclaration
            .getDeclaredProperties()
            .firstOrNull { it.name == constant }
            ?: throw MetaException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Illegal `@FetcherBy`, the companion object of owner type \"" +
                    owner.fullName +
                    "\" does any field named \"" +
                    constant +
                    "\""
            )
        val fieldType = field.type.fastResolve()
        if (fieldType.declaration.qualifiedName?.asString() != "org.babyfish.jimmer.sql.fetcher.Fetcher") {
            throw MetaException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
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
                    ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
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
        val actualEntityTypeName = argType.fastResolve().toTypeName()
        if (actualEntityTypeName.copy(nullable = false) != entityType.toTypeName().copy(nullable = false)) {
            throw MetaException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Illegal `@FetcherBy`, there is property \"" +
                    constant +
                    "\" in companion object type \"\"" +
                    companionDeclaration.qualifiedName!!.asString() +
                    " but it is not fetcher for \"" +
                    entityType.declaration.qualifiedName!!.asString() +
                    "\""
            )
        }
        typeRef.fetchBy = constant
        typeRef.fetcherOwner = owner.toTypeName()
        typeRef.fetcherDoc = docMetadata.getDoc(field)
    }

    private fun SchemaBuilder<KSDeclaration>.determineTypeNameAndArguments(type: KSType) {
        val typeRef = current<TypeRefImpl<KSDeclaration>>()
        (type.declaration as? KSTypeParameter)?.let {
            typeRef.typeName = it.parentDeclaration!!.toTypeName().typeVariable(type.declaration.simpleName.asString())
            return
        }
        typeRef.typeName = type.realDeclaration.toTypeName()
        when (typeRef.typeName.toString()) {
            "kotlin.BooleanArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.BOOLEAN
                    }
                )
                return
            }
            "kotlin.CharArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.CHAR
                    }
                )
                return
            }
            "kotlin.ByteArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.BYTE
                    }
                )
                return
            }
            "kotlin.ShortArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.SHORT
                    }
                )
                return
            }
            "kotlin.IntArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.INT
                    }
                )
                return
            }
            "kotlin.LongArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.LONG
                    }
                )
                return
            }
            "kotlin.FloatArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.FLOAT
                    }
                )
                return
            }
            "kotlin.DoubleArray" -> {
                typeRef.typeName = TypeName.LIST
                typeRef.addArgument(
                    TypeRefImpl<KSDeclaration>().apply {
                        typeName = TypeName.DOUBLE
                    }
                )
                return
            }
        }
        jsonValueTypeRef(typeRef.typeName)?.let {
            throw JsonValueTypeChangeException(it)
        }
        val simpleName = type.realDeclaration.simpleName.asString()
        val jsonFlag = listOf(
            "JsonNode",
            "JSONObject",
            "JsonObject",
            "JsonElement",
            "ObjectNode",
            "ArrayNode",
        ).any {
            it.equals(simpleName, ignoreCase = true)
        }
        if (jsonFlag) {
            typeRef.typeName = TypeName.OBJECT
            return
        }


        if (typeRef.typeName == TypeName.OBJECT) {
            throw UnambiguousTypeException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Client API system does not accept unambiguous type `java.lang.Object`"
            )
        }
        for (argument in type.arguments) {
            when (argument.variance) {
                Variance.STAR -> throw UnambiguousTypeException(
                    ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                    ancestorSource(),
                    "Client API type system does not accept generic argument <*>"
                )
                Variance.CONTRAVARIANT -> throw UnambiguousTypeException(
                    ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                    ancestorSource(),
                    "Client API type system does not accept generic argument <in...>"
                )
                else -> typeRef { argType ->
                    fillType(argument.type!!)
                    typeRef.addArgument(argType)
                }
            }
        }
    }

    private fun SchemaBuilder<KSDeclaration>.jsonValueTypeRef(typeName: TypeName): TypeRefImpl<KSDeclaration>? {
        val declaration = ctx.resolver.getClassDeclarationByName(typeName.toString()) ?: return null
        val jsonValueFun = declaration
            .getDeclaredFunctions()
            .firstOrNull {
                it.annotation(JsonValue::class) !== null &&
                    it.parameters.isEmpty() &&
                    it.returnType?.realDeclaration?.qualifiedName?.asString().let { n ->
                        n != "kotlin.Unit" && n != "kotlin.Nothing"
                    }
            } ?: return null
        if (!jsonValueTypeNameStack.add(typeName)) {
            throw MetaException(
                ancestorSource(ApiOperationImpl::class.java, ApiParameterImpl::class.java),
                ancestorSource(),
                "Cannot resolve \"@" +
                    JsonValue::class.java.getName() +
                    "\" because of dead recursion: " +
                    jsonValueTypeNameStack
            )
        }
        try {
            var result: TypeRefImpl<KSDeclaration>? = null
            typeRef {
                fillType(jsonValueFun.returnType!!)
                result = it
            }
            return result
        } finally {
            jsonValueTypeNameStack.remove(typeName);
        }
        return null
    }

    private fun SchemaBuilder<KSDeclaration>.fillDefinition(declaration: KSClassDeclaration, immutable: Boolean) {

        val definition = current<TypeDefinitionImpl<KSDeclaration>>()
        definition.isApiIgnore = declaration.annotation(ApiIgnore::class) !== null
        definition.doc = docMetadata.getDoc(declaration)

        if (declaration.classKind == ClassKind.ENUM_CLASS) {
            fillEnumDefinition(declaration)
            return
        }

        definition.kind = if (immutable) {
            TypeDefinition.Kind.IMMUTABLE
        } else {
            TypeDefinition.Kind.OBJECT
        }

        if (!immutable || declaration.classKind == ClassKind.INTERFACE) {
            val isClientException = declaration.annotation(ClientException::class) != null
            for (propDeclaration in declaration.getDeclaredProperties()) {
                if (!propDeclaration.isPublic() ||
                    propDeclaration.annotation(ApiIgnore::class) != null ||
                    propDeclaration.annotation(JsonIgnore::class) != null) {
                    continue
                }
                if (isClientException &&
                    propDeclaration.name.let { it == "code" || it == "fields" }) {
                    continue
                }
                val ksTypeReference = declaration
                    .takeIf { immutable }
                    ?.let {
                        ctx.typeOf(declaration)
                            .properties[propDeclaration.name]!!
                            .converterMetadata
                            ?.targetType
                    }
                    ?.let {
                        val resolver = ctx.resolver
                        when (it.variance) {
                            Variance.STAR -> resolver.createKSTypeReferenceFromKSType(resolver.builtIns.anyType.makeNullable())
                            Variance.CONTRAVARIANT -> resolver.createKSTypeReferenceFromKSType(resolver.builtIns.anyType)
                            else -> it.type
                        }
                    } ?: propDeclaration.type
                prop(propDeclaration, propDeclaration.name) { prop ->
                    try {
                        typeRef { type ->
                            fillType(ksTypeReference)
                            prop.setType(type)
                        }
                        prop.doc = docMetadata.getDoc(propDeclaration)
                        definition.addProp(prop)
                    } catch (ex: UnambiguousTypeException) {
                        // Do nothing
                    }
                }
            }
            for (funcDeclaration in declaration.getDeclaredFunctions()) {
                if (!funcDeclaration.isConstructor() &&
                    funcDeclaration.isPublic() &&
                    funcDeclaration.parameters.isEmpty() &&
                    funcDeclaration.annotation(JsonIgnore::class) == null &&
                    funcDeclaration.annotation(ApiIgnore::class) == null) {
                    val returnTypReference = funcDeclaration.returnType ?: continue
                    val returnTypeName = returnTypReference.realDeclaration.qualifiedName?.asString() ?: continue
                    if (returnTypeName == "kotlin.Unit" || returnTypeName == "kotlin.Nothing") {
                        continue
                    }
                    val name = StringUtil
                        .propName(funcDeclaration.simpleName.asString(), returnTypeName == "kotlin.Boolean")
                        ?: continue
                    try {
                        prop(funcDeclaration, name) { prop ->
                            typeRef { type ->
                                fillType(returnTypReference)
                                prop.setType(type)
                            }
                            definition.addProp(prop)
                        }
                    } catch (ex: UnambiguousTypeException) {
                        // Do nothing
                    }
                }
            }
        }

        if (declaration.annotation(ClientException::class) != null) {
            val metadata = clientExceptionContext[declaration]
            if (metadata.code !== null) {
                definition.error = TypeDefinition.Error(
                    metadata.family,
                    metadata.code
                )
            }
        }

        if (declaration.classKind == ClassKind.CLASS || declaration.classKind == ClassKind.INTERFACE) {
            for (superTypeReference in declaration.superTypes) {
                val superDeclaration = superTypeReference.realDeclaration
                if (superDeclaration.annotation(ApiIgnore::class) == null) {
                    val superName = superDeclaration.toTypeName()
                    if (superName.isGenerationRequired &&
                        superName != CODE_BASED_EXCEPTION_NAME &&
                        superName != CODE_BASED_RUNTIME_EXCEPTION_NAME) {
                        typeRef { superType ->
                            fillType(superTypeReference)
                            definition.addSuperType(superType)
                        }
                    }
                }
            }
        }
    }

    private fun SchemaBuilder<KSDeclaration>.fillEnumDefinition(declaration: KSClassDeclaration) {

        val definition = current<TypeDefinitionImpl<KSDeclaration>>()
        definition.kind = TypeDefinition.Kind.ENUM

        for (childDeclaration in declaration.declarations) {
            if (childDeclaration is KSClassDeclaration && childDeclaration.classKind == ClassKind.ENUM_ENTRY) {
                constant(childDeclaration, childDeclaration.simpleName.asString()) {
                    it.doc = docMetadata.getDoc(childDeclaration)
                    definition.addEnumConstant(it)
                }
            }
        }
    }

    private fun isApiService(declaration: KSClassDeclaration): Boolean {
        if (!ctx.include(declaration)) {
            return false
        }
        if (declaration.annotation(ApiIgnore::class) !== null) {
            return false
        }
        if (declaration.annotation(Api::class) !== null) {
            return true
        }
        if (!explicitClientApi) {
            return false
        }
        return declaration.annotation("org.springframework.web.bind.annotation.RestController") !== null
    }

    private fun isApiOperation(declaration: KSFunctionDeclaration): Boolean {
        if (!declaration.isPublic()) {
            return false
        }
        if (declaration.annotation(ApiIgnore::class) !== null) {
            return false
        }
        if (declaration.annotation(Api::class) !== null) {
            return true
        }
        if (!explicitClientApi) {
            return false
        }
        return ApiOperation.AUTO_OPERATION_ANNOTATIONS.any { declaration.annotation(it) !== null }
    }

    private class UnambiguousTypeException(
        declaration: KSDeclaration,
        childDeclaration: KSDeclaration?,
        reason: String,
        cause: Throwable? = null
    ) : MetaException(declaration, childDeclaration, reason, cause)

    private class JsonValueTypeChangeException(
        val typeRef: TypeRefImpl<KSDeclaration>
    ): RuntimeException()

    companion object {

        fun KSDeclaration.toTypeName(): TypeName {
            val simpleNames = mutableListOf<String>()
            var d: KSDeclaration? = this
            while (d is KSClassDeclaration) {
                simpleNames += d.simpleName.asString()
                d = d.parentDeclaration
            }
            simpleNames.reverse()
            return TypeName.of(packageName.asString(), simpleNames)
        }

        val KSType.realDeclaration: KSDeclaration
            get() = declaration.let {
                if (it is KSTypeAlias) {
                    it.findActualType()
                } else {
                    it
                }
            }

        val KSTypeReference.realDeclaration: KSDeclaration
            get() = resolve().declaration.let {
                if (it is KSTypeAlias) {
                    it.findActualType()
                } else {
                    it
                }
            }

        @Suppress("UNCHECKED_CAST")
        private fun TypeRefImpl<KSDeclaration>.removeOptional() {
            if (typeName == TypeName.OPTIONAL) {
                val target = arguments[0] as TypeRefImpl<KSDeclaration>
                replaceBy(target, null)
            }
        }

        private val CODE_BASED_EXCEPTION_NAME = TypeName.of(
            CodeBasedException::class.java
        )

        private val CODE_BASED_RUNTIME_EXCEPTION_NAME = TypeName.of(
            CodeBasedRuntimeException::class.java
        )
    }
}
