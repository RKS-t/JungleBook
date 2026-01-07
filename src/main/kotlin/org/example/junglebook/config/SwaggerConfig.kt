package org.example.junglebook.config

import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod")
class SwaggerConfig {

    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("junglebook-api")
            .packagesToScan("org.example.junglebook.web.controller")
            .pathsToMatch("/api/**")
            .build()
    }

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("JungleBook API")
                    .description("정글북 프로젝트 API 명세서입니다.")
                    .version("v1.0.0")
            )
    }
}