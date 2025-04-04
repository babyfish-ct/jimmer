package org.babyfish.jimmer.apt.transactional;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.apt.immutable.generator.Annotations;
import org.babyfish.jimmer.apt.immutable.generator.Constants;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public class TxGenerator {

    private final Context ctx;

    private final TypeElement typeElement;

    private final TypeMirror sqlClientType;

    private final TypeMirror runtimeExceptionType;

    private final String sqlClientName;

    private final AnnotationMirror classTx;

    private TypeSpec.Builder typeBuilder;

    public TxGenerator(Context ctx, TypeElement typeElement) {
        this.ctx = ctx;
        this.typeElement = typeElement;
        this.sqlClientType = ctx.getElements().getTypeElement("org.babyfish.jimmer.sql.JSqlClient").asType();
        this.runtimeExceptionType = ctx.getElements().getTypeElement("java.lang.RuntimeException").asType();
        this.sqlClientName = sqlClientName();
        this.classTx = TxUtil.tx(ctx, typeElement);
    }

    private String sqlClientName() {
        VariableElement sqlClientElement = null;
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement variableElement = (VariableElement) element;
                if (!variableElement.getModifiers().contains(Modifier.STATIC)) {
                    if (ctx.getTypes().isAssignable(sqlClientType, variableElement.asType())) {
                        if (sqlClientElement != null) {
                            throw new MetaException(
                                    element,
                                    "The class uses @Tx cannot multiple non-static sqlClient fields"
                            );
                        }
                        sqlClientElement = variableElement;
                    }
                }
            }
        }
        if (sqlClientElement == null) {
            throw new MetaException(
                    typeElement,
                    "The class uses @Tx must have a non-static field whose type is JSqlClient"
            );
        }
        if (sqlClientElement.getModifiers().contains(Modifier.PRIVATE)) {
            throw new MetaException(
                    sqlClientElement,
                    "The sqlClient field of the class uses @Tx cannot be private, default or protected is recommended"
            );
        }
        return sqlClientElement.getSimpleName().toString();
    }

    public void generate() {
        typeBuilder = TypeSpec
                .classBuilder(typeElement.getSimpleName().toString() + "Tx")
                .superclass(typeElement.asType());
        for (AnnotationMirror annotationMirror : typeElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) ctx.getTypes().asElement(annotationMirror.getAnnotationType());
            String qualifiedName = annotationElement.getQualifiedName().toString();
            if (!TxUtil.TARGET_ANNOTATION.equals(qualifiedName) && !TxUtil.TX.equals(qualifiedName)) {
                typeBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        AnnotationMirror targetAnnotation = TxUtil.targetAnnotation(ctx, typeElement);
        if (targetAnnotation != null) {
            String qualifiedName = targetAnnotation.getElementValues().values().iterator().next().getValue().toString();
            typeBuilder.addAnnotation(ClassName.bestGuess(qualifiedName));
        }
        if (typeElement.getModifiers().contains(Modifier.PUBLIC)) {
            typeBuilder.addModifiers(Modifier.PUBLIC);
        }
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            typeBuilder.addModifiers(Modifier.ABSTRACT);
        }

        addConstructors();
        addMethods();
        try {
            JavaFile
                    .builder(
                            ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName().toString(),
                            typeBuilder.build()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(ctx.getFiler());
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate tx class for '%s'",
                            typeElement.getQualifiedName().toString()
                    ),
                    ex
            );
        }
    }

    private void addConstructors() {
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.CONSTRUCTOR && !element.getModifiers().contains(Modifier.PRIVATE)) {
                ExecutableElement executableElement = (ExecutableElement) element;
                MethodSpec.Builder builder = MethodSpec.constructorBuilder();
                StringBuilder sb = new StringBuilder();
                for (VariableElement variableElement : executableElement.getParameters()) {
                    builder.addParameter(ParameterSpec.get(variableElement));
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(variableElement.getSimpleName().toString());
                }
                builder.addStatement("super(" + sb + ')');
                typeBuilder.addMethod(builder.build());
            }
        }
    }

    private void addMethods() {
        for (Element element : typeElement.getEnclosedElements()) {
            if (!(element instanceof ExecutableElement)) {
                continue;
            }
            AnnotationMirror tx = TxUtil.tx(ctx, element);
            if (tx != null) {
                if (element.getKind() != ElementKind.METHOD) {
                    throw new MetaException(
                            element,
                            "Member that is not method cannot be decorated by @Tx"
                    );
                }
                if (element.getModifiers().contains(Modifier.STATIC)) {
                    throw new MetaException(
                            element,
                            "Static method cannot be decorated by @Tx"
                    );
                }
                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new MetaException(
                            element,
                            "Private method cannot be decorated by @Tx"
                    );
                }
                if (element.getModifiers().contains(Modifier.FINAL)) {
                    throw new MetaException(
                            element,
                            "Final method cannot be decorated by @Tx"
                    );
                }
                if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                    throw new MetaException(
                            element,
                            "Abstract method cannot be decorated by @Tx"
                    );
                }
                for (TypeMirror thrownType : ((ExecutableElement)element).getThrownTypes()) {
                    if (!ctx.getTypes().isAssignable(runtimeExceptionType, thrownType)) {
                        TypeElement thrownElement = (TypeElement) ctx.getTypes().asElement(thrownType);
                        throw new MetaException(
                                element,
                                "Method decorated by @Tx can only throw RuntimeException, but it throws \"" +
                                        thrownElement.getQualifiedName() +
                                        "\""
                        );
                    }
                }
            }
            if (tx == null &&
                    classTx != null &&
                    element.getModifiers().contains(Modifier.PUBLIC) &&
                    element.getKind() == ElementKind.METHOD &&
                    !element.getModifiers().contains(Modifier.STATIC) &&
                    !element.getModifiers().contains(Modifier.PRIVATE)) {
                if (element.getModifiers().contains(Modifier.FINAL)) {
                    throw new MetaException(
                            element,
                            "The public method inherits the class-level @Tx cannot be final"
                    );
                }
                tx = classTx;
            }
            if (tx != null) {
                addMethod((ExecutableElement) element, tx);
            }
        }
    }

    private void addMethod(ExecutableElement executableElement, AnnotationMirror tx) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(executableElement.getSimpleName().toString())
                .addAnnotation(Override.class);
        if (executableElement.getModifiers().contains(Modifier.PROTECTED)) {
            builder.addModifiers(Modifier.PROTECTED);
        }
        if (executableElement.getModifiers().contains(Modifier.PUBLIC)) {
            builder.addModifiers(Modifier.PUBLIC);
        }
        String doc = ctx.getElements().getDocComment(executableElement);
        if (doc != null) {
            builder.addJavadoc(doc.replace("$", "$$"));
        }
        for (AnnotationMirror annotationMirror : executableElement.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) ctx.getTypes().asElement(annotationMirror.getAnnotationType());
            String qualifiedName = annotationElement.getQualifiedName().toString();
            if (!"java.lang.Override".equals(qualifiedName) && !TxUtil.TX.equals(qualifiedName)) {
                builder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }
        }
        for (TypeParameterElement typeParameterElement : executableElement.getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameterElement));
        }
        builder.returns(TypeName.get(executableElement.getReturnType()));
        StringBuilder sb = new StringBuilder();
        for (VariableElement variableElement : executableElement.getParameters()) {
            builder.addParameter(ParameterSpec.get(variableElement));
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(variableElement.getSimpleName().toString());
        }
        for (TypeMirror thrownType : executableElement.getThrownTypes()) {
            builder.addException(TypeName.get(thrownType));
        }
        Element propagationElement = Annotations.annotationValue(tx, "value", null);
        String propagation = propagationElement != null ?
                propagationElement.getSimpleName().toString() :
                "REQUIRED";
        boolean isVoid = executableElement.getReturnType().getKind() == TypeKind.VOID;
        CodeBlock.Builder cb = CodeBlock.builder();
        cb.add(
                "$L$L.transaction($T.$L, () -> $> {\n",
                isVoid ? "" : "return ",
                sqlClientName,
                Constants.PROPAGATION_CLASS_NAME,
                propagation
        );
        cb.add(
                "$Lsuper.$L($L);\n",
                isVoid ? "" : "return ",
                executableElement.getSimpleName().toString(),
                sb.toString()
        );
        if (isVoid) {
            cb.add("return null;\n");
        }
        cb.add("$<});");
        builder.addCode(cb.build());
        typeBuilder.addMethod(builder.build());
    }
}
