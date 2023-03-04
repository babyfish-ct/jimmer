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
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.ksp.generator.*
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.io.File
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
                it.trim().split("\\s*,[,;]\\s*").toTypedArray()
            }

    private val excludes: Array<String>? =
        environment.options["jimmer.source.excludes"]
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                it.trim().split("\\s*[,;]\\s*").toTypedArray()
            }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!processed.compareAndSet(false, true)) {
            return emptyList()
        }

        val ctx = Context(resolver)
        val classDeclarationMultiMap = findModelMap(ctx)
        generateJimmerTypes(resolver, ctx, classDeclarationMultiMap)

        val errorDeclarations = findErrorTypes(resolver)
        generateErrorTypes(resolver, errorDeclarations)

        return classDeclarationMultiMap.values.flatten()
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
        var step = 0
        while (true) {
            var hasNext = false
            for (declarations in modelMap.values) {
                for (declaration in declarations) {
                    hasNext = hasNext or ctx.typeOf(declaration).resolve(ctx, step)
                }
            }
            if (!hasNext) {
                break
            }
            step++
        }
        return modelMap
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
        resolver: Resolver,
        ctx: Context,
        classDeclarationMultiMap: Map<KSFile, List<KSClassDeclaration>>
    ) {
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