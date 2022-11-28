package org.babyfish.jimmer.impl.validation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ValidationException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator<T> {

    private static final Pattern I18N_PATTERN = Pattern.compile("\\{[^}]+}");

    private static ResourceBundle BUNDLE;

    private final String message;

    private final List<ConstraintValidator<?, T>> constraintValidators;

    @SuppressWarnings("unchecked")
    public Validator(
            @NotNull Class<? extends Annotation> annotationType,
            @NotNull String message,
            @NotNull Class<?> type,
            @Nullable Integer propId
    ) {
        ImmutableProp prop = propId != null ? ImmutableType.get(type).getProp(propId) : null;

        Annotation annotation;
        if (prop != null) {
            annotation = prop.getAnnotation(annotationType);
        } else {
            annotation = type.getAnnotation(annotationType);
        }

        if (message.isEmpty()) {
            message = tryGetDefaultMessage(annotation);
        }

        if (!message.isEmpty()) {
            this.message = translateMessage(message);
        } else if (prop != null) {
            this.message =
                    "'" +
                            type.getName() +
                            '.' +
                            prop.getName() +
                            "' does not match the validation rule of @" + annotationType.getName();
        } else {
            this.message =
                    "'" +
                            type.getName() +
                            "' does not match the validation rule of @" + annotationType.getName();
        }
        Constraint constraint = annotationType.getAnnotation(Constraint.class);
        List<ConstraintValidator<?, T>> constraintValidators = new ArrayList<>();
        for (Class<? extends ConstraintValidator<?, ?>> validatorType :
                new LinkedHashSet<>(Arrays.asList(constraint.validatedBy()))) {
            ConstraintValidator<?, T> constraintValidator;
            try {
                constraintValidator = (ConstraintValidator<?, T>) validatorType.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                throw new IllegalArgumentException(
                        "Cannot create constraint validator instance of \"" +
                                validatorType.getName() +
                                "\" declared by the annotation \"@" +
                                annotationType.getName() +
                                "\"",
                        ex
                );
            } catch (InvocationTargetException ex) {
                throw new IllegalArgumentException(
                        "Cannot create constraint validator instance of \"" +
                                validatorType.getName() +
                                "\" declared by the annotation \"@" +
                                annotationType.getName() +
                                "\"",
                        ex.getTargetException()
                );
            }
            ((ConstraintValidator<Annotation, ?>)constraintValidator).initialize(annotation);
            constraintValidators.add(constraintValidator);
        }
        this.constraintValidators = constraintValidators;
    }

    public void validate(T value) {
        for (ConstraintValidator<?, T> constraintValidator : constraintValidators) {
            if (!constraintValidator.isValid(value, null)) {
                throw new ValidationException(message);
            }
        }
    }

    private static String tryGetDefaultMessage(Annotation annotation) {
        Method method;
        try {
            method = annotation.annotationType().getMethod("message");
        } catch (NoSuchMethodException ex) {
            return "";
        }
        if (method.getReturnType() != String.class) {
            return "";
        }
        try {
            return (String)method.invoke(annotation);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new AssertionError("Internal bug", ex);
        }
    }

    private static String translateMessage(String message) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = I18N_PATTERN.matcher(message);
        int pos = 0;
        while (matcher.find()) {
            if (matcher.start() != pos) {
                builder.append(message.substring(pos, matcher.start()));
            }
            builder.append(
                    bundle().getString(
                            message.substring(matcher.start() + 1, matcher.end() - 1)
                    )
            );
            pos = matcher.end();
        }
        if (pos == 0) {
            return message;
        } else if (pos != message.length()) {
            builder.append(message.substring(pos));
        }
        return builder.toString();
    }

    private static ResourceBundle bundle() {
        ResourceBundle bundle = BUNDLE;
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("ValidationMessages");
            BUNDLE = bundle;
        }
        return bundle;
    }
}
