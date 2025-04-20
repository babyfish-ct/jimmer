package org.babyfish.jimmer.apt.tuple;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.immutable.generator.Constants;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class BaseRowMemberGenerator extends AbstractMemberGenerator {

    public BaseRowMemberGenerator(
            Context context,
            TypeElement typeElement,
            ClassName className,
            TypeSpec.Builder typeBuilder
    ) {
        super(context, typeElement, className, typeBuilder);
    }

    public void generate() {
        typeBuilder.addSuperinterface(
                ParameterizedTypeName.get(
                        Constants.BASE_ROW_CLASS_NAME,
                        ClassName.get(typeElement)
                )
        );
        generateFields();
        generateConstructor();
        generateGetters();
    }

    private void generateFields() {
        for (VariableElement fieldElement : fieldElements) {
            FieldSpec.Builder builder = FieldSpec
                    .builder(
                            expressionTypeName(fieldElement),
                            fieldElement.getSimpleName().toString(),
                            Modifier.PRIVATE,
                            Modifier.FINAL
                    );
            typeBuilder.addField(builder.build());
        }
    }

    private void generateConstructor() {
        MethodSpec.Builder builder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        builder.addParameter(
                ParameterizedTypeName.get(
                        Constants.ABSTRACT_TYPED_TUPLE_TABLE_MAPPER_CLASS_NAME,
                        ClassName.get(typeElement),
                        className
                ),
                "mapper"
        );
        ClassName tupleClassName = ClassName.get(typeElement);
        int size = fieldElements.size();
        for (int i = 0; i < size; i++) {
            builder.addStatement(
                    "this.$L = mapper.get($L)",
                    fieldElements.get(i).getSimpleName().toString(),
                    i
            );
        }
        typeBuilder.addMethod(builder.build());
    }

    private void generateGetters() {
        for (VariableElement fieldElement : fieldElements) {
            MethodSpec.Builder builder = MethodSpec
                    .methodBuilder(fieldElement.getSimpleName().toString())
                    .addModifiers(Modifier.PUBLIC, Modifier.PUBLIC)
                    .returns(expressionTypeName(fieldElement))
                    .addStatement("return $L", fieldElement.getSimpleName().toString());
            typeBuilder.addMethod(builder.build());
        }
    }
}
