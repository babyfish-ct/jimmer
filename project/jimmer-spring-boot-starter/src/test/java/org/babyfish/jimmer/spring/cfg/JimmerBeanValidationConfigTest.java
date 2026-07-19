package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.DraftObjects;
import org.babyfish.jimmer.UnloadedException;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutable;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutableDraft;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutableProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JimmerBeanValidationConfigTest {

    @Test
    public void testAutoConfigurationImportsBeanValidationConfig() {
        Import importAnnotation = JimmerAutoConfiguration.class.getAnnotation(Import.class);

        Assertions.assertNotNull(importAnnotation);
        Assertions.assertTrue(
                Arrays
                        .asList(importAnnotation.value())
                        .contains(JimmerBeanValidationConfig.class)
        );
    }

    @Test
    public void testSkipUnloadedImmutableProperty() {
        ValidatedImmutable immutable = unloadedImmutable();

        Assertions.assertThrows(UnloadedException.class, immutable::getName);

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            AtomicBoolean initializerCalled = new AtomicBoolean();

            ctx.register(JimmerBeanValidationConfig.class);
            ctx.registerBean(LocalValidatorFactoryBean.class, () -> {
                LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
                validatorFactoryBean.setConfigurationInitializer(configuration -> initializerCalled.set(true));
                return validatorFactoryBean;
            });
            ctx.refresh();

            Validator validator = ctx.getBean(Validator.class);
            Set<ConstraintViolation<ValidatedImmutable>> violations = validator.validate(immutable);

            Assertions.assertTrue(initializerCalled.get());
            Assertions.assertTrue(violations.isEmpty());
        }
    }

    @Test
    public void testPlainValidatorTouchesUnloadedImmutableProperty() {
        ValidatedImmutable immutable = unloadedImmutable();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            Assertions.assertThrows(ValidationException.class, () -> validator.validate(immutable));
        } finally {
            validator.destroy();
        }
    }

    private static ValidatedImmutable unloadedImmutable() {
        return ValidatedImmutableDraft.$.produce(draft -> {
            draft.setName("Jimmer");
            DraftObjects.unload(draft, ValidatedImmutableProps.NAME);
        });
    }
}
