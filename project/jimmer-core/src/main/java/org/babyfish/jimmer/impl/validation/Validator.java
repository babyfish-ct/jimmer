package org.babyfish.jimmer.impl.validation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ValidationException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator<T> {

    public static final String JAKARTA_CONSTRAINT_FULL_NAME = "jakarta.validation.Constraint";
    public static final String JAKARTA_VALIDATION_EXCEPTION_CLASS_NAME = "jakarta.validation.ValidationException";

    private static final Pattern I18N_PATTERN = Pattern.compile("\\{[^}]+}");

    private static ResourceBundle BUNDLE;

    private final String message;

    private final List<ConstraintValidator<?, T>> javaxConstraintValidators;

    private final List<JakartaConstraintValidatorDelegate<T>> jakartaConstraintValidators;

    @SuppressWarnings("unchecked")
    public Validator(
            @NotNull Class<? extends Annotation> annotationType,
            @NotNull String message,
            @NotNull Class<?> type,
            @Nullable PropId propId
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

        Constraint javaxConstraint = annotationType.getAnnotation(Constraint.class);
        if (javaxConstraint != null) {
            this.javaxConstraintValidators = new ArrayList<>();
            this.jakartaConstraintValidators = Collections.emptyList();
            for (Class<? extends ConstraintValidator<?, ?>> validatorType :
                    new LinkedHashSet<>(Arrays.asList(javaxConstraint.validatedBy()))) {
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
                ((ConstraintValidator<Annotation, ?>) constraintValidator).initialize(annotation);
                javaxConstraintValidators.add(constraintValidator);
            }
        } else {
            final Class<? extends Annotation> jakartaConstraintClass;
            try {
                jakartaConstraintClass =
                        Class.forName(JAKARTA_CONSTRAINT_FULL_NAME).asSubclass(Annotation.class);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(
                        "jakarta.validation API is required at runtime for Jakarta Bean Validation constraints. " +
                                "Add jakarta.validation:jakarta.validation-api (and an implementation such as " +
                                "Hibernate Validator) to the classpath.",
                        ex
                );
            }
            Annotation jakartaConstraintMeta = annotationType.getAnnotation(jakartaConstraintClass);
            Objects.requireNonNull(
                    jakartaConstraintMeta,
                    "Annotation type \""+annotationType.getName()+"\" must be meta-annotated with " +
                            "javax.validation.Constraint or jakarta.validation.Constraint"
            );
            this.javaxConstraintValidators = Collections.emptyList();
            this.jakartaConstraintValidators = new ArrayList<>();
            try {
                Method validatedByMethod = jakartaConstraintClass.getMethod("validatedBy");
                Class<?>[] validatedBy = (Class<?>[]) validatedByMethod.invoke(jakartaConstraintMeta);

                for (Class<?> validatorType : new LinkedHashSet<>(Arrays.asList(validatedBy))) {
                    Object constraintValidator = validatorType.getConstructor().newInstance();
                    Method initialize =
                            findInitializeMethod(validatorType, annotation.annotationType());
                    initialize.invoke(constraintValidator, annotation);
                    jakartaConstraintValidators.add(new JakartaConstraintValidatorDelegate<>(constraintValidator));
                }
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getTargetException();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(
                        "jakarta.validation API is required at runtime for Jakarta Bean Validation constraints. " +
                                "Add jakarta.validation:jakarta.validation-api (and an implementation such as " +
                                "Hibernate Validator) to the classpath.",
                        ex
                );
            }
        }
    }

    private static Method findInitializeMethod(Class<?> validatorType, Class<? extends Annotation> annotationClass)
            throws NoSuchMethodException {
        for (Method method : validatorType.getMethods()) {
            if (!"initialize".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param.isAssignableFrom(annotationClass)) {
                return method;
            }
        }
        throw new NoSuchMethodException(
                "No initialize(" + annotationClass.getName() + ") on " + validatorType.getName()
        );
    }

    public void validate(T value) {
        for (ConstraintValidator<?, T> constraintValidator : javaxConstraintValidators) {
            if (!constraintValidator.isValid(value, null)) {
                throw new ValidationException(message);
            }
        }
        for (JakartaConstraintValidatorDelegate<T> delegate : jakartaConstraintValidators) {
            try {
                if (!delegate.isValid(value)) {
                    throwJakartaValidationException(message);
                }
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new IllegalStateException(cause);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static void throwJakartaValidationException(String message) {
        try {
            Class<?> exClass = Class.forName(JAKARTA_VALIDATION_EXCEPTION_CLASS_NAME);
            Constructor<?> ctor = exClass.getConstructor(String.class);
            throw (RuntimeException) ctor.newInstance(message);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "jakarta.validation API is required at runtime for Jakarta Bean Validation constraints.",
                    e
            );
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static final class JakartaConstraintValidatorDelegate<T> {

        private final Object delegate;

        private final Method isValidMethod;

        JakartaConstraintValidatorDelegate(Object delegate) throws ReflectiveOperationException {
            this.delegate = delegate;
            Class<?> ctxClass = Class.forName("jakarta.validation.ConstraintValidatorContext");
            this.isValidMethod = delegate.getClass().getMethod("isValid", Object.class, ctxClass);
        }

        boolean isValid(T value) throws InvocationTargetException, IllegalAccessException {
            return (Boolean) isValidMethod.invoke(delegate, value, null);
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
