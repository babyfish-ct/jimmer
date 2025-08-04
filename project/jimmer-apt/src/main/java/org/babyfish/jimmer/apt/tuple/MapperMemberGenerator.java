//package org.babyfish.jimmer.apt.tuple;
//
//import com.squareup.javapoet.*;
//import org.babyfish.jimmer.apt.Context;
//import org.babyfish.jimmer.apt.immutable.generator.Constants;
//import org.babyfish.jimmer.apt.util.ClassNames;
//import org.babyfish.jimmer.impl.util.StringUtil;
//import org.jetbrains.annotations.Nullable;
//
//import javax.lang.model.element.Modifier;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.element.VariableElement;
//
//public class MapperMemberGenerator extends AbstractMemberGenerator {
//
//    private static final TypeName SELECTION_ARR_TYPE_NAME = ArrayTypeName.of(
//            ParameterizedTypeName.get(
//                    Constants.SELECTION_CLASS_NAME,
//                    WildcardTypeName.subtypeOf(TypeName.OBJECT)
//            )
//    );
//
//    MapperMemberGenerator(
//            Context context,
//            TypeElement typeElement,
//            ClassName className,
//            TypeSpec.Builder typeBuilder
//    ) {
//        super(context, typeElement, className, typeBuilder);
//    }
//
//    public void generate() {
//        typeBuilder.superclass(
//                ParameterizedTypeName.get(
//                        Constants.ABSTRACT_BASE_TABLE_MAPPER_CLASS_NAME,
//                        ClassName.get(typeElement),
//                        ClassNames.of(
//                                className,
//                                it -> typeElement.getSimpleName().toString() +
//                                        TypedTupleProcessor.BASE_TABLE_SUFFIX
//                        )
//                )
//        );
//        generateConstructor();
//        generateOf();
//        int size = fieldElements.size();
//        for (int i = 0; i < size; i++) {
//            generateBuilderClass(
//                    i,
//                    fieldElements.get(i),
//                    i + 1 < size ? fieldElements.get(i + 1) : null
//            );
//        }
//    }
//
//    private void generateConstructor() {
//        MethodSpec.Builder builder = MethodSpec
//                .constructorBuilder()
//                .addParameter(
//                        ArrayTypeName.of(
//                                ParameterizedTypeName.get(
//                                        Constants.SELECTION_CLASS_NAME,
//                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
//                                )
//                        ),
//                        "selections"
//                )
//                .addStatement(
//                        "super($T.class, $T.class, selections)",
//                        ClassName.get(typeElement),
//                        ClassNames.of(
//                                className,
//                                it -> typeElement.getSimpleName().toString() +
//                                        TypedTupleProcessor.BASE_TABLE_SUFFIX
//                        )
//                );
//        typeBuilder.addMethod(builder.build());
//    }
//
//    private void generateOf() {
//        MethodSpec.Builder builder = MethodSpec
//                .methodBuilder("of")
//                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
//        VariableElement firstFieldElement = fieldElements.get(0);
//        ClassName className = this.className
//                .nestedClass(StringUtil.typeName(firstFieldElement.getSimpleName().toString()));
//        builder.returns(className);
//        builder.addStatement(
//                "return new $T(new $T<?>[$L])",
//                className,
//                Constants.SELECTION_CLASS_NAME,
//                fieldElements.size()
//        );
//        typeBuilder.addMethod(builder.build());
//    }
//
//    private void generateBuilderClass(
//            int index,
//            VariableElement fieldElement,
//            @Nullable VariableElement nextFieldElement
//    ) {
//        TypeSpec.Builder builder = TypeSpec
//                .classBuilder(StringUtil.typeName(fieldElement.getSimpleName().toString()))
//                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                .addField(
//                        FieldSpec
//                                .builder(SELECTION_ARR_TYPE_NAME, "selections")
//                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
//                                .build()
//                )
//                .addMethod(
//                        MethodSpec
//                                .constructorBuilder()
//                                .addParameter(
//                                        SELECTION_ARR_TYPE_NAME,
//                                        "selections"
//                                )
//                                .addStatement("this.selections = selections")
//                                .build()
//                )
//                .addMethod(
//                        newBuildMethod(
//                                index,
//                                fieldElement,
//                                nextFieldElement
//                        )
//                );
//        typeBuilder.addType(builder.build());
//    }
//
//    private MethodSpec newBuildMethod(
//            int index,
//            VariableElement fieldElement,
//            @Nullable VariableElement nextFieldElement
//    ) {
//        MethodSpec.Builder builder = MethodSpec
//                .methodBuilder(fieldElement.getSimpleName().toString())
//                .addParameter(
//                        expressionTypeName(fieldElement, false),
//                        "selection"
//                )
//                .addStatement("selections[$L] = selection", index);
//        if (nextFieldElement != null) {
//            ClassName className = this.className
//                    .nestedClass(StringUtil.typeName(nextFieldElement.getSimpleName().toString()));
//            builder.returns(className);
//            builder.addStatement("return new $T(selections)", className);
//        } else {
//            builder.returns(className);
//            builder.addStatement("return new $T(selections)", className);
//        }
//        return builder.build();
//    }
//}
