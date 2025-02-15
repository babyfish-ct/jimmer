package org.babyfish.jimmer.ksp.client

import com.google.devtools.ksp.*
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
            val map = implDocStringMap(typeDeclaration)
            if (map.isNotEmpty()) {
                map[""]?.let {
                    docMap[typeDeclaration] = it
                }
                for (propDeclaration in typeDeclaration.getAllProperties()) {
                    map[propDeclaration.name]?.let {
                        docMap[propDeclaration] = it
                    }
                }
            }
        }

        return docMap[declaration] ?: "".also {
            docMap[declaration] = it
        }
    }

    private fun implDocStringMap(typeDeclaration: KSClassDeclaration): Map<String, String> {
        val qualifiedName = typeDeclaration.qualifiedName ?: return emptyMap()
        val draftDeclaration = ctx
            .resolver
            .getClassDeclarationByName(qualifiedName.asString() + "Draft")
            ?: return emptyMap()
        val producerDeclaration = draftDeclaration
            .declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { "$" == it.simpleName.asString() }
            ?: return emptyMap()
        val implDeclaration = producerDeclaration
            .declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { "Impl" == it.simpleName.asString() }
            ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        map[""] = implDeclaration
            .annotation(Description::class)
            ?.get(Description::value)
            ?.takeIf { it.isNotBlank() }
            ?: ""
        for (declaration in implDeclaration.declarations) {
            if (declaration is KSPropertyDeclaration && declaration.isPublic() && !declaration.isInternal()) {
                map[declaration.name] = declaration
                    .annotation(Description::class)
                    ?.get(Description::value)
                    ?.takeIf { it.isNotBlank() }
                    ?: ""
            }
        }
        return map
    }
}