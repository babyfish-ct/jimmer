package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable

@SamePassword
@Immutable
interface RegisterRequest {

    val name: String

    val password: String

    val passwordAgain: String

    @IdCard(message = "{i18n.idCardError}")
    val idCard: String
}