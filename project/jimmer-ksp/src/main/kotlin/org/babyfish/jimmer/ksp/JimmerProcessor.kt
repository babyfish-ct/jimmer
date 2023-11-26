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
import org.babyfish.jimmer.ksp.dto.DtoGenerator
import org.babyfish.jimmer.ksp.dto.DtoProcessor
import org.babyfish.jimmer.ksp.error.ErrorGenerator
import org.babyfish.jimmer.ksp.error.ErrorProcessor
import org.babyfish.jimmer.ksp.immutable.ImmutableProcessor
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.sql.Entity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.math.min

class JimmerProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val includes: Array<String>? =
        environment.options["jimmer.source.includes"]
                ?.takeIf { it.isNotEmpty() }
                ?.trim()
                ?.split("\\s*,[,;]\\s*")
                ?.toTypedArray()

    private val excludes: Array<String>? =
        environment.options["jimmer.source.excludes"]
                ?.takeIf { it.isNotEmpty() }
                ?.trim()
                ?.split("\\s*[,;]\\s*")
                ?.toTypedArray()

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

    private var serverGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            val ctx = Context(resolver, environment)
            val processedDeclarations = mutableListOf<KSClassDeclaration>()
            if (!serverGenerated) {
                processedDeclarations += ImmutableProcessor(ctx).process()
                processedDeclarations += ErrorProcessor(ctx).process()
                DtoProcessor(ctx, dtoDirs, dtoMutable).process()
                serverGenerated = true
            }
            return processedDeclarations
        } catch (ex: MetaException) {
            environment.logger.error(ex.message!!, ex.declaration)
            emptyList()
        }
    }
}