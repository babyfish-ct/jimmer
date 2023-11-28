package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import org.babyfish.jimmer.dto.compiler.DtoUtils
import org.babyfish.jimmer.ksp.client.ClientProcessor
import org.babyfish.jimmer.ksp.dto.DtoProcessor
import org.babyfish.jimmer.ksp.error.ErrorProcessor
import org.babyfish.jimmer.ksp.immutable.ImmutableProcessor

class JimmerProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

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

    private var clientGenerated = false

    private var delayedFiles: Collection<KSFile>? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            val ctx = Context(resolver, environment)
            val processedDeclarations = mutableListOf<KSClassDeclaration>()
            if (!serverGenerated) {
                processedDeclarations += ImmutableProcessor(ctx).process()
                processedDeclarations += ErrorProcessor(ctx).process()
                val dtoGenerated = DtoProcessor(ctx, dtoDirs, dtoMutable).process()
                serverGenerated = true
                if (dtoGenerated) {
                    delayedFiles = resolver.getNewFiles().toList()
                    return processedDeclarations
                }
            }
            if (!clientGenerated) {
                ClientProcessor(ctx, delayedFiles).process()
                clientGenerated = true
                delayedFiles = null
            }
            return processedDeclarations
        } catch (ex: MetaException) {
            environment.logger.error(ex.message!!, ex.declaration)
            emptyList()
        }
    }
}