package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import org.babyfish.jimmer.dto.compiler.DtoAstException
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.dto.compiler.DtoUtils
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.ksp.dto.DtoContext
import org.babyfish.jimmer.ksp.dto.DtoException
import org.babyfish.jimmer.ksp.generator.*
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.math.min

class JimmerProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val processed = AtomicBoolean()

    private val includes: Array<String>? =
        environment.options["jimmer.source.includes"]
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.trim().split("\\s*,[,;]\\s*").toTypedArray()
            }

    private val excludes: Array<String>? =
        environment.options["jimmer.source.excludes"]
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.trim().split("\\s*[,;]\\s*").toTypedArray()
            }

    private val dtoDirs: Collection<String> =
        environment.options["jimmer.dto.dirs"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { text ->
                text.split("\\s*[,:;]\\s*")
                    .map {
                        when {
                            it == "" || it == "/" -> null
                            it.startsWith("/") -> it.substring(1)
                            it.endsWith("/") -> it.substring(0, it.length - 1)
                            else -> it.takeIf { it.isNotEmpty() }
                        }
                    }
                    .filterNotNull()
                    .toSet()
            }
            ?.let { DtoUtils.standardDtoDirs(it) }
            ?: listOf("src/main/dto")

    private val dtoMutable: Boolean =
        environment.options["jimmer.dto.mutable"]?.trim() == "true"

    private val processedDtoPaths = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!processed.compareAndSet(false, true)) {
            return emptyList()
        }

        return try {
            val ctx = Context(resolver)

            val classDeclarationMultiMap = findModelMap(ctx)
            val dtoTypeMap = findDtoTypeMap(ctx)
            val errorDeclarations = findErrorTypes(resolver)

            generateJimmerTypes(ctx, classDeclarationMultiMap)
            generateDtoTypes(resolver, dtoTypeMap)
            generateErrorTypes(resolver, errorDeclarations)

            return classDeclarationMultiMap.values.flatten()
        } catch (ex: MetaException) {
            environment.logger.error(ex.message!!, ex.declaration)
            emptyList()
        }
    }

    private fun findModelMap(ctx: Context): Map<KSFile, List<KSClassDeclaration>> {
        val modelMap = mutableMapOf<KSFile, MutableList<KSClassDeclaration>>()
        for (file in ctx.resolver.getNewFiles()) {
            for (classDeclaration in file.declarations.filterIsInstance<KSClassDeclaration>()) {
                if (include(classDeclaration)) {
                    val annotation = ctx.typeAnnotationOf(classDeclaration)
                    if (classDeclaration.qualifiedName !== null && annotation != null) {
                        if (classDeclaration.classKind != ClassKind.INTERFACE) {
                            throw GeneratorException(
                                "The immutable interface '${classDeclaration.fullName}' " +
                                    "must be interface"
                            )
                        }
                        if (classDeclaration.typeParameters.isNotEmpty()) {
                            throw GeneratorException(
                                "The immutable interface '${classDeclaration.fullName}' " +
                                    "cannot have type parameters"
                            )
                        }
                        if (classDeclaration.isPrivate() || classDeclaration.isProtected()) {
                            throw GeneratorException(
                                "The immutable interface '${classDeclaration.fullName}' " +
                                    "cannot be private or protected'"
                            )
                        }
                        modelMap.computeIfAbsent(file) { mutableListOf() } +=
                            classDeclaration
                    }
                }
            }
        }
        for (declarations in modelMap.values) {
            for (declaration in declarations) {
                ctx.typeOf(declaration)
            }
        }
        ctx.resolve()
        return modelMap
    }

    private fun findDtoTypeMap(
        ctx: Context
    ): Map<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>> {
        val dtoTypeMap = mutableMapOf<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>>()
        val newCtx = Context(ctx)
        val dtoCtx = DtoContext(newCtx.resolver.getAllFiles().firstOrNull(), dtoDirs)
        val immutableTypeMap = mutableMapOf<KspDtoCompiler, ImmutableType>()
        for (dtoFile in dtoCtx.dtoFiles) {
            if (!processedDtoPaths.add(dtoFile.path)) {
                continue
            }
            val compiler = try {
                KspDtoCompiler(dtoFile)
            } catch (ex: DtoAstException) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.path +
                        "\": " +
                        ex.message,
                    ex
                )
            } catch (ex: Throwable) {
                throw DtoException(
                    "Failed to read \"" +
                        dtoFile.path +
                        "\": " +
                        ex.message,
                    ex
                )
            }
            val classDeclaration = newCtx.resolver.getClassDeclarationByName(compiler.sourceTypeName)
            if (classDeclaration === null) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.path +
                        "\": No entity type \"" +
                        compiler.sourceTypeName +
                        "\""
                )
            }
            if (!include(classDeclaration)) {
                continue
            }
            if (classDeclaration.annotation(Entity::class) == null) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.path +
                        "\": the \"" +
                        compiler.sourceTypeName +
                        "\" is not decorated by \"@" +
                        Entity::class.qualifiedName +
                        "\""
                )
            }
            immutableTypeMap[compiler] = newCtx.typeOf(classDeclaration)
        }
        newCtx.resolve()
        for ((compiler, immutableType) in immutableTypeMap) {
            dtoTypeMap.computeIfAbsent(immutableType) {
                mutableListOf()
            } += compiler.compile(immutableType)
        }
        return dtoTypeMap
    }

    private fun findErrorTypes(resolver: Resolver): List<KSClassDeclaration> =
        resolver
            .getNewFiles()
            .flatMap { file ->
                file
                    .declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter{
                        it.classKind == ClassKind.ENUM_CLASS &&
                            it.annotation(ErrorFamily::class) != null &&
                            include(it)
                    }
            }
            .toList()

    private fun generateJimmerTypes(
        ctx: Context,
        classDeclarationMultiMap: Map<KSFile, List<KSClassDeclaration>>
    ) {
        val allFiles = ctx.resolver.getAllFiles().toList()
        for ((file, classDeclarations) in classDeclarationMultiMap) {
            DraftGenerator(environment.codeGenerator, ctx, file, classDeclarations)
                .generate(allFiles)
            val sqlClassDeclarations = classDeclarations.filter {
                it.annotation(Entity::class) !== null ||
                    it.annotation(MappedSuperclass::class) !== null ||
                    it.annotation(Embeddable::class) != null
            }
            if (sqlClassDeclarations.size > 1) {
                throw GeneratorException(
                    "The $file declares several types decorated by " +
                        "@${Entity::class.qualifiedName}, @${MappedSuperclass::class.qualifiedName} " +
                        "or ${Embeddable::class.qualifiedName}: " +
                        sqlClassDeclarations.joinToString { it.fullName }
                )
            }
            if (sqlClassDeclarations.isNotEmpty()) {
                val sqlClassDeclaration = sqlClassDeclarations[0]
                PropsGenerator(environment.codeGenerator, ctx, file, sqlClassDeclaration)
                    .generate(allFiles)
                if (sqlClassDeclaration.annotation(Entity::class) !== null) {
                    FetcherGenerator(environment.codeGenerator, ctx, file, sqlClassDeclaration)
                        .generate(allFiles)
                }
            }
        }

        val packageCollector = PackageCollector()
        for (file in ctx.resolver.getNewFiles()) {
            for (classDeclaration in file.declarations.filterIsInstance<KSClassDeclaration>()) {
                val qualifiedName = classDeclaration.qualifiedName!!.asString()
                if (includes !== null && !includes.any { qualifiedName.startsWith(it) }) {
                    continue
                }
                if (excludes !== null && excludes.any { qualifiedName.startsWith(it) }) {
                    continue
                }
                if (classDeclaration.annotation(Entity::class) !== null) {
                     packageCollector.accept(classDeclaration)
                }
            }
        }
        JimmerModuleGenerator(
            environment.codeGenerator,
            packageCollector.toString(),
            packageCollector.declarations
        ).generate(allFiles)
    }

    private fun generateErrorTypes(resolver: Resolver, declarations: List<KSClassDeclaration>) {
        val allFiles = resolver.getNewFiles().toList()
        for (declaration in declarations) {
            ErrorGenerator(declaration, environment.codeGenerator).generate(allFiles)
        }
    }

    private fun include(declaration: KSClassDeclaration): Boolean {
        val qualifiedName = declaration.qualifiedName!!.asString()
        if (includes !== null && !includes.any { qualifiedName.startsWith(it) }) {
            return false
        }
        if (excludes !== null && excludes.any { qualifiedName.startsWith(it) }) {
            return false
        }
        return true
    }

    private fun generateDtoTypes(
        resolver: Resolver,
        dtoTypeMap: Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>>
    ) {
        val allFiles = resolver.getAllFiles().toList()
        for (dtoTypes in dtoTypeMap.values) {
            for (dtoType in dtoTypes) {
                DtoGenerator(dtoType, dtoMutable, environment.codeGenerator).generate(allFiles)
            }
        }
    }

    private class PackageCollector {

        private var paths: MutableList<String>? = null

        private var str: String? = null

        private val _declarations: MutableList<KSClassDeclaration> = ArrayList()

        fun accept(declaration: KSClassDeclaration) {
            _declarations.add(declaration)
            if (paths != null && paths!!.isEmpty()) {
                return
            }
            str = null
            var newPaths = DOT_PATTERN.split(declaration.packageName.asString()).toMutableList()
            if (paths == null) {
                paths = newPaths
            } else {
                val len = min(paths!!.size, newPaths.size)
                var index = 0
                while (index < len) {
                    if (paths!![index] != newPaths[index]) {
                        break
                    }
                    index++
                }
                if (index < paths!!.size) {
                    paths!!.subList(index, paths!!.size).clear()
                }
            }
        }

        val declarations: List<KSClassDeclaration>
            get() = _declarations

        override fun toString(): String {
            var s = str
            if (s == null) {
                val ps = paths
                s = if (ps.isNullOrEmpty()) "" else java.lang.String.join(".", ps)
                str = s
            }
            return s!!
        }

        companion object {
            private val DOT_PATTERN = Pattern.compile("\\.")
        }
    }
}