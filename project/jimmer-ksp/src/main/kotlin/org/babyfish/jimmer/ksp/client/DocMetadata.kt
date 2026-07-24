package org.babyfish.jimmer.ksp.client

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.babyfish.jimmer.client.Description
import org.babyfish.jimmer.client.meta.Doc
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.name

class DocMetadata(
    private val ctx: Context
) {
    private val docMap = mutableMapOf<KSDeclaration, String>()

    private val draftDocMap = mutableMapOf<KSClassDeclaration, Map<String, String>>()

    fun getDoc(declaration: KSDeclaration): Doc? =
        getString(declaration)?.let { Doc.parse(it) }

    fun getString(declaration: KSDeclaration): String? =
        getStringImpl(declaration).takeIf { it.isNotBlank() }

    private fun getStringImpl(declaration: KSDeclaration): String {

        val existing = docMap[declaration]
        if (existing !== null) {
            return existing
        }
        val docString = declaration.docString?.takeIf { it.isNotBlank() }
        if (docString != null) {
            return docString.also {
                docMap[declaration] = it
            }
        }

        val descriptionString = declaration
            .annotation(Description::class)
            ?.get(Description::value)
            ?.takeIf { it.isNotBlank() }
        if (descriptionString != null) {
            return descriptionString.also {
                docMap[declaration] = it
            }
        }

        val typeDeclaration = if (declaration is KSPropertyDeclaration) {
            declaration.parentDeclaration as KSClassDeclaration
        } else {
            declaration as? KSClassDeclaration
        }
        if (typeDeclaration !== null) {
            val key = if (declaration is KSPropertyDeclaration) declaration.name else ""
            val value = draftDocStringMap(typeDeclaration)[key]
            if (!value.isNullOrBlank()) {
                docMap[declaration] = value
                return value
            }
        }

        return docMap[declaration] ?: "".also {
            docMap[declaration] = it
        }
    }

    private fun draftDocStringMap(typeDeclaration: KSClassDeclaration): Map<String, String> =
        draftDocMap.getOrPut(typeDeclaration) {
            createDraftDocStringMap(typeDeclaration)
        }

    private fun createDraftDocStringMap(typeDeclaration: KSClassDeclaration): Map<String, String> {
        val qualifiedName = typeDeclaration.qualifiedName ?: return emptyMap()
        val draftDeclaration = ctx
            .resolver
            .getClassDeclarationByName(qualifiedName.asString() + "Draft")
            ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        draftDeclaration
            .annotation(Description::class)
            ?.get(Description::value)
            ?.takeIf { it.isNotBlank() }
            ?.let { map[""] = it }
        for (declaration in draftDeclaration.declarations) {
            if (declaration is KSPropertyDeclaration) {
                declaration
                    .annotation(Description::class)
                    ?.get(Description::value)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { map[declaration.name] = it }
            }
        }
        return map
    }
}
