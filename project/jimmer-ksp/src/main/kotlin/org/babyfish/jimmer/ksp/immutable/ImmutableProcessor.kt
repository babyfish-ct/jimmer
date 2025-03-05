package org.babyfish.jimmer.ksp.immutable

import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.immutable.generator.DraftGenerator
import org.babyfish.jimmer.ksp.immutable.generator.FetcherGenerator
import org.babyfish.jimmer.ksp.immutable.generator.JimmerModuleGenerator
import org.babyfish.jimmer.ksp.immutable.generator.PropsGenerator
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.util.regex.Pattern
import kotlin.math.min

class ImmutableProcessor(
    private val ctx: Context,
    private val isModuleRequired: Boolean
) {
    fun process(): Collection<KSClassDeclaration> {
        val modelMap = findModelMap()
        generateJimmerTypes(modelMap)
        return modelMap.values.flatten()
    }

    private fun findModelMap(): Map<KSFile, List<KSClassDeclaration>> {
        val modelMap = mutableMapOf<KSFile, MutableList<KSClassDeclaration>>()
        for (file in ctx.resolver.getNewFiles()) {
            for (classDeclaration in file.declarations.filterIsInstance<KSClassDeclaration>()) {
                if (ctx.include(classDeclaration)) {
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
                        modelMap.computeIfAbsent(file) { mutableListOf() } += classDeclaration
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

    private fun generateJimmerTypes(
        classDeclarationMultiMap: Map<KSFile, List<KSClassDeclaration>>
    ) {
        val allFiles = ctx.resolver.getAllFiles().toList()
        for ((file, classDeclarations) in classDeclarationMultiMap) {
            DraftGenerator(ctx.environment.codeGenerator, ctx, file, classDeclarations)
                .generate(allFiles)
            if (classDeclarations.size > 1) {
                throw GeneratorException(
                    "The $file declares several types decorated by " +
                        "@${Immutable::class.qualifiedName}, " +
                        "@${Entity::class.qualifiedName}, " +
                        "@${MappedSuperclass::class.qualifiedName} " +
                        "or ${Embeddable::class.qualifiedName}: " +
                        classDeclarations.joinToString { it.fullName }
                )
            }
            val sqlClassDeclarations = classDeclarations.filter {
                it.annotation(Entity::class) !== null ||
                    it.annotation(MappedSuperclass::class) !== null ||
                    it.annotation(Embeddable::class) != null
            }
            if (sqlClassDeclarations.isNotEmpty()) {
                val sqlClassDeclaration = sqlClassDeclarations[0]
                PropsGenerator(ctx.environment.codeGenerator, ctx, file, sqlClassDeclaration)
                    .generate(allFiles)
                if (sqlClassDeclaration.annotation(Entity::class) !== null || sqlClassDeclaration.annotation(Embeddable::class) !== null) {
                    FetcherGenerator(ctx.environment.codeGenerator, ctx, file, sqlClassDeclaration)
                        .generate(allFiles)
                }
            }
        }

        val packageCollector = PackageCollector()
        for (file in ctx.resolver.getNewFiles()) {
            for (classDeclaration in file.declarations.filterIsInstance<KSClassDeclaration>()) {
                if (ctx.include(classDeclaration) && classDeclaration.annotation(Entity::class) !== null) {
                    packageCollector.accept(classDeclaration)
                }
            }
        }
        if (!ctx.isBuddyIgnoreResourceGeneration) {
            JimmerModuleGenerator(
                ctx.environment.codeGenerator,
                packageCollector.toString(),
                packageCollector.declarations,
                isModuleRequired
            ).generate(allFiles)
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