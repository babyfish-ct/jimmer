package org.babyfish.jimmer.spring.meta;

import org.babyfish.jimmer.sql.meta.MetaStringResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.StringValueResolver;

public class SpringMetaStringResolver implements MetaStringResolver {

    private final StringValueResolver stringValueResolver;

    public SpringMetaStringResolver(StringValueResolver stringValueResolver) {
        this.stringValueResolver = stringValueResolver;
    }

    @Override
    @Nullable
    public String resolve(@NotNull String value) {
        return stringValueResolver.resolveStringValue(value);
    }
}
