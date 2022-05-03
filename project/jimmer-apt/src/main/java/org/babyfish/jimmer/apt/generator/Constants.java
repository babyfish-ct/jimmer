package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.meata.ImmutableType;
import org.babyfish.jimmer.runtime.DraftContext;

import java.util.function.Consumer;

class Constants {

    public static final ClassName DRAFT_CONTEXT_CLASS_NAME =
            ClassName.get(DraftContext.class);

    public static final ClassName DRAFT_CONSUMER_CLASS_NAME =
            ClassName.get(DraftConsumer.class);

    public static final ClassName RUNTIME_TYPE_CLASS_NAME =
            ClassName.get(ImmutableType.class);

    public static final String DRAFT_FIELD_CTX =
            "__ctx";

    public static final String DRAFT_FIELD_BASE =
            "__base";

    public static final String DRAFT_FIELD_MODIFIED =
            "__modified";

    public static final String DRAFT_FIELD_RESOLVING =
            "__resolving";

    public static final String DRAFT_FIELD_EMAIL_PATTERN =
            "__email_pattern";

    public static String regexpPatternFieldName(ImmutableProp prop, int index) {
        return "__" + prop.getName() + "_pattern" + (index == 0 ? "" : "_" + index);
    }
}
