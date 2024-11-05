package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

@Immutable
public interface User {

    String loginName();

    String email();

    byte[] password();

    String area();

    String nickName();
}
