package org.babyfish.jimmer.apt.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.DraftConsumer;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.impl.validation.Validator;

import java.util.function.Consumer;

class Constants {

    public static final ClassName CLONEABLE_CLASS_NAME =
            ClassName.get(Cloneable.class);

    public static final ClassName CLONE_NOT_SUPPORTED_EXCEPTION_CLASS_NAME =
            ClassName.get(CloneNotSupportedException.class);

    public static final ClassName JSON_IGNORE_CLASS_NAME =
            ClassName.get(JsonIgnore.class);

    public static final ClassName DRAFT_CONTEXT_CLASS_NAME =
            ClassName.get(DraftContext.class);

    public static final ClassName DRAFT_CONSUMER_CLASS_NAME =
            ClassName.get(DraftConsumer.class);

    public static final ClassName RUNTIME_TYPE_CLASS_NAME =
            ClassName.get(org.babyfish.jimmer.meta.ImmutableType.class);

    public static final ClassName VALIDATOR_CLASS_NAME =
            ClassName.get(Validator.class);

    public static final ClassName SCALAR_CLASS_NAME =
            ClassName.get(TypedProp.Scalar.class);

    public static final ClassName SCALAR_LIST_CLASS_NAME =
            ClassName.get(TypedProp.ScalarList.class);

    public static final ClassName REFERENCE_CLASS_NAME =
            ClassName.get(TypedProp.Reference.class);

    public static final ClassName REFERENCE_LIST_CLASS_NAME =
            ClassName.get(TypedProp.ReferenceList.class);

    public static final ClassName TYPED_PROP_CLASS_NAME =
            ClassName.get(TypedProp.class);

    public static final ClassName JOIN_TYPE_CLASS_NAME =
            ClassName.get(JoinType.class);

    public static final ClassName PROPS_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "Props"
            );

    public static final ClassName PROPS_FOR_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "PropsFor"
            );

    public static final ClassName TABLE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "Table"
            );

    public static final ClassName TABLE_EX_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "TableEx"
            );

    public static final ClassName TABLE_IMPLEMENTOR_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "TableImplementor"
            );

    public static final ClassName ABSTRACT_TYPED_TABLE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table.spi",
                    "AbstractTypedTable"
            );

    public static final ClassName DELAYED_OPERATION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table.spi",
                    "AbstractTypedTable",
                    "DelayedOperation"
            );

    public static final ClassName CONSUMER_CLASS_NAME =
            ClassName.get(Consumer.class);

    public static final ClassName PROP_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression"
            );

    public static final ClassName PROP_STRING_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Str"
            );

    public static final ClassName PROP_NUMERIC_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Num"
            );

    public static final ClassName PROP_COMPARABLE_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Cmp"
            );

    public static final ClassName ABSTRACT_TYPE_FETCHER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher.spi",
                    "AbstractTypedFetcher"
            );

    public static final ClassName FETCHER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "Fetcher"
            );

    public static final ClassName FETCHER_IMPL_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher.impl",
                    "FetcherImpl"
            );

    public static final ClassName FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "FieldConfig"
            );

    public static final ClassName LIST_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "ListFieldConfig"
            );

    public static final ClassName RECURSIVE_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "RecursiveFieldConfig"
            );

    public static final ClassName RECURSIVE_LIST_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "RecursiveListFieldConfig"
            );

    public static final ClassName ENTITY_MANAGER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.runtime",
                    "EntityManager"
            );

    public static final ClassName CLASS_CLASS_NAME =
            ClassName.get(Class.class);

    public static final ClassName WEAK_JOIN_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "WeakJoin"
            );

    public static final ClassName TABLE_PROXIES_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "TableProxies"
            );

    public static final ClassName ABSTRACT_TYPED_EMBEDDED_PROP_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.embedded",
                    "AbstractTypedEmbeddedPropExpression"
            );

    public static final ClassName EMBEDDED_PROP_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Embedded"
            );

    public static final String DRAFT_FIELD_CTX =
            "__ctx";

    public static final String DRAFT_FIELD_BASE =
            "__base";

    public static final String DRAFT_FIELD_MODIFIED =
            "__modified";

    public static final String DRAFT_FIELD_RESOLVING =
            "__resolving";

    public static final String DRAFT_FIELD_EMAIL_PATTERN =
            "__EMAIL_PATTERN__";

    public static final String JimmerModule =
            "JimmerModule";

    public static String regexpPatternFieldName(ImmutableProp prop, int index) {
        return "__" + Strings.upper(prop.getName()) + "_PATTER" + (index == 0 ? "" : "_" + index);
    }

    public static String validatorFieldName(ClassName annotationClassName) {
        return "__" +
                Strings.upper(annotationClassName.simpleName()) +
                "_VALIDATOR_" +
                Math.abs(annotationClassName.hashCode());
    }

    public static String validatorFieldName(ImmutableProp prop, ClassName annotationClassName) {
        return "__" +
                Strings.upper(prop.getName()) +
                "_" +
                Strings.upper(annotationClassName.simpleName()) +
                "_VALIDATOR_" +
                Math.abs(annotationClassName.hashCode());
    }
}
