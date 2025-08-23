package org.example.junglebook.config

import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod") // 운영 환경 제외
class SwaggerConfig {

    // API를 그룹화하고 스캔할 범위를 지정
    @Bean
    fun publicApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("junglebook-api")
            .packagesToScan("org.example.junglebook.controller") // 스캔할 패키지
            .pathsToMatch("/api/**") // 포함할 경로 패턴
            .build()
    }

    // API 문서의 전반적인 정보 설정
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