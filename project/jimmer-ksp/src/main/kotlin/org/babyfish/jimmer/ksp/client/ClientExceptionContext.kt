package org.babyfish.jimmer.ksp.client

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import org.babyfish.jimmer.error.CodeBasedException
import org.babyfish.jimmer.error.CodeBasedRuntimeException
import org.babyfish.jimmer.internal.ClientException
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.client.ClientProcessor.Companion.realDeclaration

class ClientExceptionContext {

    private val metadataMap = mutableMapOf<KSClassDeclaration, ClientExceptionMetadata>()

    private val nonAbstractDeclarationMap = mutableMapOf<Key, KSDeclaration>()

    operator fun get(declaration: KSClassDeclaration): ClientExceptionMetadata =
        metadataMap[declaration] ?:
            create(declaration).also {
                metadataMap[declaration] = it
                try {
                    initSubMetadatas(it)
                } catch (ex: Throwable) {
                    metadataMap.remove(declaration)
                    throw ex
                }
            }

    private fun create(declaration: KSClassDeclaration): ClientExceptionMetadata {
        val annotation = declaration.annotation(ClientException::class)
            ?: throw MetaException(
                declaration,
                "the exception type extends \"" +
                    CodeBasedException::class.qualifiedName +
                    "\" or \"" +
                    CodeBasedRuntimeException::class.qualifiedName +
                    "\" must be decorated by \"@" +
                    ClientException::class.qualifiedName +
                    "\""
            )
        val code = annotation[ClientException::code]?.takeIf { it.isNotEmpty() }
        val subTypes = annotation.getClassListArgument(ClientException::subTypes)
        if (code === null && subTypes.isEmpty()) {
            throw MetaException(
                declaration,
                "it is decorated by @\"" +
                    ClientException::class.java.getName() +
                    "\" but neither \"code\" nor \"subTypes\" of the annotation is specified"
            )
        }
        if (code !== null && subTypes.isNotEmpty()) {
            throw MetaException(
                declaration,
                ("it is decorated by @\"" +
                    ClientException::class.java.getName() +
                    "\" but both \"code\" and \"subTypes\" of the annotation are specified")
            )
        }
        if (code !== null && declaration.isAbstract()) {
            throw MetaException(
                declaration,
                "it is decorated by @\"" +
                    ClientException::class.java.getName() +
                    "\" and the \"code\" of the annotation is specified so that " +
                    "it cannot be abstract"
            )
        }
        if (subTypes.isNotEmpty() && !declaration.isAbstract()) {
            throw MetaException(
                declaration,
                ("it is decorated by @\"" +
                    ClientException::class.java.getName() +
                    "\" and the \"subTypes\" of the annotation is specified so that " +
                    "it must be abstract")
            )
        }
        val superDeclaration = declaration
            .superTypes
            .first { (it.realDeclaration as KSClassDeclaration).classKind == ClassKind.CLASS }
            .realDeclaration as KSClassDeclaration
        var superMetadata: ClientExceptionMetadata? = null
        if (superDeclaration.qualifiedName!!.asString() != CodeBasedException::class.qualifiedName &&
            superDeclaration.qualifiedName!!.asString() != CodeBasedRuntimeException::class.qualifiedName) {
            val superAnnotation = superDeclaration.annotation(ClientException::class)
            if (superAnnotation !== null) {
                if (!superAnnotation.getClassListArgument(ClientException::subTypes).any { it == declaration }) {
                    throw MetaException(
                        declaration,
                        "its super type \"" +
                            superDeclaration.qualifiedName!!.asString() +
                            "\" is decorated by " +
                            ClientException::class.java.getName() +
                            "\" but the \"subTypes\" of the annotation does not contain current type"
                    )
                }
                superMetadata = get(superDeclaration)
            }
        }
        val family: String = annotation[ClientException::family]?.takeIf { it.isNotEmpty() }
            ?: superMetadata?.family
            ?: "DEFAULT"
        if (superMetadata != null && superMetadata.family != family) {
            throw MetaException(
                declaration,
                "Its family is \"" +
                    family +
                    "\" but the family of super exception is \"" +
                    superMetadata.family +
                    "\""
            )
        }
        code?.let {
            nonAbstractDeclarationMap.put(Key(family, it), declaration)?.let { conflictDeclaration ->
                throw MetaException(
                    declaration,
                    "Duplicated error family \"" +
                        family +
                        "\" and code \"" +
                        code +
                        "\", it is used by another exception type \"" +
                        conflictDeclaration.qualifiedName!!.asString() +
                        "\""
                )
            }
        }
        return ClientExceptionMetadata(
            declaration,
            family,
            code,
            superMetadata
        )
    }

    private fun initSubMetadatas(metadata: ClientExceptionMetadata) {
        val annotation = metadata.declaration.annotation(ClientException::class)!!
        val subTypes = annotation.getClassListArgument(ClientException::subTypes)
        for (subType in subTypes) {
            val backRefDeclaration = subType
                .superTypes
                .first { (it.realDeclaration as KSClassDeclaration).classKind == ClassKind.CLASS }
                .realDeclaration as KSClassDeclaration
            if (backRefDeclaration != metadata.declaration) {
                throw MetaException(
                    metadata.declaration,
                    "it is decorated by \"@${ClientException::class.qualifiedName}\" " +
                        "which specifies the sub type \"${subType.fullName}\", " +
                        "but the super type of that sub type is not current type"
                )
            }
            if (subType.annotation(ClientException::class) == null) {
                throw MetaException(
                    metadata.declaration,
                    "it is decorated by \"@${ClientException::class.qualifiedName}\" " +
                        "which specifies the sub type \"${subType.fullName}\", " +
                        "but that sub type is not decorated by \"@${ClientException::class.qualifiedName}\""
                )
            }
        }
        metadata.subMetadatas = subTypes.map { get(it) }.distinct().toList()
    }

    private data class Key(
        val family: String,
        val code: String
    )
}