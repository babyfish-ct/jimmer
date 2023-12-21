package org.babyfish.jimmer.ksp.client

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class ClientExceptionMetadata(
    val declaration: KSClassDeclaration,
    val family: String,
    val code: String?,
    val superMetadata: ClientExceptionMetadata?
) {
    private lateinit var _subMetadatas: List<ClientExceptionMetadata>

    var subMetadatas: List<ClientExceptionMetadata>
        get() = _subMetadatas
        internal set(value) { _subMetadatas = value }
}