//package org.babyfish.jimmer.apt.tuple;
//
//import com.squareup.javapoet.ClassName;
//import com.squareup.javapoet.ParameterizedTypeName;
//import com.squareup.javapoet.TypeName;
//import com.squareup.javapoet.TypeSpec;
//import org.babyfish.jimmer.apt.Context;
//import org.babyfish.jimmer.apt.MetaException;
//import org.babyfish.jimmer.apt.immutable.generator.Constants;
//import org.babyfish.jimmer.sql.TypedTuple;
//
//import javax.lang.model.element.*;
//import javax.lang.model.type.PrimitiveType;
//import javax.lang.model.type.TypeMirror;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public abstract class AbstractMemberGenerator {
//
//    protected final Context context;
//
//    protected final TypeElement typeElement;
//
//    protected final ClassName className;
//
//    protected final TypeSpec.Builder typeBuilder;
//
//    protected final List<VariableElement> fieldElements;
//
//    AbstractMemberGenerator(
//            Context context,
//            TypeElement typeElement,
//            ClassName className,
//            TypeSpec.Builder typeBuilder
//    ) {
//        this.context = context;
//        this.typeElement = typeElement;
//        this.className = className;
//        this.typeBuilder = typeBuilder;
//        List<VariableElement> fieldElements = new ArrayList<>();
//        for (Element element : typeElement.getEnclosedElements()) {
//            if (element instanceof VariableElement) {
//                if (!element.getModifiers().contains(Modifier.STATIC)) {
//                    validateFieldElement((VariableElement) element);
//                    fieldElements.add((VariableElement) element);
//                }
//            }
//        }
//        if (fieldElements.isEmpty()) {
//            throw new MetaException(
//                    typeElement,
//                    "There is no non-state field"
//            );
//        }
//        this.fieldElements = Collections.unmodifiableList(fieldElements);
//    }
//
//    private void validateFieldElement(VariableElement fieldElement) {
//         if (context.isDto(fieldElement.asType())) {
//             throw new MetaException(
//                     fieldElement,
//                     "The field of type decorated by \"@" +
//                             TypedTuple.class.getName() +
//                             "\" cannot be dto"
//             );
//         }
//    }
//
//    protected final TypeName expressionTypeName(VariableElement fieldElement, boolean typeBootstrap) {
//        TypeMirror type = fieldElement.asType();
//        if (context.isEntity(type)) {
//            return context.getImmutableType(type).getTableClassName();
//        }
//        if (typeBootstrap) {
//            if (context.isEmbeddable(type)) {
//                return context.getImmutableType(type).getPropExpressionClassName();
//            }
//            if (context.isDate(type)) {
//                return ParameterizedTypeName.get(
//                        Constants.DATE_EXPRESSION_CLASS_NAME,
//                        ClassName.get(type).box()
//                );
//            }
//            if (context.isTemporal(type)) {
//                return ParameterizedTypeName.get(
//                        Constants.TEMPORAL_EXPRESSION_CLASS_NAME,
//                        ClassName.get(type).box()
//                );
//            }
//            if (context.isNumber(type) || type instanceof PrimitiveType) {
//                return ParameterizedTypeName.get(
//                        Constants.NUMERIC_EXPRESSION_CLASS_NAME,
//                        ClassName.get(type).box()
//                );
//            }
//            if (context.isComparable(type)) {
//                return ParameterizedTypeName.get(
//                        Constants.COMPARABLE_EXPRESSION_CLASS_NAME,
//                        ClassName.get(type).box()
//                );
//            }
//        }
//        return ParameterizedTypeName.get(
//                Constants.EXPRESSION_CLASS_NAME,
//                ClassName.get(type).box()
//        );
//    }
//}
