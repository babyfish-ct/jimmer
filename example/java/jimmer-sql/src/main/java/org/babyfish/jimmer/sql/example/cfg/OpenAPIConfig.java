package org.babyfish.jimmer.sql.example.cfg;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("demo:jimmer-sql").version("0.5.14"))
                .components(
                        new Components()
                        .addSecuritySchemes(
                                "tenantHeader",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("tenant")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("tenantHeader"));
    }
}
