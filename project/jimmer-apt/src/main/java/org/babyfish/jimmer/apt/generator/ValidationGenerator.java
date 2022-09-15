package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.MetaException;
import org.babyfish.jimmer.apt.meta.ValidationMessages;

import javax.validation.ValidationException;
import javax.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ValidationGenerator {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final ImmutableProp prop;

    private final String valueName;

    private final MethodSpec.Builder methodBuilder;

    public ValidationGenerator(
            ImmutableProp prop,
            String valueName,
            MethodSpec.Builder methodBuilder
    ) {
        this.prop = prop;
        this.valueName = valueName;
        this.methodBuilder = methodBuilder;
    }

    public void generate() {
        if (!prop.isNullable() && !prop.getTypeName().isPrimitive()) {
            methodBuilder
                    .beginControlFlow("if ($L == null)", valueName)
                    .addStatement(
                            "throw new IllegalArgumentException(\"'$L' cannot be null\")",
                            prop.getName()
                    )
                    .endControlFlow();
        }
        generateNotEmpty();
        generateNotBlank();
        generateSize();
        generateBound();
        generateEmail();
        generatePattern();
        generateConstraints();
    }

    private void generateNotEmpty() {
        NotEmpty[] notEmpties = prop.getAnnotations(NotEmpty.class);
        if (notEmpties.length == 0) {
            return;
        }
        if (!isSimpleClass(String.class) && !isSimpleClass(List.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            notEmpties[0].annotationType().getName() +
                            " but its type is neither string nor list"
            );
        }
        validate(
                "$L.isEmpty()",
                new Object[]{ valueName },
                notEmpties[0].message(),
                () -> "it cannot be empty"
        );
    }

    private void generateNotBlank() {
        NotBlank[] notBlanks = prop.getAnnotations(NotBlank.class);
        if (notBlanks.length == 0) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            notBlanks[0].annotationType().getName() +
                            " but its type is not string"
            );
        }
        validate(
                "$L.trim().isEmpty()",
                new Object[]{ valueName },
                notBlanks[0].message(),
                () -> "it cannot be empty"
        );
    }

    private void generateSize() {
        Size[] sizes = prop.getAnnotations(Size.class);
        if (sizes.length == 0) {
            return;
        }
        if (!isSimpleClass(String.class) && !isSimpleClass(List.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            sizes[0].annotationType().getName() +
                            " but its type is neither string nor list"
            );
        }
        int min = 0;
        int max = Integer.MAX_VALUE;
        String minMessage = null;
        String maxMessage = null;
        for (Size size : sizes) {
            if (size.min() > min) {
                min = size.min();
                minMessage = size.message();
            }
            if (size.max() < max) {
                max = size.max();
                maxMessage = size.message();
            }
        }
        if (min > max) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", its size validation rules is illegal " +
                            "so that there is not valid length"
            );
        }
        if (min == 0 && max == Integer.MAX_VALUE) {
            return;
        }
        String sizeFun = isSimpleClass(String.class) ? "length" : "size";
        if (min > 0) {
            final int finalValue = min;
            validate(
                    "$L.$L() < $L",
                    new Object[]{ valueName, sizeFun, finalValue },
                    minMessage,
                    () -> "it cannot be less than " + finalValue
            );
        }
        if (max < Integer.MAX_VALUE) {
            final int finalValue = max;
            validate(
                    "$L.$L() > $L",
                    new Object[]{ valueName, sizeFun, finalValue },
                    maxMessage,
                    () -> "it cannot be greater than " + finalValue
            );
        }
    }

    private void generateBound() {

        Min[] minArr = prop.getAnnotations(Min.class);
        Max[] maxArr = prop.getAnnotations(Max.class);
        Positive[] positives = prop.getAnnotations(Positive.class);
        PositiveOrZero[] positiveOrZeros = prop.getAnnotations(PositiveOrZero.class);
        Negative[] negatives = prop.getAnnotations(Negative.class);
        NegativeOrZero[] negativeOrZeros = prop.getAnnotations(NegativeOrZero.class);
        Annotation annotation = Arrays.stream(new Annotation[][] { minArr, maxArr, positives, positiveOrZeros, negatives, negativeOrZeros})
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (annotation == null) {
            return;
        }
        if (!prop.getTypeName().isPrimitive() &&
                !prop.getTypeName().isBoxedPrimitive() &&
                !isSimpleClass(BigInteger.class) &&
                !isSimpleClass(BigDecimal.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            annotation.annotationType().getName() +
                            " but its type is numeric"
            );
        }

        Long minValue = null;
        Long maxValue = null;
        String message = null;
        for (Min min : minArr) {
            if (minValue == null || min.value() > minValue) {
                minValue = min.value();
                message = min.message();
            }
        }
        for (Positive positive : positives) {
            if (minValue == null || 1L > minValue) {
                minValue = 1L;
                message = positive.message();
            }
        }
        for (PositiveOrZero positiveOrZero : positiveOrZeros) {
            if (minValue == null || 0L > minValue) {
                minValue = 0L;
                message = positiveOrZero.message();
            }
        }
        for (Max max : maxArr) {
            if (maxValue == null || max.value() < maxValue) {
                maxValue = max.value();
                message = max.message();
            }
        }
        for (Negative negative : negatives) {
            if (maxValue == null || -1L < maxValue) {
                maxValue = -1L;
                message = negative.message();
            }
        }
        for (NegativeOrZero negativeOrZero : negativeOrZeros) {
            if (maxValue == null || 0L < maxValue) {
                maxValue = 0L;
                message = negativeOrZero.message();
            }
        }
        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", its numeric range validation rules is illegal " +
                            "so that there is not valid number"
            );
        }
        if (minValue != null) {
            final long finalValue = minValue;
            validateBound(minValue, "<", message);
        }
        if (maxValue != null) {
            validateBound(maxValue, ">", message);
        }
    }

    private void generateEmail() {
        Email[] emails = prop.getAnnotations(Email.class);
        if (emails.length == 0) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            emails[0].annotationType().getName() +
                            " but its type is not string"
            );
        }
        validate(
                "!$L.matcher($L).matches()",
                new Object[]{ Constants.DRAFT_FIELD_EMAIL_PATTERN, valueName },
                emails[0].message(),
                () -> "it is not email address"
        );
    }

    private void generatePattern() {
        Pattern[] patterns = prop.getAnnotations(Pattern.class);
        if (patterns.length == 0) {
            return;
        }
        if (!isSimpleClass(String.class)) {
            throw new MetaException(
                    "Illegal property \"" +
                            prop +
                            "\", it's decorated by the annotation @" +
                            patterns[0].annotationType().getName() +
                            " but its type is not string"
            );
        }
        for (int i = 0; i < patterns.length; i++) {
            final int index = i;
            validate(
                    "!$L.matcher($L).matches()",
                    new Object[]{ Constants.regexpPatternFieldName(prop, i), valueName },
                    patterns[index].message(),
                    () -> "it does not match the regexp '" +
                            patterns[index].regexp().replace("\\", "\\\\") +
                            "'"
            );
        }
    }

    private void generateConstraints() {

    }

    private void validate(
            String condition,
            Object[] args,
            String errorMessage,
            Supplier<String> defaultMessageSupplier
    ) {
        if (!prop.isNullable() || prop.getTypeName().isPrimitive()) {
            methodBuilder.beginControlFlow(
                    "if (" + condition + ")",
                    args != null ? args : EMPTY_ARGS
            );
        } else {
            methodBuilder.beginControlFlow(
                    "if (" + valueName + " != null && " + condition + ")",
                    args != null ? args : EMPTY_ARGS
            );
        }
        if (errorMessage == null ||
                errorMessage.isEmpty() ||
                errorMessage.startsWith("{javax.validation.constraints.")
        ) {
            errorMessage = "Illegal value '\" + " +
                    valueName +
                    " + \"' for property '" +
                    prop +
                    "', " +
                    defaultMessageSupplier.get();
            methodBuilder.addStatement(
                    "throw new $T(\"$L\")",
                    ValidationException.class,
                    errorMessage
            );
        }
        methodBuilder.endControlFlow();
    }

    private boolean isSimpleClass(Class<?> type) {
        TypeName typeName = prop.getTypeName();
        ClassName className;
        if (typeName instanceof ClassName) {
            className = ((ClassName)typeName);
        } else if (typeName instanceof ParameterizedTypeName) {
            className = ((ParameterizedTypeName)typeName).rawType;
        } else {
            return false;
        }
        return className.packageName().equals(type.getPackage().getName()) &&
                className.simpleNames().size() == 1 &&
                className.simpleName().equals(type.getSimpleName());
    }

    private void validateBound(long bound, String cmp, String message) {
        String bigNumLiteral;
        if (prop.getTypeName().equals(ClassName.get(BigDecimal.class))) {
            if (bound == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound == 1) {
                bigNumLiteral = "$T.ONE";
            } else if (bound == 2) {
                bigNumLiteral = "$T.TWO";
            } else if (bound == 10) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ")";
            }
        } else if (prop.getTypeName().equals(ClassName.get(BigInteger.class))) {
            if (bound == -1) {
                bigNumLiteral = "$T.NEGATIVE_ONE";
            } else if (bound == 0) {
                bigNumLiteral = "$T.ZERO";
            } else if (bound == 1) {
                bigNumLiteral = "$T.ONE";
            } else if (bound == 2) {
                bigNumLiteral = "$T.TWO";
            } else if (bound == 10) {
                bigNumLiteral = "$T.TEN";
            } else {
                bigNumLiteral = "$T.valueOf(" + bound + ", 0)";
            }
        } else {
            bigNumLiteral = null;
        }
        validate(
                bigNumLiteral != null ?
                        "$L.compareTo(" + bigNumLiteral + ") $L 0" :
                        "$L $L $L",
                bigNumLiteral != null ?
                        new Object[] { valueName, prop.getElementType(), cmp } :
                        new Object[] { valueName, cmp, bound },
                message,
                () -> "it cannot be " +
                        (cmp.equals("<") ? "less than" : "greater than") +
                        " " +
                        bound
        );
    }
}
