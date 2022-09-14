package org.babyfish.jimmer.jackson.meta;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import org.babyfish.jimmer.jackson.PropUtils;
import org.babyfish.jimmer.meta.ImmutableProp;

class BeanProp extends BeanProperty.Std {

    private ImmutableProp prop;

    public BeanProp(
            TypeResolutionContext ctx,
            ImmutableProp prop
    ) {
        super(
                new PropertyName(prop.getName()),
                PropUtils.getJacksonType(prop),
                null,
                new BeanMember(ctx, prop),
                PropertyMetadata.STD_REQUIRED_OR_OPTIONAL
        );
    }
}
