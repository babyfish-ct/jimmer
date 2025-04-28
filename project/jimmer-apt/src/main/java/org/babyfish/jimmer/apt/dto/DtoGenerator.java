package org.babyfish.jimmer.apt.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.client.DocMetadata;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.apt.util.ConverterMetadata;
import org.babyfish.jimmer.apt.util.GenericParser;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.dto.compiler.*;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.*;

import static org.babyfish.jimmer.apt.immutable.generator.Constants.*;
import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class DtoGenerator {

    private static final String[] EMPTY_STR_ARR = new String[0];

    private static final String JSON_DESERIALIZE_TYPE_NAME = JsonDeserialize.class.getName();

    private static final String KOTLIN_DTO_TYPE_NAME = "org.babyfish.jimmer.kt.dto.KotlinDto";

    private final Context ctx;

    private final DocMetadata docMetadata;

    final DtoType<ImmutableType, ImmutableProp> dtoType;

    private final Document document;

    private final DtoGenerator parent;

    private final DtoGenerator root;

    private final String innerClassName;

    private final Set<String> interfaceMethodNames;

    private TypeSpec.Builder typeBuilder;

    public DtoGenerator(
            Context ctx,
            DocMetadata docMetadata,
            DtoType<ImmutableType, ImmutableProp> dtoType
    ) {
        this(ctx, docMetadata, dtoType, null, null);
    }

    private DtoGenerator(
            Context ctx,
            DocMetadata docMetadata,
            DtoType<ImmutableType, ImmutableProp> dtoType,
            DtoGenerator parent,
            String innerClassName
    ) {
        if ((parent == null) != (innerClassName == null)) {
            throw new IllegalArgumentException("The nullity values of `parent` and `innerClassName` must be same");
        }
        this.ctx = ctx;
        this.docMetadata = docMetadata;
        this.dtoType = dtoType;
        this.document = new Document(ctx, dtoType);
        this.parent = parent;
        this.root = parent != null ? parent.root : this;
        this.innerClassName = innerClassName;
        this.interfaceMethodNames = DtoInterfaces.abstractMethodNames(ctx, dtoType);
    }

    public void generate() {
        String simpleName = getSimpleName();
        typeBuilder = TypeSpec
                .classBuilder(simpleName)
                .addModifiers(Modifier.PUBLIC);
        if (isImpl() && dtoType.getBaseType().isEntity()) {
            typeBuilder.addSuperinterface(
                    dtoType.getModifiers().contains(DtoModifier.SPECIFICATION) ?
                            ParameterizedTypeName.get(
                                    org.babyfish.jimmer.apt.immutable.generator.Constants.JSPECIFICATION_CLASS_NAME,
                                    dtoType.getBaseType().getClassName(),
                                    dtoType.getBaseType().getTableClassName()
                            ) :
                            ParameterizedTypeName.get(
                                    dtoType.getModifiers().contains(DtoModifier.INPUT) ?
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.INPUT_CLASS_NAME :
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.VIEW_CLASS_NAME,
                                    dtoType.getBaseType().getClassName()
                            )
            );
        }
        if (isImpl() && dtoType.getBaseType().isEmbeddable()) {
            typeBuilder.addSuperinterface(
                    ParameterizedTypeName.get(
                            org.babyfish.jimmer.apt.immutable.generator.Constants.EMBEDDABLE_DTO_CLASS_NAME,
                            dtoType.getBaseType().getClassName()
                    )
            );
        }
        for (TypeRef typeRef : dtoType.getSuperInterfaces()) {
            typeBuilder.addSuperinterface(getTypeName(typeRef));
        }
        if (isHibernateValidatorEnhancementRequired()) {
            typeBuilder.addSuperinterface(
                    org.babyfish.jimmer.apt.immutable.generator.Constants.HIBERNATE_VALIDATOR_ENHANCED_BEAN
            );
        }
        if (parent == null) {
            typeBuilder.addAnnotation(generatedAnnotation(dtoType.getDtoFile()));
        } else {
            typeBuilder.addAnnotation(generatedAnnotation());
        }
        if (isSerializerRequired()) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.JSON_SERIALIZE_CLASS_NAME)
                            .addMember(
                                    "using",
                                    "$T.class",
                                    getDtoClassName("Serializer")
                            )
                            .build()
            );
        }
        if (isBuildRequired()) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.JSON_DESERIALIZE_CLASS_NAME)
                            .addMember(
                                    "builder",
                                    "$T.class",
                                    getDtoClassName("Builder")
                            )
                            .build()
            );
        }
        String doc = document.get();
        if (doc == null) {
            doc = baseDocComment();
        }
        if (doc != null && !doc.isEmpty()) {
            typeBuilder.addAnnotation(
                    AnnotationSpec
                            .builder(org.babyfish.jimmer.apt.immutable.generator.Constants.DESCRIPTION_CLASS_NAME)
                            .addMember("value", "$S", doc)
                            .build()
            );
        }
        for (AnnotationMirror annotationMirror : dtoType.getBaseType().getTypeElement().getAnnotationMirrors()) {
            if (isCopyableAnnotation(annotationMirror, dtoType.getAnnotations(), null)) {
                typeBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        for (Anno anno : dtoType.getAnnotations()) {
            if (!anno.getQualifiedName().equals(KOTLIN_DTO_TYPE_NAME)) {
                typeBuilder.addAnnotation(annotationOf(anno));
            }
        }
        if (innerClassName != null) {
            typeBuilder.addModifiers(Modifier.STATIC);
            addMembers();
        } else {
            addMembers();
        }
        if (innerClassName != null) {
            assert  parent != null;
            parent.typeBuilder.addType(typeBuilder.build());
        } else {
            try {
                JavaFile
                        .builder(
                                root.dtoType.getPackageName(),
                                typeBuilder.build()
                        )
                        .indent("    ")
                        .build()
                        .writeTo(ctx.getFiler());
            } catch (IOException ex) {
                throw new GeneratorException(
                        String.format(
                                "Cannot generate dto type '%s' for '%s'",
                                dtoType.getName(),
                                dtoType.getBaseType().getQualifiedName()
                        ),
                        ex
                );
            }
        }
    }

    public String getSimpleName() {
        return innerClassName != null ? innerClassName : dtoType.getName();
    }

    private ClassName getDtoClassName() {
        return getDtoClassName(null);
    }

    ClassName getDtoClassName(String nestedClassName) {
        if (innerClassName != null) {
            List<String> list = new ArrayList<>();
            collectNames(list);
            List<String> simpleNames = list.subList(1, list.size());
            if (nestedClassName != null) {
                simpleNames = new ArrayList<>(simpleNames);
                simpleNames.add(nestedClassName);
            }
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    list.get(0),
                    simpleNames.toArray(EMPTY_STR_ARR)
            );
        }
        if (nestedClassName == null) {
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    dtoType.getName()
            );
        }
        return ClassName.get(
                root.dtoType.getPackageName(),
                dtoType.getName(),
                nestedClassName
        );
    }

    private void addMembers() {

        boolean isSpecification = dtoType.getModifiers().contains(DtoModifier.SPECIFICATION);
        if (!isSpecification) {
            addMetadata();
        }

        if (!dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
                addAccessorField(prop);
            }
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addField(prop);
            addStateField(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addField(prop);
        }

        addDefaultConstructor();
        if (!isSpecification) {
            addConverterConstructor();
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addAccessors(prop);
        }
        for (UserProp prop : dtoType.getUserProps()) {
            addAccessors(prop);
        }

        if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            addEntityType();
            addApplyTo();
        } else {
            addToEntity(false);
            addToEntity(true);
        }

        addHashCode();
        addEquals();
        addToString();

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            addSpecificationConverter(prop);
        }

        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
            if (targetType == null) {
                continue;
            }
            if (!prop.isRecursive() || targetType.isFocusedRecursion()) {
                new DtoGenerator(
                        ctx,
                        docMetadata,
                        targetType,
                        this,
                        targetSimpleName(prop)
                ).generate();
            }
        }

        if (isSerializerRequired()) {
            new SerializerGenerator(this).generate();
        }
        if (isBuildRequired()) {
            new InputBuilderGenerator(this).generate();
        }

        if (isHibernateValidatorEnhancementRequired()) {
            addHibernateValidatorEnhancement(false);
            addHibernateValidatorEnhancement(true);
        }
    }

    private void addMetadata() {
        FieldSpec.Builder builder = FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_METADATA_CLASS_NAME,
                                dtoType.getBaseType().getClassName(),
                                getDtoClassName()
                        ),
                        "METADATA"
                )
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        CodeBlock.Builder cb = CodeBlock
                .builder()
                .indent()
                .add("\n")
                .add(
                        "new $T<$T, $T>(\n",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_METADATA_CLASS_NAME,
                        dtoType.getBaseType().getClassName(),
                        getDtoClassName()
                )
                .indent()
                .add("$T.$L", dtoType.getBaseType().getFetcherClassName(), "$")
                .indent();
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (prop.getNextProp() == null) {
                addFetcherField(prop, cb);
            }
        }
        for (DtoProp<ImmutableType, ImmutableProp> hiddenProp : dtoType.getHiddenFlatProps()) {
            if (!hiddenProp.getBaseProp().isId()) {
                addHiddenFetcherField(hiddenProp, cb);
            }
        }
        cb
                .add(",\n")
                .unindent()
                .add("$T::new\n", getDtoClassName())
                .unindent()
                .unindent()
                .add(")");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private void addFetcherField(DtoProp<ImmutableType, ImmutableProp> prop, CodeBlock.Builder cb) {
        if (prop.getBaseProp().getAnnotation(Id.class) == null) {
            PropConfig<ImmutableProp> config = prop.getConfig();
            if (prop.getTargetType() != null) {
                if (prop.isRecursive()) {
                    cb.add("\n.$N(", StringUtil.identifier("recursive", prop.getBaseProp().getName()));
                } else {
                    cb.add("\n.$N(", prop.getBaseProp().getName());
                }
                if (config != null) {
                    cb.add("\n$>");
                }
                if (!prop.isRecursive()) {
                    cb.add("$T.METADATA.getFetcher()", getPropElementName(prop));
                    if (config != null) {
                        cb.add(", \n");
                    }
                }
            } else {
                cb.add("\n.$N(", prop.getBaseProp().getName());
            }
            if (config != null) {
                addConfigLambda(cb, prop);
                cb.add("$<\n");
            }
            cb.add(")");
        }
    }

    private void addConfigLambda(
            CodeBlock.Builder cb,
            DtoProp<ImmutableType, ImmutableProp> prop
    ) {
        PropConfig<ImmutableProp> cfg = prop.getConfig();
        assert cfg != null;
        cb.add("cfg -> cfg$>");
        if (cfg.getPredicate() != null || !cfg.getOrderItems().isEmpty()) {
            cb.add("\n.filter(it -> it$>\n");
            List<PropConfig.Predicate> realPredicates;
            if (cfg.getPredicate() instanceof PropConfig.Predicate.And) {
                realPredicates = ((PropConfig.Predicate.And)cfg.getPredicate()).getPredicates();
            } else if (cfg.getPredicate() != null) {
                realPredicates = Collections.singletonList(cfg.getPredicate());
            } else {
                realPredicates = Collections.emptyList();
            }
            for (PropConfig.Predicate realPredicate : realPredicates) {
                cb.add(".where(\n$>");
                addConfigPredicate(cb, realPredicate);
                cb.add("$<\n)");
            }
            if (!cfg.getOrderItems().isEmpty()) {
                cb.add(".orderBy(\n$>");
                boolean addComma = false;
                for (PropConfig.OrderItem<ImmutableProp> orderItem : cfg.getOrderItems()) {
                    if (addComma) {
                        cb.add(",\n");
                    } else {
                        addComma = true;
                    }
                    addPropPath(cb, orderItem.getPath());
                    cb.add(orderItem.isDesc() ? ".desc()" : ".asc()");
                }
                cb.add("$<\n)");
            }
            cb.add("$<\n)");
        }
        if (!cfg.getFetchType().equals("AUTO")) {
            cb.add("\n.fetchType($T.$L)", REFERENCE_FETCH_TYPE_CLASS_NAME, cfg.getFetchType());
        }
        if (cfg.getFilterClassName() != null) {
            String filterClassName = cfg.getFilterClassName();
            TypeElement filterTypeElement = ctx.getElements().getTypeElement(filterClassName);
            if (filterTypeElement == null) {
                throw new DtoException(
                        "There is no filter class: " + filterClassName
                );
            }
            new GenericParser(
                    "filter",
                    filterTypeElement,
                    "org.babyfish.jimmer.sql.fetcher.FieldFilter"
            ).parse();
            cb.add("\n.filter(new $T())", filterTypeElement);
        }
        if (cfg.getRecursionClassName() != null) {
            String recursionClassName = cfg.getRecursionClassName();
            TypeElement recursionTypeElement = ctx.getElements().getTypeElement(recursionClassName);
            if (recursionTypeElement == null) {
                throw new DtoException(
                        "There is no recursion class: " + recursionClassName
                );
            }
            TypeName entityTypeName = new GenericParser(
                    "recursion",
                    recursionTypeElement,
                    "org.babyfish.jimmer.sql.fetcher.RecursionStrategy"
            ).parse().argumentTypeNames.get(0);
            TypeName associatedEntityTypeName = prop.getTargetType().getBaseType().getClassName();
            if (!associatedEntityTypeName.equals(entityTypeName)) {
                throw new DtoException(
                        "The recursion class \"" +
                                recursionClassName +
                                "\" is illegal, it specify the generic type argument of \"" +
                                "org.babyfish.jimmer.sql.fetcher.RecursionStrategy" +
                                "\" as \"" +
                                entityTypeName +
                                "\", which is not associated entity type \"" +
                                associatedEntityTypeName +
                                "\""
                );
            }
            cb.add("\n.recursive(new $T())", recursionTypeElement);
        }
        if (cfg.getLimit() != Integer.MAX_VALUE) {
            if (cfg.getOffset() != 0) {
                cb.add("\n.limit($L, $L)", cfg.getLimit(), cfg.getOffset());
            } else {
                cb.add("\n.limit($L)", cfg.getLimit());
            }
        }
        if (cfg.getBatch() != 0) {
            cb.add("\n.batch($L)", cfg.getBatch());
        }
        if (cfg.getDepth() != Integer.MAX_VALUE) {
            cb.add("\n.depth($L)", cfg.getDepth());
        }
        cb.add("$<");
    }

    @SuppressWarnings("unchecked")
    private void addConfigPredicate(
            CodeBlock.Builder cb,
            PropConfig.Predicate predicate
    ) {
        if (predicate instanceof PropConfig.Predicate.And) {
            cb.add("$T.and(\n$>", PREDICATE_CLASS_NAME);
            boolean addComma = false;
            for (PropConfig.Predicate subPredicate : ((PropConfig.Predicate.And)predicate).getPredicates()) {
                if (addComma) {
                    cb.add(",\n");
                } else {
                    addComma = true;
                }
                addConfigPredicate(cb, subPredicate);
            }
            cb.add("$<\n)");
        } else if (predicate instanceof PropConfig.Predicate.Or) {
            cb.add("$T.or(\n$>", PREDICATE_CLASS_NAME);
            boolean addComma = false;
            for (PropConfig.Predicate subPredicate : ((PropConfig.Predicate.Or)predicate).getPredicates()) {
                if (addComma) {
                    cb.add(",\n");
                } else {
                    addComma = true;
                }
                addConfigPredicate(cb, subPredicate);
            }
            cb.add("$<\n)");
        } else if (predicate instanceof PropConfig.Predicate.Cmp) {
            PropConfig.Predicate.Cmp<ImmutableProp> cmp =
                    (PropConfig.Predicate.Cmp<ImmutableProp>) predicate;
            addPropPath(cb, cmp.getPath());
            switch (cmp.getOperator()) {
                case "=":
                    cb.add(".eq(");
                    break;
                case "<>":
                    cb.add(".ne(");
                    break;
                case "<":
                    cb.add(".lt(");
                    break;
                case "<=":
                    cb.add(".le(");
                    break;
                case ">":
                    cb.add(".gt(");
                    break;
                case ">=":
                    cb.add(".ge(");
                    break;
                case "like":
                    cb.add(".like(");
                    break;
                case "ilike":
                    cb.add(".ilike(");
                    break;
                default:
                    throw new DtoException("Illegal operator: " + cmp.getOperator());
            }
            if (cmp.getValue() instanceof String) {
                cb.add("$S)", cmp.getValue());
            } else {
                String value = cmp.getValue().toString();
                TypeName typeName = cmp.getPath().get(cmp.getPath().size() - 1).getProp().getTypeName();
                if (typeName.isBoxedPrimitive()) {
                    typeName = typeName.unbox();
                }
                if (typeName.equals(TypeName.LONG)) {
                    cb.add("$LL", value);
                } else if (typeName.equals(TypeName.FLOAT)) {
                    cb.add("$LF", value);
                } else if (typeName.equals(TypeName.DOUBLE)) {
                    cb.add("$LD", value);
                } else if (typeName.equals(BIG_INTEGER_CLASS_NAME)) {
                    cb.add(
                            "new $T($S)",
                            BIG_INTEGER_CLASS_NAME,
                            cmp.getValue().toString()
                    );
                } else if (typeName.equals(BIG_DECIMAL_CLASS_NAME)) {
                    cb.add(
                            "new $T($S)",
                            BIG_DECIMAL_CLASS_NAME,
                            cmp.getValue().toString()
                    );
                } else {
                    cb.add("$L", value);
                }
                cb.add(")");
            }
        } else if (predicate instanceof PropConfig.Predicate.Nullity) {
            PropConfig.Predicate.Nullity<ImmutableProp> nullity =
                    (PropConfig.Predicate.Nullity<ImmutableProp>) predicate;
            addPropPath(cb, nullity.getPath());
            cb.add(nullity.isNegative() ? ".isNotNull()" : ".isNull()");
        } else {
            throw new DtoException("Illegal predicate: " + predicate.getClass().getName());
        }
    }

    private void addPropPath(CodeBlock.Builder cb, List<PropConfig.PathNode<ImmutableProp>> pathNodes) {
        cb.add("it.getTable()");
        for (PropConfig.PathNode<ImmutableProp> pathNode : pathNodes) {
            if (pathNode.isAssociatedId()) {
                cb.add(".$LId()", pathNode.getProp().getName());
            } else {
                cb.add(".$L()", pathNode.getProp().getName());
            }
        }
    }

    private void addAccessorField(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (isSimpleProp(prop)) {
            return;
        }
        FieldSpec.Builder builder = FieldSpec.builder(
                org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        );
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.add("new $T(", org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME);
        cb.indent();

        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        if (prop.isNullable() && (
                !prop.toTailProp().getBaseProp().isNullable() ||
                        dtoType.getModifiers().contains(DtoModifier.SPECIFICATION) ||
                        dtoType.getModifiers().contains(DtoModifier.FUZZY) ||
                        prop.getInputModifier() == DtoModifier.FUZZY)
        ) {
            cb.add("\nfalse");
        } else {
            cb.add("\ntrue");
        }

        if (prop.getNextProp() == null) {
            cb.add(",\nnew int[] { $T.$L }", dtoType.getBaseType().getProducerClassName(), prop.getBaseProp().getSlotName());
        } else {
            cb.add(",\nnew int[] {");
            cb.indent();
            boolean addComma = false;
            for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                if (addComma) {
                    cb.add(",");
                } else {
                    addComma = true;
                }
                cb.add("\n$T.$L", p.getBaseProp().getDeclaringType().getProducerClassName(), p.getBaseProp().getSlotName());
            }
            cb.unindent();
            cb.add("\n}");
        }

        if (prop.isIdOnly()) {
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.$L($T.class, ",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().isList() ? "idListGetter" : "idReferenceGetter",
                        tailProp.getBaseProp().getTargetType().getClassName()
                );
                addConverterLoading(cb, prop, false);
                cb.add(")");

                cb.add(
                        ",\n$T.$L($T.class, ",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().isList() ? "idListSetter" : "idReferenceSetter",
                        tailProp.getBaseProp().getTargetType().getClassName()
                );
                addConverterLoading(cb, prop, false);
                cb.add(")");
            }
        } else if (tailProp.getTargetType() != null) {
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(
                        ",\n$T.<$T, $T>$L($T::new)",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().getTargetType().getClassName(),
                        getPropElementName(tailProp),
                        tailProp.getBaseProp().isList() ? "objectListGetter" : "objectReferenceGetter",
                        getPropElementName(tailProp)
                );
                cb.add(
                        ",\n$T.$L($T::$L)",
                        org.babyfish.jimmer.apt.immutable.generator.Constants.DTO_PROP_ACCESSOR_CLASS_NAME,
                        tailProp.getBaseProp().isList() ? "objectListSetter" : "objectReferenceSetter",
                        getPropElementName(tailProp),
                        tailProp.getTargetType().getBaseType().isEntity() ? "toEntity" : "toImmutable"
                );
            }
        } else if (prop.getEnumType() != null) {
            EnumType enumType = prop.getEnumType();
            TypeName enumTypeName = tailProp.getBaseProp().getTypeName();
            if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                cb.add(",\nnull");
            } else {
                cb.add(",\narg -> {\n");
                cb.indent();
                cb.beginControlFlow("switch (($T)arg)", enumTypeName);
                for (Map.Entry<String, String> e: enumType.getValueMap().entrySet()) {
                    cb.add("case $L:\n", e.getKey());
                    cb.indent();
                    cb.addStatement("return $L", e.getValue());
                    cb.unindent();
                }
                cb.add("default:\n");
                cb.indent();
                cb.addStatement(
                        "throw new AssertionError($S)",
                        "Internal bug"
                );
                cb.unindent();
                cb.endControlFlow();
                cb.unindent();
                cb.add("}");
            }
            cb.add(",\narg -> {\n");
            cb.indent();
            addValueToEnum(cb, prop, "arg");
            cb.unindent();
            cb.add("}");
        } else if (converterMetadataOf(prop) != null) {
            cb.add(",\narg -> ");
            addConverterLoading(cb, prop, true);
            cb.add(".output(arg)");
            cb.add(",\narg -> ");
            addConverterLoading(cb, prop, true);
            cb.add(".input(arg)");
        }

        cb.unindent();
        cb.add("\n)");
        builder.initializer(cb.build());
        typeBuilder.addField(builder.build());
    }

    private void addValueToEnum(CodeBlock.Builder cb, DtoProp<ImmutableType, ImmutableProp> prop, String variableName) {
        EnumType enumType = prop.getEnumType();
        TypeName enumTypeName = prop.toTailProp().getBaseProp().getTypeName();
        cb.beginControlFlow("switch (($T)$L)", enumType.isNumeric() ? TypeName.INT : org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME, variableName);
        for (Map.Entry<String, String> e: enumType.getConstantMap().entrySet()) {
            cb.add("case $L:\n", e.getKey());
            cb.indent();
            cb.addStatement("return $T.$L", enumTypeName, e.getValue());
            cb.unindent();
        }
        cb.add("default:\n");
        cb.indent();
        cb.addStatement(
                "throw new IllegalArgumentException($S + $L + $S)",
                "Illegal value `\"",
                variableName,
                "\"`for enum type: \"" + enumTypeName + "\""
        );
        cb.unindent();
        cb.endControlFlow();
    }

    private void addConverterLoading(CodeBlock.Builder cb, DtoProp<ImmutableType, ImmutableProp> prop, boolean forList) {
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        cb.add(
                "$T.$L.unwrap().$L",
                baseProp.getDeclaringType().getPropsClassName(),
                StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER),
                prop.toTailProp().getBaseProp().isAssociation(true) ?
                        "getAssociatedIdConverter(" + forList + ")" :
                        "getConverter()"
        );
    }

    private boolean isSimpleProp(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (prop.getNextProp() != null) {
            return false;
        }
        if ((prop.isNullable() && (!prop.isBaseNullable() || dtoType.getModifiers().contains(DtoModifier.SPECIFICATION))) ||
                (prop.getBaseProp().getConverterMetadata() != null &&
                        !dtoType.getModifiers().contains(DtoModifier.INPUT) &&
                        !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION))
        ) {
            return false;
        }
        return getPropTypeName(prop).equals(prop.getBaseProp().getTypeName());
    }

    private void addHiddenFetcherField(DtoProp<ImmutableType, ImmutableProp> prop, CodeBlock.Builder cb) {
        if (!"flat".equals(prop.getFuncName())) {
            addFetcherField(prop, cb);
            return;
        }
        DtoType<ImmutableType, ImmutableProp> targetDtoType = prop.getTargetType();
        assert targetDtoType != null;
        cb.add("\n.$N($>", prop.getBaseProp().getName());
        cb.add("$T.$L$>", prop.getBaseProp().getTargetType().getFetcherClassName(), "$");
        for (DtoProp<ImmutableType, ImmutableProp> childProp : targetDtoType.getDtoProps()) {
            addHiddenFetcherField(childProp, cb);
        }
        cb.add("$<$<\n)");
    }

    private void addField(DtoProp<ImmutableType, ImmutableProp> prop) {
        TypeName typeName = getPropTypeName(prop);
        if (isFieldNullable(prop)) {
            typeName = typeName.box();
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getName())
                .addModifiers(ctx.getDtoFieldModifier());
        String doc = doc(prop, true);
        if (doc != null) {
            builder.addJavadoc(doc);
        }
        if (dtoType.getModifiers().contains(DtoModifier.INPUT) && prop.getInputModifier() == DtoModifier.FIXED) {
            builder.addAnnotation(org.babyfish.jimmer.apt.immutable.generator.Constants.FIXED_INPUT_FIELD_CLASS_NAME);
        }
        boolean isBuilderRequired = isBuildRequired();
        for (AnnotationMirror annotationMirror : prop.toTailProp().getBaseProp().getAnnotations()) {
            if (isBuilderRequired) {
                String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType()
                        .asElement())
                        .getQualifiedName()
                        .toString();
                if (qualifiedName.equals(JSON_DESERIALIZE_TYPE_NAME)) {
                    continue;
                }
            }
            if (isCopyableAnnotation(annotationMirror, dtoType.getAnnotations(), false)) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        for (Anno anno : prop.getAnnotations()) {
            if (isBuilderRequired && anno.getQualifiedName().equals(JSON_DESERIALIZE_TYPE_NAME)) {
                continue;
            }
            if (hasElementType(anno, ElementType.FIELD)) {
                builder.addAnnotation(annotationOf(anno));
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addField(UserProp prop) {
        TypeName typeName = getPropTypeName(prop);
        if (isFieldNullable(prop)) {
            typeName = typeName.box();
        }
        FieldSpec.Builder builder = FieldSpec
                .builder(typeName, prop.getAlias())
                .addModifiers(ctx.getDtoFieldModifier());
        if (prop.getDefaultValueText() != null) {
            builder.initializer(prop.getDefaultValueText());
        }
        String doc = doc(prop, true);
        if (doc != null) {
            builder.addJavadoc(doc);
        }
        for (Anno anno : prop.getAnnotations()) {
            if (hasElementType(anno, ElementType.FIELD)) {
                builder.addAnnotation(annotationOf(anno));
            }
        }
        typeBuilder.addField(builder.build());
    }

    private void addStateField(DtoProp<ImmutableType, ImmutableProp> prop) {
        String stateFieldName = stateFieldName(prop, false);
        if (stateFieldName == null) {
            return;
        }
        typeBuilder.addField(
                TypeName.BOOLEAN,
                stateFieldName,
                ctx.getDtoFieldModifier()
        );
    }

    @SuppressWarnings("unchecked")
    private void addAccessors(AbstractProp prop) {
        TypeName typeName = getPropTypeName(prop);
        String getterName = getterName(prop);
        String setterName = setterName(prop);
        String stateFieldName = stateFieldName(prop, false);

        MethodSpec.Builder getterBuilder = MethodSpec
                .methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName);
        if (interfaceMethodNames.contains(getterName)) {
            getterBuilder.addAnnotation(Override.class);
        }
        if (!(prop instanceof DtoProp<?, ?>) || ((DtoProp<?, ?>)prop).getNextProp() == null) {
            String doc = doc(prop, false);
            if (doc != null && !doc.isEmpty()) {
                getterBuilder.addAnnotation(
                        AnnotationSpec.builder(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.DESCRIPTION_CLASS_NAME
                        ).addMember(
                                "value", "$S", doc
                        ).build()
                );
            }
        }
        if (!typeName.isPrimitive()) {
            if (prop.isNullable()) {
                getterBuilder.addAnnotation(Nullable.class);
            } else {
                getterBuilder.addAnnotation(NotNull.class);
            }
        }
        boolean isBuilderRequired = isBuildRequired();
        if (prop instanceof DtoProp<?, ?>) {
            DtoProp<ImmutableType, ImmutableProp> dtoProp = (DtoProp<ImmutableType, ImmutableProp>) prop;
            for (AnnotationMirror annotationMirror : dtoProp.toTailProp().getBaseProp().getAnnotations()) {
                if (isBuilderRequired) {
                    String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType()
                            .asElement())
                            .getQualifiedName()
                            .toString();
                    if (qualifiedName.equals(JSON_DESERIALIZE_TYPE_NAME)) {
                        continue;
                    }
                }
                if (isCopyableAnnotation(annotationMirror, dtoProp.getAnnotations(), true)) {
                    getterBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
                }
            }
        }
        for (Anno anno : prop.getAnnotations()) {
            if (isBuilderRequired && anno.getQualifiedName().equals(JSON_DESERIALIZE_TYPE_NAME)) {
                continue;
            }
            if (hasElementType(anno, ElementType.METHOD)) {
                getterBuilder.addAnnotation(annotationOf(anno));
            }
        }
        if (stateFieldName != null) {
            getterBuilder.beginControlFlow(
                    "if ($L)",
                    '!' + stateFieldName
            );
            getterBuilder.addStatement(
                    "throw new IllegalStateException($S)",
                    "The property \"" + prop.getName() + "\" is not specified"
            );
            getterBuilder.endControlFlow();
        }
        if (!prop.isNullable() && isFieldNullable(prop)) {
            getterBuilder.beginControlFlow(
                    "if ($L == null)",
                    prop.getName()
            );
            if (dtoType.getModifiers().contains(DtoModifier.INPUT) &&
                    typeName instanceof ParameterizedTypeName &&
                    LIST_CLASS_NAME.equals(((ParameterizedTypeName) typeName).rawType)) {
                getterBuilder.addComment(
                        "GraphQLInput requires `obj." +
                                getterName +
                                "().add(...)`"
                );
                getterBuilder.addStatement(
                        "return this.$L = new $T<>()",
                        prop.getName(),
                        ARRAY_LIST_CLASS_NAME
                );
            } else {
                getterBuilder.addStatement(
                        "throw new IllegalStateException($S)",
                        "The property \"" + prop.getName() + "\" is not specified"
                );
            }
            getterBuilder.endControlFlow();
        }
        getterBuilder.addStatement("return $L", prop.getName());
        typeBuilder.addMethod(getterBuilder.build());

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(typeName, prop.getName());
        if (!typeName.isPrimitive()) {
            if (prop.isNullable()) {
                parameterBuilder.addAnnotation(Nullable.class);
            } else {
                parameterBuilder.addAnnotation(NotNull.class);
            }
        }
        MethodSpec.Builder setterBuilder = MethodSpec
                .methodBuilder(setterName)
                .addParameter(parameterBuilder.build())
                .addModifiers(Modifier.PUBLIC);
        if (interfaceMethodNames.contains(setterName)) {
            setterBuilder.addAnnotation(Override.class);
        }
        setterBuilder.addStatement("this.$L = $L", prop.getName(), prop.getName());
        if (stateFieldName != null) {
            setterBuilder.addStatement("this.$L = true", stateFieldName);
        }
        typeBuilder.addMethod(setterBuilder.build());

        if (stateFieldName != null) {
            MethodSpec.Builder isLoadedBuilder = MethodSpec
                    .methodBuilder(StringUtil.identifier("is", prop.getName(), "Loaded"))
                    .returns(TypeName.BOOLEAN)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(ApiIgnore.class)
                    .addAnnotation(JsonIgnore.class)
                    .addStatement("return this.$L", stateFieldName);
            typeBuilder.addMethod(isLoadedBuilder.build());
            MethodSpec.Builder setLoadedBuilder = MethodSpec
                    .methodBuilder(StringUtil.identifier("set", prop.getName(), "Loaded"))
                    .addParameter(TypeName.BOOLEAN, "loaded")
                    .addStatement("this.$L = loaded", stateFieldName);
            typeBuilder.addMethod(setLoadedBuilder.build());
        }
    }

    @SuppressWarnings("unchecked")
    private String doc(AbstractProp prop, boolean contentOnly) {
        String doc = document.get(prop);
        if (doc == null & prop instanceof DtoProp<?, ?>) {
            DtoProp<ImmutableType, ImmutableProp> dtoProp = (DtoProp<ImmutableType, ImmutableProp>) prop;
            if (dtoProp.getBasePropMap().isEmpty() && dtoProp.getFuncName() == null) {
                doc = baseDocComment(dtoProp.toTailProp().getBaseProp());
            }
        }
        if (doc == null) {
            return null;
        }
        if (contentOnly) {
            int index = -1;
            index = docKeyIndex(index, doc, "@param");
            index = docKeyIndex(index, doc, "@return");
            index = docKeyIndex(index, doc, "@exception");
            index = docKeyIndex(index, doc, "@throws");
            index = docKeyIndex(index, doc, "@see");
            if (index != -1) {
                doc = doc.substring(0, index);
            }
        }
        return doc;
    }

    private void addDefaultConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        typeBuilder.addMethod(builder.build());
    }

    private void addConverterConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        ParameterSpec
                                .builder(dtoType.getBaseType().getClassName(), "base")
                                .addAnnotation(NotNull.class)
                                .build()
                );
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            if (isSimpleProp(prop)) {
                if (prop.isNullable()) {
                    builder.addStatement(
                            "this.$L = (($T)base).__isLoaded($T.byIndex($T.$L)) ? base.$L() : null",
                            prop.getName(),
                            ImmutableSpi.class,
                            org.babyfish.jimmer.apt.immutable.generator.Constants.PROP_ID_CLASS_NAME,
                            dtoType.getBaseType().getProducerClassName(),
                            prop.getBaseProp().getSlotName(),
                            prop.getBaseProp().getGetterName()
                    );
                } else {
                    builder.addStatement(
                            "this.$L = base.$L()",
                            prop.getName(),
                            prop.getBaseProp().getGetterName()
                    );
                }
            } else {
                if (!prop.isNullable() && prop.isBaseNullable()) {
                    builder.addStatement(
                            "this.$L = $L.get($>\n" +
                                    "base,\n" +
                                    "$S\n" +
                                    "$<)",
                            prop.getName(),
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                            "Cannot convert \"" +
                                    dtoType.getBaseType().getClassName() +
                                    "\" to " +
                                    "\"" +
                                    getDtoClassName() +
                                    "\" because the cannot get non-null " +
                                    "value for \"" +
                                    prop.getName() +
                                    "\""
                    );
                } else {
                    builder.addStatement(
                            "this.$L = $L.get(base)",
                            prop.getName(),
                            StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER)
                    );
                }
            }
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addToEntity(boolean withId) {
        boolean idOverridable =
                dtoType.getModifiers().contains(DtoModifier.INPUT) &&
                dtoType.getBaseType().isEntity();
        if (withId && !idOverridable) {
            return;
        }
        ImmutableProp baseIdProp = withId ? dtoType.getBaseType().getIdProp() : null;
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(dtoType.getBaseType().isEntity() ?
                        (withId ? "toEntityById" : "toEntity") :
                        "toImmutable");
        if (baseIdProp != null) {
            builder.addParameter(
                    ParameterSpec.builder(
                            baseIdProp.getTypeName().box(),
                            "id"
                    ).addAnnotation(Nullable.class).build()
            );
        } else {
            builder.addAnnotation(Override.class);
        }
        builder.addModifiers(Modifier.PUBLIC)
                .returns(dtoType.getBaseType().getClassName());
        if (!withId && idOverridable) {
            builder.addStatement("return toEntityById(null)");
        } else {
            builder.addCode(
                    "return $T.$L.produce(__draft -> {$>\n",
                    dtoType.getBaseType().getDraftClassName(),
                    "$"
            );
            for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
                if (prop.getBaseProp().isJavaFormula()) {
                    continue;
                }
                String stateFieldName = stateFieldName(prop, false);
                if (stateFieldName != null) {
                    builder.beginControlFlow("if ($L)", stateFieldName);
                }
                if (isSimpleProp(prop)) {
                    builder.addStatement("__draft.$L(this.$L)", prop.getBaseProp().getSetterName(), prop.getName());
                } else {
                    ImmutableProp tailBaseProp = prop.toTailProp().getBaseProp();
                    if (tailBaseProp.isList() && tailBaseProp.isAssociation(true)) {
                        builder.addStatement(
                                "$L.set(__draft, this.$L != null ? this.$L : $T.emptyList())",
                                StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                                prop.getName(),
                                prop.getName(),
                                org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTIONS_CLASS_NAME
                        );
                    } else {
                        builder.addStatement(
                                "$L.set(__draft, this.$L)",
                                StringUtil.snake(prop.getName() + "Accessor", StringUtil.SnakeCase.UPPER),
                                prop.getName()
                        );
                    }
                }
                if (stateFieldName != null) {
                    builder.endControlFlow();
                }
            }
            if (baseIdProp != null) {
                builder.beginControlFlow("if (id != null)");
                builder.addStatement("__draft.$L($L)", baseIdProp.getSetterName(), "id");
                builder.endControlFlow();
            }
            builder.addCode("$<});\n");
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addEntityType() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("entityType")
                .returns(
                        ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.CLASS_CLASS_NAME,
                                dtoType.getBaseType().getClassName()
                        )
                )
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $T.class", dtoType.getBaseType().getClassName());
        if (isImpl()) {
            builder.addAnnotation(Override.class);
        }
        typeBuilder.addMethod(builder.build());
    }

    private void addApplyTo() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("applyTo")
                .addModifiers(Modifier.PUBLIC);
        if (isImpl()) {
            builder.addAnnotation(Override.class)
                    .addParameter(
                            ParameterSpec.builder(
                                    ParameterizedTypeName.get(
                                            org.babyfish.jimmer.apt.immutable.generator.Constants.SPECIFICATION_ARGS_CLASS_NAME,
                                            dtoType.getBaseType().getClassName(),
                                            dtoType.getBaseType().getTableClassName()
                                    ),
                                    "args"
                            ).build()
                    );
        } else {
            builder.addParameter(
                    ParameterSpec
                            .builder(
                                    org.babyfish.jimmer.apt.immutable.generator.Constants.PREDICATE_APPLIER_CLASS_NAME,
                                    "__applier"
                            )
                            .build()
            );
        }

        List<ImmutableProp> stack = Collections.emptyList();
        if (isImpl()) {
            builder.addStatement(
                    "$T __applier = args.getApplier()",
                    org.babyfish.jimmer.apt.immutable.generator.Constants.PREDICATE_APPLIER_CLASS_NAME
            );
        }
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            List<ImmutableProp> newStack = new ArrayList<>(stack.size() + 2);
            DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
            for (DtoProp<ImmutableType, ImmutableProp> p = prop; p != null; p = p.getNextProp()) {
                if (p != tailProp || p.getTargetType() != null) {
                    newStack.add(p.getBaseProp());
                }
            }
            stack = addStackOperations(builder, stack, newStack);
            addPredicateOperation(builder, prop);
        }
        addStackOperations(builder, stack, Collections.emptyList());
        typeBuilder.addMethod(builder.build());
    }

    private List<ImmutableProp> addStackOperations(
            MethodSpec.Builder builder,
            List<ImmutableProp> stack,
            List<ImmutableProp> newStack
    ) {
        int size = Math.min(stack.size(), newStack.size());
        int sameCount = size;
        for (int i = 0; i < size; i++) {
            if (stack.get(i) != newStack.get(i)) {
                sameCount = i;
                break;
            }
        }
        for (int i = stack.size() - sameCount; i > 0; --i) {
            builder.addStatement("__applier.pop()");
        }
        for (ImmutableProp prop : newStack.subList(sameCount, newStack.size())) {
            builder.addStatement(
                    "__applier.push($T.$L.unwrap())",
                    prop.getDeclaringType().getPropsClassName(),
                    StringUtil.snake(prop.getName(), StringUtil.SnakeCase.UPPER)
            );
        }
        return newStack;
    }

    private void addPredicateOperation(MethodSpec.Builder builder, DtoProp<ImmutableType, ImmutableProp> prop) {
        String propName = prop.getName();
        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        if (tailProp.getTargetType() != null) {
            builder.beginControlFlow("if (this.$L != null)", propName);
            if (tailProp.getTargetType().getBaseType().isEntity()) {
                builder.addStatement("this.$L.applyTo(args.child())", propName);
            } else {
                builder.addStatement("this.$L.applyTo(args.getApplier())", propName);
            }
            builder.endControlFlow();
            return;
        }

        String funcName = tailProp.getFuncName();
        String javaMethodName = funcName;
        if (funcName == null) {
            funcName = "eq";
            javaMethodName = "eq";
        } else if ("null".equals(funcName)) {
            javaMethodName = "isNull";
        } else if ("notNull".equals(funcName)) {
            javaMethodName = "isNotNull";
        } else if ("id".equals(funcName)) {
            funcName = "associatedIdEq";
            javaMethodName = "associatedIdEq";
        }

        CodeBlock.Builder cb = CodeBlock.builder();
        if (org.babyfish.jimmer.dto.compiler.Constants.MULTI_ARGS_FUNC_NAMES.contains(funcName)) {
            cb.add("__applier.$L(new $T[] { ", javaMethodName, org.babyfish.jimmer.apt.immutable.generator.Constants.IMMUTABLE_PROP_CLASS_NAME);
            boolean addComma = false;
            for (ImmutableProp baseProp : tailProp.getBasePropMap().values()) {
                if (addComma) {
                    cb.add(", ");
                } else {
                    addComma = true;
                }
                cb.add(
                        "$T.$L.unwrap()",
                        baseProp.getDeclaringType().getPropsClassName(),
                        StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER)
                );
            }
            cb.add(" }, ");
        } else {
            cb.add(
                    "__applier.$L($T.$L.unwrap(), ",
                    funcName,
                    tailProp.getBaseProp().getDeclaringType().getPropsClassName(),
                    StringUtil.snake(tailProp.getBaseProp().getName(), StringUtil.SnakeCase.UPPER)
            );
        }
        if (isSpecificationConverterRequired(tailProp)) {
            cb.add(
                    "$L(this.$L)",
                    StringUtil.identifier("__convert", propName),
                    propName
            );
        } else {
            cb.add("this.$L", propName);
        }
        if ("like".equals(funcName) || "notLike".equals(funcName)) {
            cb.add(", ");
            cb.add(tailProp.getLikeOptions().contains(LikeOption.INSENSITIVE) ? "true" : "false");
            cb.add(", ");
            cb.add(tailProp.getLikeOptions().contains(LikeOption.MATCH_START) ? "true" : "false");
            cb.add(", ");
            cb.add(tailProp.getLikeOptions().contains(LikeOption.MATCH_END) ? "true" : "false");
        }
        cb.addStatement(")");
        builder.addCode(cb.build());
    }

    private void addSpecificationConverter(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (!isSpecificationConverterRequired(prop)) {
            return;
        }
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        TypeName baseTypeName = null;
        String funcName = prop.getFuncName();
        if (funcName != null) {
            switch (funcName) {
                case "id":
                    baseTypeName = baseProp.getTargetType().getIdProp().getTypeName();
                    if (baseProp.isList() && !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                        baseTypeName = ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                                baseTypeName.box()
                        );
                    }
                    break;
                case "null":
                case "notNull":
                    baseTypeName = TypeName.BOOLEAN;
                    break;
                case "valueIn":
                case "valueNotIn":
                    baseTypeName = ParameterizedTypeName.get(
                            org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                            baseProp.getTypeName().box()
                    );
                    break;
                case "associatedIdEq":
                case "associatedIdNe":
                    baseTypeName = baseProp.getTargetType().getIdProp().getTypeName();
                    break;
                case "associatedIdIn":
                case "associatedIdNotIn":
                    baseTypeName = ParameterizedTypeName.get(
                            org.babyfish.jimmer.apt.immutable.generator.Constants.LIST_CLASS_NAME,
                            baseProp.getTargetType().getIdProp().getTypeName().box()
                    );
            }
        }
        if (baseTypeName == null) {
            baseTypeName = baseProp.getTypeName();
        }
        baseTypeName = baseTypeName.box();
        TypeName dtoPropTypeName = getPropTypeName(prop);
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtil.identifier("__convert", prop.getName()))
                .addModifiers(Modifier.PRIVATE)
                .addParameter(dtoPropTypeName, "value")
                .returns(baseTypeName);
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.beginControlFlow("if ($L == null)", prop.getName());
        cb.addStatement("return null");
        cb.endControlFlow();
        if (prop.getEnumType() != null) {
            addValueToEnum(cb, prop, "value");
        } else {
            cb.addStatement(
                    "return $T.$L.unwrap().<$T, $T>$L.input(value)",
                    baseProp.getDeclaringType().getPropsClassName(),
                    StringUtil.snake(baseProp.getName(), StringUtil.SnakeCase.UPPER),
                    baseTypeName,
                    getPropTypeName(prop).box(),
                    baseProp.isAssociation(true) ?
                            "getAssociatedIdConverter(true)" :
                            "getConverter(" + (prop.isFunc("valueIn", "valueNotIn") ? "true" : "") + ")"
            );
        }
        builder.addCode(cb.build());
        typeBuilder.addMethod(builder.build());
    }

    private void addHibernateValidatorEnhancement(boolean getter) {
        String methodName = "$$_hibernateValidator_get" +
                (getter ? "Getter" : "Field") +
                "Value";
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME, "name")
                .returns(TypeName.OBJECT)
                .beginControlFlow("switch (name)");
        for (AbstractProp prop : dtoType.getProps()) {
            builder.addStatement(
                    "case $S: return $L",
                    getter ?
                            StringUtil.identifier(
                                    getPropTypeName(prop) == TypeName.BOOLEAN ? "is" : "get",
                                    prop.getName()
                            ) :
                            prop.getName(),
                    prop.getName()
            );
        }
        builder
                .addStatement(
                        "default: throw new IllegalArgumentException($S + name + $S)",
                        "No " + (getter ? "getter" : "field") + " named \"",
                        "\""
                )
                .endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    @SuppressWarnings("unchecked")
    public TypeName getPropTypeName(AbstractProp prop) {
        if (prop instanceof DtoProp<?, ?>) {
            return getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop);
        }
        return getTypeName(((UserProp)prop).getTypeRef());
    }

    private TypeName getPropTypeName(DtoProp<ImmutableType, ImmutableProp> prop) {

        ImmutableProp baseProp = prop.toTailProp().getBaseProp();

        EnumType enumType = prop.getEnumType();
        if (enumType != null) {
            if (enumType.isNumeric()) {
                return prop.isNullable() ? TypeName.INT.box() : TypeName.INT;
            }
            return org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME;
        }
        ConverterMetadata metadata = converterMetadataOf(prop);
        final TypeName propElementName = getPropElementName(prop);
        if (dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            String funcName = prop.toTailProp().getFuncName();
            if (funcName != null) {
                switch (funcName) {
                    case "null":
                    case "notNull":
                        return TypeName.BOOLEAN;
                    case "valueIn":
                    case "valueNotIn":
                        return ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTION_CLASS_NAME,
                                metadata != null ?
                                        metadata.getTargetTypeName() :
                                        toListType(
                                                propElementName,
                                            baseProp.isList()
                                        )
                        );
                    case "id":
                    case "associatedIdEq":
                    case "associatedIdNe":
                        final TypeName clientTypeName = baseProp.getTargetType().getIdProp().getClientTypeName();
                        if (prop.isNullable()) {
                            return clientTypeName.box();
                        }
                        return clientTypeName;
                    case "associatedIdIn":
                    case "associatedIdNotIn":
                        return ParameterizedTypeName.get(
                                org.babyfish.jimmer.apt.immutable.generator.Constants.COLLECTION_CLASS_NAME,
                                baseProp.getTargetType().getIdProp().getClientTypeName().box()
                        );
                }
            }
            if (baseProp.isAssociation(true)) {
                return propElementName;
            }
        }
        if (metadata != null) {
            return metadata.getTargetTypeName();
        }

        return toListType(propElementName, baseProp.isList()
                && !(propElementName instanceof ParameterizedTypeName && ((ParameterizedTypeName) propElementName).rawType.equals(LIST_CLASS_NAME)));
    }

    private static TypeName toListType(TypeName typeName, boolean isList) {
        return isList ? ParameterizedTypeName.get(LIST_CLASS_NAME, typeName.box()) : typeName;
    }

    private static TypeName getTypeName(@Nullable TypeRef typeRef) {
        return getTypeName(typeRef, false);
    }

    private static TypeName getTypeName(@Nullable TypeRef typeRef, boolean toBoxType) {
        if (typeRef == null) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }
        TypeName typeName;
        switch (typeRef.getTypeName()) {
            case "Boolean":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.BOOLEAN.box() : TypeName.BOOLEAN;
                break;
            case "Char":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.CHAR.box() : TypeName.CHAR;
                break;
            case "Byte":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.BYTE.box() : TypeName.BYTE;
                break;
            case "Short":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.SHORT.box() : TypeName.SHORT;
                break;
            case "Int":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.INT.box() : TypeName.INT;
                break;
            case "Long":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.LONG.box() : TypeName.LONG;
                break;
            case "Float":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.FLOAT.box() : TypeName.FLOAT;
                break;
            case "Double":
                typeName = toBoxType || typeRef.isNullable() ? TypeName.DOUBLE.box() : TypeName.DOUBLE;
                break;
            case "Any":
                typeName = TypeName.OBJECT;
                break;
            case "String":
                typeName = org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME;
                break;
            case "Array":
                typeName = ArrayTypeName.of(
                        typeRef.getArguments().get(0).getTypeRef() == null ?
                                TypeName.OBJECT :
                                getTypeName(typeRef.getArguments().get(0).getTypeRef(), false)
                );
                break;
            case "Iterable":
            case "MutableIterable":
                typeName = ClassName.get(Iterable.class);
                break;
            case "Collection":
            case "MutableCollection":
                typeName = ClassName.get(Collection.class);
                break;
            case "List":
            case "MutableList":
                typeName = ClassName.get(List.class);
                break;
            case "Set":
            case "MutableSet":
                typeName = ClassName.get(Set.class);
                break;
            case "Map":
            case "MutableMap":
                typeName = ClassName.get(Map.class);
                break;
            default:
                typeName = ClassName.bestGuess(typeRef.getTypeName());
                break;
        }
        int argCount = typeRef.getArguments().size();
        if (argCount == 0 || typeName instanceof ArrayTypeName) {
            return typeName;
        }
        TypeName[] argTypeNames = new TypeName[argCount];
        for (int i = 0; i < argCount; i++) {
            TypeRef.Argument arg = typeRef.getArguments().get(i);
            TypeName argTypeName = getTypeName(arg.getTypeRef(), true);
            if (arg.isIn()) {
                argTypeName = WildcardTypeName.supertypeOf(argTypeName);
            } else if (arg.getTypeRef() != null && (arg.isOut() || isForceOut(typeRef.getTypeName()))) {
                argTypeName = WildcardTypeName.subtypeOf(argTypeName);
            }
            argTypeNames[i] = argTypeName;
        }
        return ParameterizedTypeName.get(
                (ClassName) typeName,
                argTypeNames
        );
    }

    private void addHashCode() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.INT);
        boolean first = true;
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            CodeBlock.Builder cb = CodeBlock.builder();
            if (first) {
                cb.add("int hash = ");
                first = false;
            } else {
                cb.add("hash = hash * 31 + ");
            }
            TypeName typeName = getPropTypeName(prop);
            if (typeName.isPrimitive()) {
                cb.add(
                        "$T.hashCode($L)",
                        typeName.box(),
                        prop.getName().equals("hash") ? "this." + prop.getName() : prop.getName()
                );
            } else if (typeName instanceof ArrayTypeName) {
                cb.add("$T.hashCode($L)", Arrays.class, prop.getName());
            } else {
                cb.add("$T.hashCode($L)", Objects.class, prop.getName());
            }
            builder.addStatement(cb.build());
            String stateFieldName = stateFieldName(prop, false);
            if (stateFieldName != null) {
                builder.addStatement("hash = hash * 31 + Boolean.hashCode($L)", stateFieldName);
            }
        }
        for (UserProp prop : dtoType.getUserProps()) {
            CodeBlock.Builder cb = CodeBlock.builder();
            if (first) {
                cb.add("int hash = ");
                first = false;
            } else {
                cb.add("hash = hash * 31 + ");
            }
            TypeName typeName = getTypeName(prop.getTypeRef());
            if (typeName.isPrimitive()) {
                cb.add(
                        "$T.hashCode($L)",
                        typeName.box(),
                        prop.getAlias().equals("hash") ? "this." + prop.getAlias() : prop.getAlias()
                );
            } else if (typeName instanceof ArrayTypeName) {
                cb.add("$T.hashCode($L)", Arrays.class, prop.getName());
            } else {
                cb.add("$T.hashCode($L)", Objects.class, prop.getAlias());
            }
            builder.addStatement(cb.build());
        }
        builder.addStatement(first ? "return 0" : "return hash");
        typeBuilder.addMethod(builder.build());
    }

    private void addEquals() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.OBJECT, "o")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN);
        builder.beginControlFlow("if (o == null || this.getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow();
        builder.addStatement("$L other = ($L) o", getSimpleName(), getSimpleName());
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            String propName = prop.getName();
            String stateFieldName = stateFieldName(prop, false);
            if (stateFieldName != null) {
                builder.beginControlFlow("if ($L != other.$L)", stateFieldName, stateFieldName);
                builder.addStatement("return false");
                builder.endControlFlow();
            }
            String thisProp = propName.equals("o") || propName.equals("other") ? "this" + propName : propName;
            TypeName typeName = getPropTypeName(prop);
            if (stateFieldName != null) {
                if (typeName.isPrimitive()) {
                    builder.beginControlFlow(
                            "if ($L && $L != other.$L)",
                            stateFieldName,
                            thisProp,
                            propName
                    );
                } else {
                    builder.beginControlFlow(
                            "if ($L && !$T.equals($L, other.$L))",
                            stateFieldName,
                            Objects.class,
                            thisProp,
                            propName
                    );
                }
            } else {
                if (typeName.isPrimitive()) {
                    builder.beginControlFlow("if ($L != other.$L)", thisProp, propName);
                } else if (typeName instanceof ArrayTypeName) {
                    builder.beginControlFlow("if (!$T.equals($L, other.$L))", Arrays.class, thisProp, propName);
                } else {
                    builder.beginControlFlow("if (!$T.equals($L, other.$L))", Objects.class, thisProp, propName);
                }
            }
            builder.addStatement("return false");
            builder.endControlFlow();
        }
        for (UserProp prop : dtoType.getUserProps()) {
            String propName = prop.getAlias();
            String thisProp = propName.equals("o") || propName.equals("other") ? "this" + propName : propName;
            TypeName typeName = getTypeName(prop.getTypeRef());
            if (typeName.isPrimitive()) {
                builder.beginControlFlow("if ($L != other.$L)", thisProp, propName);
            } else if (typeName instanceof ArrayTypeName) {
                builder.beginControlFlow("if (!$T.equals($L, other.$L))", Arrays.class, thisProp, propName);
            } else {
                builder.beginControlFlow("if (!$T.equals($L, other.$L))", Objects.class, thisProp, propName);
            }
            builder.addStatement("return false");
            builder.endControlFlow();
        }
        builder.addStatement("return true");
        typeBuilder.addMethod(builder.build());
    }

    private void addToString() {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(org.babyfish.jimmer.apt.immutable.generator.Constants.STRING_CLASS_NAME);
        builder.addStatement("StringBuilder builder = new StringBuilder()");
        builder.addStatement("builder.append($S).append('(')", simpleNamePath());
        String separator = "";
        for (DtoProp<ImmutableType, ImmutableProp> prop : dtoType.getDtoProps()) {
            String stateFieldName = stateFieldName(prop, false);
            if (stateFieldName != null) {
                builder.beginControlFlow("if ($L)", stateFieldName);
            } else if (prop.getInputModifier() == DtoModifier.FUZZY) {
                builder.beginControlFlow("if ($L != null)", prop.getName());
            }
            if (prop.getName().equals("builder")) {
                builder.addStatement("builder.append($S).append(this.$L)", separator + prop.getName() + '=', prop.getName());
            } else {
                builder.addStatement("builder.append($S).append($L)", separator + prop.getName() + '=', prop.getName());
            }
            if (stateFieldName != null || prop.getInputModifier() == DtoModifier.FUZZY) {
                builder.endControlFlow();
            }
            separator = ", ";
        }
        for (UserProp prop : dtoType.getUserProps()) {
            if (prop.getAlias().equals("builder")) {
                builder.addStatement("builder.append($S).append(this.$L)", separator + prop.getAlias() + '=', prop.getAlias());
            } else {
                builder.addStatement("builder.append($S).append($L)", separator + prop.getAlias() + '=', prop.getAlias());
            }
            separator = ", ";
        }
        builder.addStatement("builder.append(')')");
        builder.addStatement("return builder.toString()");
        typeBuilder.addMethod(builder.build());
    }

    private String simpleNamePath() {
        String name = getSimpleName();
        if (parent != null) {
            return parent.simpleNamePath() + '.' + name;
        }
        return name;
    }

    public TypeName getPropElementName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoProp<ImmutableType, ImmutableProp> tailProp = prop.toTailProp();
        DtoType<ImmutableType, ImmutableProp> targetType = tailProp.getTargetType();
        if (targetType != null) {
            if (tailProp.isRecursive() && !targetType.isFocusedRecursion()) {
                return getDtoClassName();
            }
            if (targetType.getName() == null) {
                List<String> list = new ArrayList<>();
                collectNames(list);
                if (!tailProp.isRecursive() || targetType.isFocusedRecursion()) {
                    list.add(targetSimpleName(tailProp));
                }
                return ClassName.get(
                        root.dtoType.getPackageName(),
                        list.get(0),
                        list.subList(1, list.size()).toArray(EMPTY_STR_ARR)
                );
            }
            return ClassName.get(
                    root.dtoType.getPackageName(),
                    targetType.getName()
            );
        }
        ImmutableProp baseProp = tailProp.getBaseProp();
        TypeName typeName;
        if (tailProp.isIdOnly()) {
            typeName = tailProp.getBaseProp().getTargetType().getIdProp().getTypeName();
        } else if (baseProp.getIdViewBaseProp() != null) {
            typeName = baseProp.getIdViewBaseProp().getTargetType().getIdProp().getClientTypeName();
        } else {
            typeName = tailProp.getBaseProp().getClientTypeName();
        }
        if (typeName.isPrimitive() && prop.isNullable()) {
            return typeName.box();
        }
        return typeName;
    }

    private void collectNames(List<String> list) {
        if (parent == null) {
            list.add(dtoType.getName());
        } else {
            parent.collectNames(list);
            list.add(innerClassName);
        }
    }

    private String targetSimpleName(DtoProp<ImmutableType, ImmutableProp> prop) {
        DtoType<ImmutableType, ImmutableProp> targetType = prop.getTargetType();
        if (targetType == null) {
            throw new IllegalArgumentException("prop is not association");
        }
        if (targetType.getName() != null) {
            return targetType.getName();
        }
        if (prop.isRecursive() && !targetType.isFocusedRecursion()) {
            return innerClassName != null ? innerClassName : dtoType.getName();
        }
        return standardTargetSimpleName("TargetOf_" + prop.getName());
    }

    private String standardTargetSimpleName(String targetSimpleName) {
        boolean conflict = false;
        for (DtoGenerator generator = this; generator != null; generator = generator.parent) {
            if (generator.getSimpleName().equals(targetSimpleName)) {
                conflict = true;
                break;
            }
        }
        if (!conflict) {
            return targetSimpleName;
        }
        for (int i = 2; i < 100; i++) {
            conflict = false;
            String newTargetSimpleName = targetSimpleName + '_' + i;
            for (DtoGenerator generator = this; generator != null; generator = generator.parent) {
                if (generator.getSimpleName().equals(newTargetSimpleName)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                return newTargetSimpleName;
            }
        }
        throw new AssertionError("Dto is too deep");
    }

    private boolean isSpecificationConverterRequired(DtoProp<ImmutableType, ImmutableProp> prop) {
        if (!dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
            return false;
        }
        return prop.getEnumType() != null || converterMetadataOf(prop) != null;
    }

    private ConverterMetadata converterMetadataOf(DtoProp<ImmutableType, ImmutableProp> prop) {
        String funcName = prop.getFuncName();
        if ("null".equals(funcName) || "notNull".equals(funcName)) {
            return null;
        }
        ImmutableProp baseProp = prop.toTailProp().getBaseProp();
        ConverterMetadata metadata = baseProp.getConverterMetadata();
        if (metadata != null) {
            return metadata;
        }
        if ("id".equals(funcName)) {
            metadata = baseProp.getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null && baseProp.isList() && !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION)) {
                metadata = metadata.toListMetadata(baseProp.context());
            }
            return metadata;
        }
        if ("associatedInEq".equals(funcName) || "associatedInNe".equals(funcName)) {
            return baseProp.getTargetType().getIdProp().getConverterMetadata();
        }
        if ("associatedIdIn".equals(funcName) || "associatedIdNotIn".equals(funcName)) {
            metadata = baseProp.getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null) {
                return metadata.toListMetadata(baseProp.context());
            }
        }
        if (baseProp.getIdViewBaseProp() != null) {
            metadata = baseProp.getIdViewBaseProp().getTargetType().getIdProp().getConverterMetadata();
            if (metadata != null) {
                return baseProp.isList() ? metadata.toListMetadata(baseProp.context()) : metadata;
            }
        }
        return null;
    }

    private boolean hasElementType(Anno anno, ElementType elementType) {
        TypeElement annoElement = ctx.getElements().getTypeElement(anno.getQualifiedName());
        if (annoElement == null) {
            throw new DtoException(
                    "Cannot find the annotation declaration whose type is \"" +
                            anno.getQualifiedName() +
                            "\""
            );
        }
        Target target = annoElement.getAnnotation(Target.class);
        if (target != null) {
            for (ElementType et : target.value()) {
                if (et == elementType) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCopyableAnnotation(
            AnnotationMirror annotationMirror,
            Collection<Anno> dtoAnnotations,
            Boolean forMethod
    ) {
        String qualifiedName = ((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().toString();
        if (qualifiedName.startsWith(KOTLIN_DTO_TYPE_NAME)) {
            return false;
        }
        if (qualifiedName.startsWith("org.babyfish.jimmer.") &&
                !qualifiedName.startsWith("org.babyfish.jimmer.client.")) {
            return false;
        }
        if (isNullityAnnotation(qualifiedName)) {
            return false;
        }
        if (forMethod != null) {
            boolean accept = false;
            Target target = annotationMirror.getAnnotationType().asElement().getAnnotation(Target.class);
            if (target != null) {
                if (Arrays.stream(target.value()).anyMatch(it -> it == ElementType.METHOD)) {
                    accept = forMethod;
                } else if (!forMethod) {
                    accept = Arrays.stream(target.value()).anyMatch(it -> it == ElementType.FIELD);
                }
            }
            if (!accept) {
                return false;
            }
        }
        for (Anno dtoAnno : dtoAnnotations) {
            if (dtoAnno.getQualifiedName().endsWith(qualifiedName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNullityAnnotation(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            qualifiedName = qualifiedName.substring(lastDotIndex + 1);
        }
        switch (qualifiedName) {
            case "Null":
            case "Nullable":
            case "NotNull":
            case "NonNull":
                return true;
            default:
                return false;
        }
    }

    static AnnotationSpec annotationOf(Anno anno) {
        AnnotationSpec.Builder builder = AnnotationSpec
                .builder(ClassName.bestGuess(anno.getQualifiedName()));
        for (Map.Entry<String, Anno.Value> e : anno.getValueMap().entrySet()) {
            String name = e.getKey();
            Anno.Value value = e.getValue();
            builder.addMember(name, codeBlockOf(value));
        }
        return builder.build();
    }

    private static CodeBlock codeBlockOf(Anno.Value value) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (value instanceof Anno.ArrayValue) {
            builder.add("{\n$>");
            boolean addSeparator = false;
            for (Anno.Value element : ((Anno.ArrayValue)value).elements) {
                if (addSeparator) {
                    builder.add(", \n");
                } else {
                    addSeparator = true;
                }
                builder.add("$L", codeBlockOf(element));
            }
            builder.add("$<\n}");
        } else if (value instanceof Anno.AnnoValue) {
            builder.add("$L", annotationOf(((Anno.AnnoValue)value).anno));
        } else if(value instanceof Anno.TypeRefValue) {
            builder.add("$T.class", getTypeName(((Anno.TypeRefValue)value).typeRef));
        } else if (value instanceof Anno.EnumValue) {
            builder.add(
                    "$T.$L",
                    ClassName.bestGuess(((Anno.EnumValue)value).qualifiedName),
                    ((Anno.EnumValue)value).constant
            );
        } else if (value instanceof Anno.LiteralValue) {
            builder.add(((Anno.LiteralValue)value).value.replace("$", "$$"));
        }
        return builder.build();
    }

    private static boolean isForceOut(String typeName) {
        switch (typeName) {
            case "Iterable":
            case "Collection":
            case "List":
            case "Set":
            case "Map":
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    String getterName(AbstractProp prop) {
        TypeName typeName = prop instanceof DtoProp<?, ?> ?
                getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop) :
                getTypeName(((UserProp)prop).getTypeRef());
        String suffix = prop instanceof DtoProp<?, ?> ?
                prop.getName() :
                prop.getAlias();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        return StringUtil.identifier(
                typeName.equals(TypeName.BOOLEAN) ? "is" : "get",
                suffix
        );
    }

    @SuppressWarnings("unchecked")
    private String setterName(AbstractProp prop) {
        TypeName typeName = prop instanceof DtoProp<?, ?> ?
                getPropTypeName((DtoProp<ImmutableType, ImmutableProp>) prop) :
                getTypeName(((UserProp)prop).getTypeRef());
        String suffix = prop instanceof DtoProp<?, ?> ?
                prop.getName() :
                prop.getAlias();
        if (suffix.startsWith("is") &&
                suffix.length() > 2 &&
                Character.isUpperCase(suffix.charAt(2)) &&
                typeName.equals(TypeName.BOOLEAN)) {
            suffix = suffix.substring(2);
        }
        return StringUtil.identifier("set", suffix);
    }

    private static int docKeyIndex(int originalIndex, String doc, String key) {
        int index = doc.indexOf(key);
        if (index == -1 || (originalIndex != -1 && originalIndex < index)) {
            return originalIndex;
        }
        if (doc.length() == index + key.length()) {
            return index;
        }
        if (Character.isWhitespace(doc.charAt(index + key.length()))) {
            return index;
        }
        return originalIndex;
    }

    private class Document {

        private final Context ctx;

        private final Doc dtoTypeDoc;

        private final Doc baseTypeDoc;

        private String result;

        public Document(Context ctx, DtoType<ImmutableType, ImmutableProp> dtoType) {
            this.ctx = ctx;
            dtoTypeDoc = Doc.parse(dtoType.getDoc());
            baseTypeDoc = Doc.parse(baseDocComment());
        }

        public String get() {
            String ret = result;
            if (ret == null) {
                if (dtoTypeDoc != null) {
                    ret = dtoTypeDoc.toString();
                } else if (baseTypeDoc != null) {
                    ret = baseTypeDoc.toString();
                } else {
                    ret = "";
                }
                ret = ret.replace("$", "$$");
                this.result = ret;
            }
            return ret.isEmpty() ? null : ret;
        }

        public String get(AbstractProp prop) {
            String value = getImpl(prop);
            if (value == null) {
                return null;
            }
            return value.replace("$", "$$");
        }

        @SuppressWarnings("unchecked")
        private String getImpl(AbstractProp prop) {
            ImmutableProp baseProp;
            if (prop instanceof DtoProp<?, ?>) {
                baseProp = ((DtoProp<?, ImmutableProp>) prop).toTailProp().getBaseProp();
            } else {
                baseProp = null;
            }
            if (prop.getDoc() != null) {
                Doc doc = Doc.parse(prop.getDoc());
                if (doc != null) {
                    return doc.toString();
                }
            }
            if (dtoTypeDoc != null) {
                String name = prop.getAlias();
                if (name == null) {
                    assert baseProp != null;
                    name = baseProp.getName();
                }
                String doc = dtoTypeDoc.getParameterValueMap().get(name);
                if (doc != null) {
                    return doc;
                }
            }
            if (baseProp != null) {
                Doc doc = Doc.parse(baseDocComment(baseProp));
                if (doc != null) {
                    return doc.toString();
                }
            }
            if (baseTypeDoc != null && baseProp != null) {
                String doc = baseTypeDoc.getParameterValueMap().get(baseProp.getName());
                if (doc != null) {
                    return doc;
                }
            }
            return null;
        }
    }

    private boolean isImpl() {
        return dtoType.getBaseType().isEntity() ||
                !dtoType.getModifiers().contains(DtoModifier.SPECIFICATION);
    }

    TypeSpec.Builder getTypeBuilder() {
        return typeBuilder;
    }

    @Nullable
    String stateFieldName(AbstractProp prop, boolean builder) {
        if (!prop.isNullable()) {
            return null;
        }
        if (!(prop instanceof DtoProp<?, ?>)) {
            return null;
        }
        if (!dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            return null;
        }
        DtoModifier modifier = ((DtoProp<?, ?>) prop).getInputModifier();
        if (modifier == DtoModifier.FIXED && !builder) {
            return null;
        }
        if (modifier == DtoModifier.STATIC || modifier == DtoModifier.FUZZY) {
            return null;
        }
        return StringUtil.identifier("_is", prop.getName(), "Loaded");
    }

    private static boolean isFieldNullable(AbstractProp prop) {
        if (prop instanceof DtoProp<?, ?>) {
            String funcName = ((DtoProp<?, ?>) prop).getFuncName();
            return !"null".equals(funcName) && !"notNull".equals(funcName);
        }
        return true;
    }

    private boolean isSerializerRequired() {
        if (!dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            return false;
        }
        for (DtoProp<?, ?> prop : dtoType.getDtoProps()) {
            DtoModifier inputModifier = prop.getInputModifier();
            if (inputModifier == DtoModifier.DYNAMIC) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuildRequired() {
        if (!dtoType.getModifiers().contains(DtoModifier.INPUT)) {
            return false;
        }
        for (DtoProp<?, ?> prop : dtoType.getDtoProps()) {
            DtoModifier inputModifier = prop.getInputModifier();
            if (inputModifier == DtoModifier.FIXED || inputModifier == DtoModifier.DYNAMIC) {
                return true;
            }
        }
        return false;
    }

    private boolean isHibernateValidatorEnhancementRequired() {
        return ctx.isHibernateValidatorEnhancement() &&
                dtoType.getDtoProps().stream().anyMatch(
                        it -> it.getInputModifier() == DtoModifier.DYNAMIC
                );
    }

    private String baseDocComment() {
        return docMetadata.getString(dtoType.getBaseType().getTypeElement());
    }

    private String baseDocComment(ImmutableProp prop) {
        return docMetadata.getString(prop.toElement());
    }
}
