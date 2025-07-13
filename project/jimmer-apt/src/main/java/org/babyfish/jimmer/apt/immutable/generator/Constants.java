package org.babyfish.jimmer.apt.immutable.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.*;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.client.Description;
import org.babyfish.jimmer.client.TNullable;
import org.babyfish.jimmer.impl.util.DtoPropAccessor;
import org.babyfish.jimmer.ClientException;
import org.babyfish.jimmer.internal.FixedInputField;
import org.babyfish.jimmer.internal.GeneratedBy;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.Visibility;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.impl.validation.Validator;
import org.babyfish.jimmer.sql.collection.IdViewList;
import org.babyfish.jimmer.sql.collection.ManyToManyViewList;
import org.babyfish.jimmer.sql.collection.MutableIdViewList;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Constants {

    public static final ClassName CLONEABLE_CLASS_NAME =
            ClassName.get(Cloneable.class);

    public static final ClassName SERIALIZABLE_CLASS_NAME =
            ClassName.get(Serializable.class);

    public static final ClassName CLONE_NOT_SUPPORTED_EXCEPTION_CLASS_NAME =
            ClassName.get(CloneNotSupportedException.class);

    public static final ClassName OBJECTS_CLASS_NAME =
            ClassName.get(Objects.class);

    public static final ClassName DESCRIPTION_CLASS_NAME =
            ClassName.get(Description.class);

    public static final ClassName JSON_IGNORE_CLASS_NAME =
            ClassName.get(JsonIgnore.class);

    public static final ClassName JSON_PROPERTY_ORDER_CLASS_NAME =
            ClassName.get(JsonPropertyOrder.class);

    public static final ClassName JSON_SERIALIZE_CLASS_NAME =
            ClassName.get(JsonSerialize.class);

    public static final ClassName JSON_SERIALIZER_CLASS_NAME =
            ClassName.get(JsonSerializer.class);

    public static final ClassName JSON_DESERIALIZE_CLASS_NAME =
            ClassName.get(JsonDeserialize.class);

    public static final ClassName JSON_POJO_BUILDER_CLASS_NAME =
            ClassName.get(JsonPOJOBuilder.class);

    public static final ClassName JSON_NAMING_CLASS_NAME =
            ClassName.get(JsonNaming.class);

    public static final ClassName STRING_CLASS_NAME =
            ClassName.get(String.class);

    public static final ClassName BIG_INTEGER_CLASS_NAME =
            ClassName.get(BigInteger.class);

    public static final ClassName BIG_DECIMAL_CLASS_NAME =
            ClassName.get(BigDecimal.class);

    public static final ClassName THROWABLE_CLASS_NAME =
            ClassName.get(Throwable.class);

    public static final ClassName LIST_CLASS_NAME =
            ClassName.get(List.class);

    public static final ClassName ARRAY_LIST_CLASS_NAME =
            ClassName.get(ArrayList.class);

    public static final ClassName MAP_CLASS_NAME =
            ClassName.get(Map.class);

    public static final ClassName COLLECTION_CLASS_NAME =
            ClassName.get(Collection.class);

    public static final ClassName COLLECTIONS_CLASS_NAME =
            ClassName.get(Collections.class);

    public static final ClassName FUNCTION_CLASS_NAME =
            ClassName.get(Function.class);

    public static final ClassName GENERATED_BY_CLASS_NAME =
            ClassName.get(GeneratedBy.class);

    public static final ClassName FIXED_INPUT_FIELD_CLASS_NAME =
            ClassName.get(FixedInputField.class);

    public static final ClassName VIEW_CLASS_NAME =
            ClassName.get(View.class);

    public static final ClassName EMBEDDABLE_DTO_CLASS_NAME =
            ClassName.get(EmbeddableDto.class);

    public static final ClassName INPUT_CLASS_NAME =
            ClassName.get(Input.class);

    public static final ClassName JSPECIFICATION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.query.specification",
                    "JSpecification"
            );

    public static final ClassName SPECIFICATION_ARGS_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.query.specification",
                    "SpecificationArgs"
            );

    public static final ClassName PREDICATE_APPLIER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.query.specification",
                    "PredicateApplier"
            );

    public static final ClassName LINKED_HASH_MAP_CLASS_NAME =
            ClassName.get(LinkedHashMap.class);

    public static final ClassName DRAFT_CONTEXT_CLASS_NAME =
            ClassName.get(DraftContext.class);

    public static final ClassName DRAFT_CONSUMER_CLASS_NAME =
            ClassName.get(DraftConsumer.class);

    public static final ClassName RUNTIME_TYPE_CLASS_NAME =
            ClassName.get(org.babyfish.jimmer.meta.ImmutableType.class);

    public static final ClassName VISIBILITY_CLASS_NAME =
            ClassName.get(Visibility.class);

    public static final ClassName PROP_ID_CLASS_NAME =
            ClassName.get(PropId.class);

    public static final ClassName VALIDATOR_CLASS_NAME =
            ClassName.get(Validator.class);

    public static final ClassName DTO_PROP_ACCESSOR_CLASS_NAME =
            ClassName.get(DtoPropAccessor.class);

    public static final ClassName ID_VIEW_LIST_CLASS_NAME =
            ClassName.get(IdViewList.class);

    public static final ClassName MUTABLE_ID_VIEW_LIST_CLASS_NAME =
            ClassName.get(MutableIdViewList.class);

    public static final ClassName MANY_TO_MANY_VIEW_LIST_CLASS_NAME =
            ClassName.get(ManyToManyViewList.class);

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

    public static final ClassName PREDICATE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "Predicate"
            );

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

    public static final ClassName TABLE_LIKE_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table.spi",
                    "TableLike"
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

    public static final ClassName EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "Expression"
            );

    public static final ClassName STRING_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "StringExpression"
            );

    public static final ClassName NUMERIC_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "NumericExpression"
            );

    public static final ClassName DATE_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "DateExpression"
            );

    public static final ClassName TEMPORAL_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "TemporalExpression"
            );

    public static final ClassName COMPARABLE_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "ComparableExpression"
            );

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

    public static final ClassName PROP_DATE_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Dt"
            );

    public static final ClassName PROP_TEMPORAL_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Tp"
            );

    public static final ClassName PROP_COMPARABLE_EXPRESSION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "PropExpression",
                    "Cmp"
            );

    public static final ClassName ABSTRACT_TYPED_FETCHER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher.spi",
                    "AbstractTypedFetcher"
            );

    public static final ClassName ID_ONLY_FETCH_TYPE =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "IdOnlyFetchType"
            );

    public static final ClassName REFERENCE_FETCH_TYPE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "ReferenceFetchType"
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

    public static final ClassName REFERENCE_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "ReferenceFieldConfig"
            );

    public static final ClassName LIST_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "ListFieldConfig"
            );

    public static final ClassName RECURSIVE_REFERENCE_FIELD_CONFIG_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "RecursiveReferenceFieldConfig"
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

    public static final ClassName WEAK_JOIN_HANDLE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "WeakJoinHandle"
            );

    public static final ClassName WEAK_JOIN_LAMBDA_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "WeakJoinLambda"
            );

    public static final ClassName J_WEAK_JOIN_LAMBDA_FACTORY_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "JWeakJoinLambdaFactory"
            );

    public static final ClassName BASE_TABLE_SYMBOL_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.base",
                    "BaseTableSymbol"
            );

    public static final ClassName BASE_TABLE_SYMBOLS_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.base",
                    "BaseTableSymbols"
            );

    public static final ClassName TABLE_PROXIES_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "TableProxies"
            );

    public static final ClassName TABLE_EX_PROXY_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table.spi",
                    "TableExProxy"
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

    public static final ClassName DTO_METADATA_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.fetcher",
                    "DtoMetadata"
            );

    public static final ClassName IMMUTABLE_PROP_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.meta",
                    "ImmutableProp"
            );

    public static final ClassName IMMUTABLE_OBJECTS_CLASS_NAME =
            ClassName.get(ImmutableObjects.class);

    public static final ClassName CLIENT_EXCEPTION_CLASS_NAME =
            ClassName.get(ClientException.class);

    public static final ClassName HIBERNATE_VALIDATOR_ENHANCED_BEAN =
            ClassName.get(
                    "org.hibernate.validator.engine",
                    "HibernateValidatorEnhancedBean"
            );

    public static final ClassName SELECTION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast",
                    "Selection"
            );

    public static final ClassName MAPPER_SELECTION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "MapperSelection"
            );

    public static final ClassName PROPAGATION_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.transaction",
                    "Propagation"
            );

    public static final ClassName BASE_TABLE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.table",
                    "BaseTable"
            );

    public static final ClassName BASE_TABLE_OWNER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.base",
                    "BaseTableOwner"
            );

    public static final ClassName BASE_TABLE_IMPLEMENTOR_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "BaseTableImplementor"
            );

    public static final ClassName ABSTRACT_BASE_TABLE_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.table",
                    "AbstractBaseTable"
            );

    public static final ClassName TYPED_TUPLE_MAPPER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.mapper",
                    "TypedTupleMapper"
            );

    public static final ClassName ABSTRACT_BASE_TABLE_MAPPER_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.mapper",
                    "AbstractBaseTableMapper"
            );

    public static final ClassName BASE_TABLE_QUERY_IMPLEMENTOR_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.query",
                    "BaseTableQueryImplementor"
            );

    public static final ClassName BASE_TABLE_SELECTIONS_CLASS_NAME =
            ClassName.get(
                    "org.babyfish.jimmer.sql.ast.impl.base",
                    "BaseTableSelections"
            );

    public static final String T_NULLABLE_QUALIFIED_NAME =
            TNullable.class.getName();

    public static final String DRAFT_FIELD_CTX =
            "__ctx";

    public static final String DRAFT_FIELD_BASE =
            "__base";

    public static final String DRAFT_FIELD_MODIFIED =
            "__modified";

    public static final String DRAFT_FIELD_RESOLVING =
            "__resolving";

    public static final String DRAFT_FIELD_RESOLVED =
            "__resolved";

    public static final String DRAFT_FIELD_EMAIL_PATTERN =
            "__EMAIL_PATTERN__";

    public static final String JIMMER_MODULE =
            "JimmerModule";

    public static final String FROZEN_EXCEPTION_MESSAGE =
            "The current draft has been resolved so it cannot be modified";

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
