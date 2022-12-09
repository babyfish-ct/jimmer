package org.babyfish.jimmer.example.kt.sql.cfg

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OpenAPIConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(Info().title("demo:jimmer-sql-kt").version("0.4.10"))
            .components(
                Components()
                    .addSecuritySchemes(
                        "tenantHeader",
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("tenant")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("tenantHeader"))
}