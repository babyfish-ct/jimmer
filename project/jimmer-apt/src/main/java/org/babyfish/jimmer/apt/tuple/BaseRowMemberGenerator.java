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
    }

    private void generateFields() {
        for (VariableElement fieldElement : fieldElements) {
            FieldSpec.Builder builder = FieldSpec
                    .builder(
                            propTypeName(fieldElement),
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
        for (VariableElement fieldElement : fieldElements) {
            builder.addParameter(
                    propTypeName(fieldElement),
                    fieldElement.getSimpleName().toString()
            );
            builder.addStatement(
                    "this.$L = $L",
                    fieldElement.getSimpleName().toString(),
                    fieldElement.getSimpleName().toString()
            );
        }
        typeBuilder.addMethod(builder.build());
    }
}
