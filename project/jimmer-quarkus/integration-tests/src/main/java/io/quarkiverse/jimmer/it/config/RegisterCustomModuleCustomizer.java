package io.quarkiverse.jimmer.it.config;

import jakarta.inject.Singleton;

import org.babyfish.jimmer.jackson.ImmutableModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class RegisterCustomModuleCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(new ImmutableModule());
    }
}
