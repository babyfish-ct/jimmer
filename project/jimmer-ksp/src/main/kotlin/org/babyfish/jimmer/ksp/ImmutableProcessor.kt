package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import org.babyfish.jimmer.ksp.generator.*
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.ksp.meta.MetaException
import org.babyfish.jimmer.meta.impl.dto.ast.DtoAstException
import org.babyfish.jimmer.meta.impl.dto.ast.DtoType
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.math.min

class ImmutableProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val processed = AtomicBoolean()

    private val includes: Array<String>? =
        environment.options["jimmer.source.includes"]
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.trim().split("\\s*,\\s*").toTypedArray()
            }

    private val excludes: Array<String>? =
        environment.options["jimmer.source.excludes"]
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.trim().split("\\s*,\\s*").toTypedArray()
            }

    private val dtoDirs: Set<String> =
        environment.options["jimmer.dtoDirs"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { text ->
                text.split("\\s*[,:;]\\s*")
                    .map {
                        when {
                            it.startsWith("/") -> it.substring(1)
                            it.endsWith("/") -> it.substring(0, it.length - 1)
                            else -> it
                        }
                    }
                    .toSet()
            }
            ?: setOf("src/main/dto")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!processed.compareAndSet(false, true)) {
            return emptyList()
        }

        val ctx = Context(resolver)
        val classDeclarationMultiMap = findModelMap(ctx)
        val dtoTypeMap = findDtoTypeMap(ctx, classDeclarationMultiMap)
        generateJimmerTypes(resolver, ctx, classDeclarationMultiMap)
        generateDtoTypes(resolver, dtoTypeMap)
        return classDeclarationMultiMap.values.flatten()
    }

    private fun findModelMap(ctx: Context): Map<KSFile, List<KSClassDeclaration>> {
        val modelMap = mutableMapOf<KSFile, MutableList<KSClassDeclaration>>()
        for (file in ctx.resolver.getAllFiles()) {
            for (classDeclaration in file.declarations.filterIsInstance<KSClassDeclaration>()) {
                val qualifiedName = classDeclaration.qualifiedName!!.asString()
                if (includes !== null && !includes.any { qualifiedName.startsWith(it) }) {
                    continue
                }
                if (excludes !== null && excludes.any { qualifiedName.startsWith(it) }) {
                    continue
                }
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
        return modelMap
    }

    private fun findDtoTypeMap(
        ctx: Context,
        classDeclarationMultiMap: Map<KSFile, List<KSClassDeclaration>>
    ): Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>> {
        if (classDeclarationMultiMap.isEmpty()) {
            return emptyMap()
        }
        var file: File? = File(classDeclarationMultiMap.keys.iterator().next().filePath).parentFile
        val actualDtoDirs = mutableListOf<String>()
        while (file != null) {
            collectActualDtoDir(file, actualDtoDirs)
            file = file.parentFile
        }

        val dtoMap = mutableMapOf<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>>()
        for (classDeclarations in classDeclarationMultiMap.values) {
            for (classDeclaration in classDeclarations) {
                val immutableType = ctx.typeOf(classDeclaration)
                for (actualDtoDir: String in actualDtoDirs) {
                    val dtoFile = File(
                        "$actualDtoDir/${
                            immutableType.qualifiedName.replace('.', '/')
                        }.dto"
                    )
                    if (dtoFile.exists()) {
                        dtoMap[immutableType] = try {
                            FileInputStream(dtoFile).use {
                                KspDtoCompiler(immutableType).compile(it)
                            }
                        } catch (ex: DtoAstException) {
                            throw MetaException(
                                "Failed to parse \"${dtoFile.absolutePath}\": ${ex.message}",
                                ex
                            )
                        } catch (ex: IOException) {
                            throw MetaException(
                                "Failed to parse \"${dtoFile.absolutePath}\": ${ex.message}",
                                ex
                            )
                        }
                    }
                }
            }
        }
        for ((type, dtoTypes) in dtoMap) {
            for (dtoType in dtoTypes) {
                for ((otherType, otherDtoTypes) in dtoMap) {
                    for (otherDtoType in otherDtoTypes) {
                        if (type != otherType && dtoType.name == otherDtoType.name) {
                            throw MetaException(
                                "Conflict dto type name, the \"" +
                                    type.qualifiedName +
                                    "\" and \"" +
                                    otherType.qualifiedName +
                                    "\" are belong to same package, " +
                                    "but they have define a dto type named \"" +
                                    dtoType.name +
                                    "\""
                            )
                        }
                    }
                }
            }
        }
        return dtoMap
    }

    private fun generateJimmerTypes(
        resolver: Resolver,
        ctx: Context,
        classDeclarationMultiMap: Map<KSFile, List<KSClassDeclaration>>
    ) {
        val packageCollector = PackageCollector()
        val allFiles = resolver.getAllFiles().toList()
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
                    packageCollector.accept(sqlClassDeclaration)
                }
            }
        }
        JimmerModuleGenerator(
            environment.codeGenerator,
            packageCollector.toString(),
            packageCollector.declarations
        ).generate(allFiles)
    }

    private fun generateDtoTypes(
        resolver: Resolver,
        dtoTypeMap: Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>>
    ) {
        val allFiles = resolver.getAllFiles().toList()
        for (dtoTypes in dtoTypeMap.values) {
            for (dtoType in dtoTypes) {
                DtoGenerator(dtoType, environment.codeGenerator).generate(allFiles)
            }
        }
    }

    private fun collectActualDtoDir(baseFile: File, outputFiles: MutableList<String>) {
        for (dtoDir in dtoDirs) {
            var subFile: File? = baseFile
            for (part in dtoDir.split("/").toTypedArray()) {
                subFile = File(subFile, part)
                if (!subFile.isDirectory) {
                    subFile = null
                    break
                }
            }
            if (subFile != null) {
                var path = subFile.absolutePath
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length - 1)
                }
                outputFiles.add(path)
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
                s = if (ps == null || ps.isEmpty()) "" else java.lang.String.join(".", ps)
                str = s
            }
            return s!!
        }

        companion object {
            private val DOT_PATTERN = Pattern.compile("\\.")
        }
    }
}