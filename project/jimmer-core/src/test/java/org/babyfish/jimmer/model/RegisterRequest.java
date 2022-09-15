package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

@SamePassword
@Immutable
public interface RegisterRequest {

    String name();

    String password();

    String passwordAgain();

    @IdCard(message = "{i18n.idCardError}")
    String idCard();
}
