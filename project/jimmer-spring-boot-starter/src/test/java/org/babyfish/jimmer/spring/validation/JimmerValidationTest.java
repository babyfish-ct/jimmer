package org.babyfish.jimmer.spring.validation;

import org.babyfish.jimmer.DraftObjects;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutable;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutableDraft;
import org.babyfish.jimmer.spring.java.validation.ValidatedImmutableProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class JimmerValidationTest {

    @Test
    public void testJavaxValidation() {
        javax.validation.Configuration<?> configuration = Mockito.mock(javax.validation.Configuration.class);
        javax.validation.TraversableResolver delegate = Mockito.mock(javax.validation.TraversableResolver.class);
        javax.validation.Path.Node property = Mockito.mock(javax.validation.Path.Node.class);
        Mockito.when(configuration.getDefaultTraversableResolver()).thenReturn(delegate);
        Mockito.when(property.getName()).thenReturn("name");

        JimmerJavaxTraversableResolver jimmerResolver = new JimmerJavaxTraversableResolver();
        Assertions.assertFalse(jimmerResolver.isReachable(unloadedImmutable(), property, null, null, null));
        Assertions.assertTrue(jimmerResolver.isReachable(loadedImmutable(), property, null, null, null));

        JimmerValidation.initialize(configuration);

        ArgumentCaptor<javax.validation.TraversableResolver> captor =
                ArgumentCaptor.forClass(javax.validation.TraversableResolver.class);
        Mockito.verify(configuration).traversableResolver(captor.capture());
        javax.validation.TraversableResolver resolver = captor.getValue();

        Assertions.assertFalse(resolver.isReachable(unloadedImmutable(), property, null, null, null));
        Mockito.verifyNoInteractions(delegate);

        Mockito.when(delegate.isReachable(Mockito.any(), Mockito.eq(property), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(true);
        Mockito.when(delegate.isCascadable(Mockito.any(), Mockito.eq(property), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(true);
        ValidatedImmutable immutable = loadedImmutable();
        Assertions.assertTrue(resolver.isReachable(immutable, property, null, null, null));
        Assertions.assertTrue(resolver.isCascadable(immutable, property, null, null, null));
    }

    @Test
    public void testJakartaValidation() {
        jakarta.validation.Configuration<?> configuration = Mockito.mock(jakarta.validation.Configuration.class);
        jakarta.validation.TraversableResolver delegate = Mockito.mock(jakarta.validation.TraversableResolver.class);
        jakarta.validation.Path.Node property = Mockito.mock(jakarta.validation.Path.Node.class);
        Mockito.when(configuration.getDefaultTraversableResolver()).thenReturn(delegate);
        Mockito.when(property.getName()).thenReturn("name");

        JimmerJakartaTraversableResolver jimmerResolver = new JimmerJakartaTraversableResolver();
        Assertions.assertFalse(jimmerResolver.isReachable(unloadedImmutable(), property, null, null, null));
        Assertions.assertTrue(jimmerResolver.isReachable(loadedImmutable(), property, null, null, null));

        JimmerValidation.initialize(configuration);

        ArgumentCaptor<jakarta.validation.TraversableResolver> captor =
                ArgumentCaptor.forClass(jakarta.validation.TraversableResolver.class);
        Mockito.verify(configuration).traversableResolver(captor.capture());
        jakarta.validation.TraversableResolver resolver = captor.getValue();

        Assertions.assertFalse(resolver.isReachable(unloadedImmutable(), property, null, null, null));
        Mockito.verifyNoInteractions(delegate);

        Mockito.when(delegate.isReachable(Mockito.any(), Mockito.eq(property), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(true);
        Mockito.when(delegate.isCascadable(Mockito.any(), Mockito.eq(property), Mockito.isNull(), Mockito.isNull(), Mockito.isNull()))
                .thenReturn(true);
        ValidatedImmutable immutable = loadedImmutable();
        Assertions.assertTrue(resolver.isReachable(immutable, property, null, null, null));
        Assertions.assertTrue(resolver.isCascadable(immutable, property, null, null, null));
    }

    private static ValidatedImmutable loadedImmutable() {
        return ValidatedImmutableDraft.$.produce(draft -> draft.setName("Jimmer"));
    }

    private static ValidatedImmutable unloadedImmutable() {
        return ValidatedImmutableDraft.$.produce(draft -> {
            draft.setName("Jimmer");
            DraftObjects.unload(draft, ValidatedImmutableProps.NAME);
        });
    }
}
