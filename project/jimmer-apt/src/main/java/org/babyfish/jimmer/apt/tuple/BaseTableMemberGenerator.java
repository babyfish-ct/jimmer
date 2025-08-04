//package org.babyfish.jimmer.apt.tuple;
//
//import com.squareup.javapoet.*;
//import org.babyfish.jimmer.apt.Context;
//import org.babyfish.jimmer.apt.immutable.generator.Constants;
//
//import javax.lang.model.element.Modifier;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.element.VariableElement;
//
//public class BaseTableMemberGenerator extends AbstractMemberGenerator {
//
//    public BaseTableMemberGenerator(
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
//                        Constants.ABSTRACT_BASE_TABLE_CLASS_NAME,
//                        ClassName.get(typeElement)
//                )
//        );
//        generateFactoryField();
//        generateFields();
//        generateConstructor();
//        generateGetters();
//    }
//
//    private void generateFactoryField() {
//        FieldSpec.Builder builder = FieldSpec
//                .builder(
//                        ParameterizedTypeName.get(
//                                Constants.FUNCTION_CLASS_NAME,
//                                ParameterizedTypeName.get(
//                                        Constants.BASE_TABLE_QUERY_IMPLEMENTOR_CLASS_NAME,
//                                        WildcardTypeName.subtypeOf(TypeName.OBJECT),
//                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
//                                ),
//                                ParameterizedTypeName.get(
//                                        Constants.BASE_TABLE_CLASS_NAME,
//                                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
//                                )
//                        ),
//                        "FACTORY",
//                        Modifier.STATIC
//                )
//                .initializer(
//                        "\n    query -> new $T(($T)query)",
//                        className,
//                        ParameterizedTypeName.get(
//                                Constants.BASE_TABLE_QUERY_IMPLEMENTOR_CLASS_NAME,
//                                ClassName.get(typeElement),
//                                className
//                        )
//                );
//        typeBuilder.addField(builder.build());
//    }
//
//    private void generateFields() {
//        for (VariableElement fieldElement : fieldElements) {
//            FieldSpec.Builder builder = FieldSpec
//                    .builder(
//                            expressionTypeName(fieldElement, true),
//                            fieldElement.getSimpleName().toString(),
//                            Modifier.PRIVATE,
//                            Modifier.FINAL
//                    );
//            typeBuilder.addField(builder.build());
//        }
//    }
//
//    private void generateConstructor() {
//        MethodSpec.Builder builder = MethodSpec
//                .constructorBuilder()
//                .addModifiers(Modifier.PUBLIC);
//        builder.addParameter(
//                ParameterizedTypeName.get(
//                        Constants.BASE_TABLE_QUERY_IMPLEMENTOR_CLASS_NAME,
//                        ClassName.get(typeElement),
//                        className
//                ),
//                "query"
//        );
//        builder.addStatement("super(query)");
//        builder.addStatement(
//                "$T mapper = (($T)query.getSelections().get(0)).getMapper()",
//                ParameterizedTypeName.get(
//                        Constants.TYPED_TUPLE_MAPPER_CLASS_NAME,
//                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
//                ),
//                ParameterizedTypeName.get(
//                        Constants.MAPPER_SELECTION_CLASS_NAME,
//                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
//                )
//        );
//        int size = fieldElements.size();
//        for (int i = 0; i < size; i++) {
//            builder.addStatement(
//                    "this.$L = $T.of(mapper.<$T>get($L), this, $L)",
//                    fieldElements.get(i).getSimpleName().toString(),
//                    Constants.BASE_TABLE_SELECTIONS_CLASS_NAME,
//                    expressionTypeName(fieldElements.get(i), true),
//                    i,
//                    i
//            );
//        }
//        typeBuilder.addMethod(builder.build());
//    }
//
//    private void generateGetters() {
//        for (VariableElement fieldElement : fieldElements) {
//            MethodSpec.Builder builder = MethodSpec
//                    .methodBuilder(fieldElement.getSimpleName().toString())
//                    .addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
//                    .returns(expressionTypeName(fieldElement, true))
//                    .addStatement("return $L", fieldElement.getSimpleName().toString());
//            typeBuilder.addMethod(builder.build());
//        }
//    }
//}
